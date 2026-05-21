// Supabase Edge Function: AI Chat Proxy (BFF Pattern)
// Routes AI requests to OpenRouter or Groq with server-side API keys.
// API keys are stored as Supabase Secrets, never exposed to the client.
//
// Deploy: supabase functions deploy ai-chat
// Set secrets:
//   supabase secrets set OPENROUTER_API_KEY=sk-or-v1-xxx
//   supabase secrets set GROQ_API_KEY=gsk_xxx

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const OPENROUTER_API_KEY = Deno.env.get("OPENROUTER_API_KEY") ?? "";
const GROQ_API_KEY = Deno.env.get("GROQ_API_KEY") ?? "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const ALLOWED_ORIGIN = Deno.env.get("ALLOWED_ORIGIN") ?? "https://zatiaras.pos";

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

const corsHeaders = (req: Request): Record<string, string> => {
  const origin = req.headers.get("Origin");
  const allowOrigin = origin === ALLOWED_ORIGIN ? origin : ALLOWED_ORIGIN;

  return {
    "Access-Control-Allow-Origin": allowOrigin,
    "Vary": "Origin",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  };
};

const getBearerToken = (req: Request): string | null => {
  const authorization = req.headers.get("Authorization") ?? "";
  const [scheme, token] = authorization.split(" ");
  if (scheme?.toLowerCase() !== "bearer" || !token) return null;
  return token;
};

const authenticateRequest = async (
  req: Request,
): Promise<{ ok: true } | { ok: false; status: number; error: string }> => {
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
    return { ok: false, status: 500, error: "Auth is not configured" };
  }

  const token = getBearerToken(req);
  if (!token) {
    return { ok: false, status: 401, error: "Missing bearer token" };
  }

  const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: {
      persistSession: false,
      autoRefreshToken: false,
    },
    global: {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  });

  const { data, error } = await supabase.auth.getUser(token);
  if (error || !data.user) {
    return { ok: false, status: 401, error: "Invalid bearer token" };
  }

  return { ok: true };
};

const jsonResponse = (req: Request, body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...corsHeaders(req),
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
    return new Response("ok", { headers: corsHeaders(req) });
  }

  if (req.method !== "POST") {
    return jsonResponse(req, { error: "Method not allowed" }, 405);
  }

  try {
    const auth = await authenticateRequest(req);
    if (!auth.ok) {
      return jsonResponse(req, { error: auth.error }, auth.status);
    }

    const body: AiChatRequest = await req.json();
    const { provider, model, messages, temperature = 0.7, max_tokens = 2048 } = body;

    if (!provider || !model || !Array.isArray(messages) || messages.length === 0) {
      return jsonResponse(req, { error: "Invalid request body. Required: provider, model, messages[]" }, 400);
    }

    const safeTemperature = clampNumber(Number(temperature), 0, 2);
    const safeMaxTokens = Math.floor(clampNumber(Number(max_tokens), 1, 4096));

    // Validate provider
    const config = PROVIDER_CONFIG[provider];
    if (!config) {
      return jsonResponse(req, { error: `Unknown provider: ${provider}. Use 'openrouter' or 'groq'.` }, 400);
    }

    const apiKey = config.getKey();
    if (!apiKey) {
      return jsonResponse(req, { error: `API key not configured for provider: ${provider}` }, 500);
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
      return jsonResponse(req, { error: `Provider error: ${providerResponse.status}` }, providerResponse.status);
    }

    const data = await providerResponse.json();
    const normalized = normalizeProviderResponse(data);

    if (!normalized.choices.length) {
      return jsonResponse(req, { error: "Provider returned empty choices" }, 502);
    }

    return jsonResponse(req, normalized, 200);
  } catch (error) {
    console.error("[ai-chat] Unexpected error:", error);
    return jsonResponse(req, { error: "Internal server error" }, 500);
  }
});
