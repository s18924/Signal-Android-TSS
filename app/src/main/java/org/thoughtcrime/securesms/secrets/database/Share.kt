/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import lombok.EqualsAndHashCode
import pjatk.secret.crypto.AesCryptoUtils
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Parcelize
@EqualsAndHashCode
data class Share @OptIn(ExperimentalEncodingApi::class) constructor(
  val data: ByteArray,
  val hash: String = Base64.encode(AesCryptoUtils.getInstance().hash(data)),
  var isShared: Boolean = false,
  var sharedWithServiceId: String? = null,
  var hashOfSecret: String? = null,
  val k: Int = 0,
  val owner: String? = null,
  var isRequested: Boolean = false
) : Parcelable