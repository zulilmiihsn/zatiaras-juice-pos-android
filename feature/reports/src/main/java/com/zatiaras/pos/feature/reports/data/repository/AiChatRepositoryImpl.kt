package com.zatiaras.pos.feature.reports.data.repository

import com.zatiaras.pos.core.data.remote.AiRemoteDataSource
import com.zatiaras.pos.core.data.remote.OpenRouterContentPart
import com.zatiaras.pos.core.data.remote.OpenRouterImageUrl
import com.zatiaras.pos.core.data.remote.OpenRouterMessage
import com.zatiaras.pos.feature.reports.domain.model.AiChatMessage
import com.zatiaras.pos.feature.reports.domain.repository.AiChatRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatRepositoryImpl @Inject constructor(
    private val aiRemoteDataSource: AiRemoteDataSource,
    private val json: Json
) : AiChatRepository {

    override suspend fun sendChatMessage(
        messages: List<AiChatMessage>,
        model: String,
        provider: String
    ): Result<String> {
        val openRouterMessages = messages.map { msg ->
            if (msg.imageBase64 != null) {
                // Multi-modal message: text + image
                val parts = buildList {
                    add(OpenRouterContentPart(type = "text", text = msg.content))
                    add(
                        OpenRouterContentPart(
                            type = "image_url",
                            imageUrl = OpenRouterImageUrl(
                                url = "data:image/jpeg;base64,${msg.imageBase64}"
                            )
                        )
                    )
                }
                OpenRouterMessage(
                    role = msg.role,
                    content = json.encodeToJsonElement(parts)
                )
            } else {
                // Text-only message
                OpenRouterMessage(
                    role = msg.role,
                    content = JsonPrimitive(msg.content)
                )
            }
        }

        return aiRemoteDataSource.getChatCompletion(
            provider = provider,
            messages = openRouterMessages,
            model = model
        )
    }
}
