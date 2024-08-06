/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AccessMachineApi {

  @GET("/api/wallet")
  suspend fun getWallet(): Response<String>

  @POST("/api/persist")
  suspend fun persist(@Body persistData: PersistData): Response<Void>

  @POST("/api/secretKeyRequest")
  suspend fun secretKeyRequest(@Body secretKeyRequest: SecretKeyRequest): Response<String>


  @GET("/api/secretKeyResponse/{transactionId}")
  suspend fun secretKeyResponse(@Path("transactionId") transactionId: String): Response<SecretKeyResponse>

}

data class PersistData(val hash: ByteArray, val key: ByteArray)
data class SecretKeyRequest(val requestorKey: ByteArray, val encryptedData: ByteArray)
data class SecretKeyResponse(val transactionId: String, val message: String, val recryptedKey: String)



