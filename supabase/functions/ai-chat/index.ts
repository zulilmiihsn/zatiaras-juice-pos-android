// Supabase Edge Function: AI Chat Proxy (BFF Pattern)
// Routes AI requests to OpenRouter or Groq with server-side API keys.
// API keys are stored as Supabase Secrets, never exposed to the client.
//
// Deploy: supabase functions deploy ai-chat --no-verify-jwt
// Set secrets:
//   supabase secrets set OPENROUTER_API_KEY=sk-or-v1-xxx
//   supabase secrets set GROQ_API_KEY=gsk_xxx

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const OPENROUTER_API_KEY = Deno.env.get("OPENROUTER_API_KEY") ?? "";
const GROQ_API_KEY = Deno.env.get("GROQ_API_KEY") ?? "";

const PROVIDER_CONFIG: Record<string, { url: string; getKey: () => string; extraHeaders?: Record<string, string> }> = {
  openrouter: {
    url: "https://openrouter.ai/api/v1/chat/completions",
    getKey: () => OPENROUTER_API_KEY,
    extraHeaders: {
      "HTTP-Referer": "https://zatiaras.pos",
      "X-Title": "Zatiaras POS",
    },
  },
  groq: {
    url: "https://api.groq.com/openai/v1/chat/completions",
    getKey: () => GROQ_API_KEY,
  },
};

interface AiChatRequest {
  provider: "openrouter" | "groq";
  model: string;
  messages: Array<{ role: string; content: unknown }>;
  temperature?: number;
  max_tokens?: number;
}

interface NormalizedAiResponse {
  id: string;
  choices: Array<{
    message: {
      role: string;
      content: string | null;
    };
    finish_reason?: string | null;
  }>;
  usage?: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  } | null;
}

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...CORS_HEADERS,
    },
  });

const clampNumber = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

const normalizeProviderResponse = (raw: any): NormalizedAiResponse => ({
  id: String(raw?.id ?? ""),
  choices: Array.isArray(raw?.choices)
    ? raw.choices.map((choice: any) => ({
        message: {
          role: String(choice?.message?.role ?? "assistant"),
          content: typeof choice?.message?.content === "string" ? choice.message.content : null,
        },
        finish_reason: typeof choice?.finish_reason === "string" ? choice.finish_reason : null,
      }))
    : [],
  usage:
    raw?.usage &&
    typeof raw.usage.prompt_tokens === "number" &&
    typeof raw.usage.completion_tokens === "number" &&
    typeof raw.usage.total_tokens === "number"
      ? {
          prompt_tokens: raw.usage.prompt_tokens,
          completion_tokens: raw.usage.completion_tokens,
          total_tokens: raw.usage.total_tokens,
        }
      : null,
});

serve(async (req: Request) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }

  try {
    const body: AiChatRequest = await req.json();
    const { provider, model, messages, temperature = 0.7, max_tokens = 2048 } = body;

    if (!provider || !model || !Array.isArray(messages) || messages.length === 0) {
      return jsonResponse({ error: "Invalid request body. Required: provider, model, messages[]" }, 400);
    }

    const safeTemperature = clampNumber(Number(temperature), 0, 2);
    const safeMaxTokens = Math.floor(clampNumber(Number(max_tokens), 1, 4096));

    // Validate provider
    const config = PROVIDER_CONFIG[provider];
    if (!config) {
      return jsonResponse({ error: `Unknown provider: ${provider}. Use 'openrouter' or 'groq'.` }, 400);
    }

    const apiKey = config.getKey();
    if (!apiKey) {
      return jsonResponse({ error: `API key not configured for provider: ${provider}` }, 500);
    }

    // Forward request to AI provider
    const headers: Record<string, string> = {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      ...(config.extraHeaders ?? {}),
    };

    const abortController = new AbortController();
    const timeout = setTimeout(() => abortController.abort("AI provider timeout"), 45000);

    const providerResponse = await fetch(config.url, {
      method: "POST",
      headers,
      body: JSON.stringify({
        model,
        messages,
        temperature: safeTemperature,
        max_tokens: safeMaxTokens,
        stream: false,
      }),
      signal: abortController.signal,
    });

    clearTimeout(timeout);

    if (!providerResponse.ok) {
      const errorText = await providerResponse.text();
      console.error(`[ai-chat] Provider ${provider} error: ${providerResponse.status} - ${errorText}`);
      return jsonResponse({ error: `Provider error: ${providerResponse.status}`, details: errorText }, providerResponse.status);
    }

    const data = await providerResponse.json();
    const normalized = normalizeProviderResponse(data);

    if (!normalized.choices.length) {
      return jsonResponse({ error: "Provider returned empty choices" }, 502);
    }

    return jsonResponse(normalized, 200);
  } catch (error) {
    console.error("[ai-chat] Unexpected error:", error);
    return jsonResponse({ error: "Internal server error", message: String(error) }, 500);
  }
});
