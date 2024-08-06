/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import pjatk.secret.crypto.AesCryptoUtils
import java.util.stream.Collectors
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Parcelize
data class Secret(
  val name: String,
  val owner: String,
  val k: Int,
  val n: Int,
  val shares: List<Share> = mutableListOf(),
  val hash: String = calculateHash(shares)
) : Parcelable {



  companion object{
    @OptIn(ExperimentalEncodingApi::class)
    fun calculateHash(shares: List<Share>): String {

      val shareWithSecretHash = shares.stream()
        .filter { it -> it.hashOfSecret != null }

        .findAny()
      if(shareWithSecretHash.isPresent){

        return shareWithSecretHash.get().hashOfSecret!!
      }


      val joinedShares: String = shares.stream()
        .map { it -> it.hash }
        .collect(Collectors.joining())

      val secretHash = Base64.encode(AesCryptoUtils.getInstance().hash(joinedShares.toByteArray()))
      shares.forEach { it.hashOfSecret = secretHash }
      return secretHash
    }
  }
}