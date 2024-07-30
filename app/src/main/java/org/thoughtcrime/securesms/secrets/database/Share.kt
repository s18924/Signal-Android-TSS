/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import lombok.EqualsAndHashCode

@Parcelize
@EqualsAndHashCode
data class Share(
  val hash: String,
  val data: ByteArray,
  var isShared: Boolean = false,
  val sharedWithServiceId: String? = null,
  val hashOfSecret: String? = null,
  val k: Int = 0
) : Parcelable