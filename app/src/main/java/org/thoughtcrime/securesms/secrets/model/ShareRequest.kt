/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets.model

data class ShareRequest(
  val requestor: String,
  val secretHash: String,
  val shareHash: String,
  val publicKey: ByteArray,
  var transactionId: String? = null,
  var recryptedKey: ByteArray? = null,
  var status: Status? = Status.NEW
  ) {

}
enum class Status { NEW, ACCEPTED, IN_PROGRESS, FAILED, SUCCESS }
