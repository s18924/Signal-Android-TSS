/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.mms.OutgoingMessage
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.sms.MessageSender

class MessageUtils {


  companion object {
    fun sendMessage(recipient: Recipient, messageBody: String) {
      val message = OutgoingMessage.text(
        recipient,
//        Recipient.resolved(RecipientId.from(6)),
        messageBody,
        0
      )
      MessageSender.send(
        AppDependencies.application,
        message,
        -1,
        MessageSender.SendType.SIGNAL,
        null,
        null
      )
    }
  }
}