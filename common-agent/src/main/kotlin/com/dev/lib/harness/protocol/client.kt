package com.dev.lib.harness.protocol

import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage as SpringUserMessage

fun UserMessage.toSpringMessage(): Message = when (this) {
    is UserMessage.Text -> SpringUserMessage(text)
}

fun List<UserMessage>.toSpringMessages(): List<Message> = map { it.toSpringMessage() }
