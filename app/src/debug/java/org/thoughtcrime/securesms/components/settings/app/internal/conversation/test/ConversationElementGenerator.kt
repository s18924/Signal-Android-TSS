/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.internal.conversation.test

import org.thoughtcrime.securesms.conversation.ConversationMessage.ConversationMessageFactory
import org.thoughtcrime.securesms.conversation.v2.data.ConversationElementKey
import org.thoughtcrime.securesms.conversation.v2.data.IncomingTextOnly
import org.thoughtcrime.securesms.conversation.v2.data.OutgoingTextOnly
import org.thoughtcrime.securesms.database.MessageTypes
import org.thoughtcrime.securesms.database.model.MediaMmsMessageRecord
import org.thoughtcrime.securesms.database.model.StoryType
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.mms.SlideDeck
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.util.adapter.mapping.MappingModel
import java.security.SecureRandom
import kotlin.time.Duration.Companion.milliseconds

/**
 * Generates random conversation messages via the given set of parameters.
 */
class ConversationElementGenerator {
  private val mappingModelCache = mutableMapOf<ConversationElementKey, MappingModel<*>>()
  private val random = SecureRandom()

  private val wordBank = listOf(
    "A",
    "Test",
    "Message",
    "To",
    "Display",
    "Content",
    "In",
    "Bubbles",
    "User",
    "Signal",
    "The"
  )

  fun getMappingModel(key: ConversationElementKey): MappingModel<*> {
    val cached = mappingModelCache[key]
    if (cached != null) {
      return cached
    }

    val messageModel = generateMessage(key)
    mappingModelCache[key] = messageModel
    return messageModel
  }

  private fun getIncomingType(): Long {
    return MessageTypes.BASE_INBOX_TYPE or MessageTypes.SECURE_MESSAGE_BIT
  }

  private fun getSentOutgoingType(): Long {
    return MessageTypes.BASE_SENT_TYPE or MessageTypes.SECURE_MESSAGE_BIT
  }

  private fun getSentFailedOutgoingType(): Long {
    return MessageTypes.BASE_SENT_FAILED_TYPE or MessageTypes.SECURE_MESSAGE_BIT
  }

  private fun generateMessage(key: ConversationElementKey): MappingModel<*> {
    val messageId = key.requireMessageId()
    val now = getNow()

    val testMessageWordLength = random.nextInt(40) + 1
    val testMessage = (0 until testMessageWordLength).map {
      wordBank.random()
    }.joinToString(" ")

    val isIncoming = random.nextBoolean()

    val record = MediaMmsMessageRecord(
      messageId,
      if (isIncoming) Recipient.UNKNOWN else Recipient.self(),
      0,
      if (isIncoming) Recipient.self() else Recipient.UNKNOWN,
      now,
      now,
      now,
      1,
      1,
      testMessage,
      SlideDeck(),
      if (isIncoming) getIncomingType() else getSentFailedOutgoingType(),
      emptySet(),
      emptySet(),
      0,
      0,
      0,
      false,
      1,
      null,
      emptyList(),
      emptyList(),
      false,
      emptyList(),
      false,
      false,
      now,
      1,
      now,
      null,
      StoryType.NONE,
      null,
      null,
      null,
      null,
      -1,
      null,
      null,
      0
    )

    val conversationMessage = ConversationMessageFactory.createWithUnresolvedData(
      ApplicationDependencies.getApplication(),
      record,
      Recipient.UNKNOWN
    )

    return if (isIncoming) {
      IncomingTextOnly(conversationMessage)
    } else {
      OutgoingTextOnly(conversationMessage)
    }
  }

  private fun getNow(): Long {
    val now = System.currentTimeMillis()
    return now - random.nextInt(20.milliseconds.inWholeMilliseconds.toInt()).toLong()
  }
}
