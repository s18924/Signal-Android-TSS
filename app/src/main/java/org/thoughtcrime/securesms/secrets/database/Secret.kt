/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Secret(
  val hash: String,
  val name: String,
  val owner: String,
  val k: Int,
  val n: Int,
  val shares: List<Share> = mutableListOf()
) : Parcelable {}