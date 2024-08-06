/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SecretServerApiUtils {
  private val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8080/")
    .addConverterFactory(GsonConverterFactory.create()).build()

  val apiService: AccessMachineApi = retrofit.create(AccessMachineApi::class.java)

  suspend fun <T> makeApiRequest(apiCall: suspend () -> Response<T>): Response<T> {
    return apiCall()

  }


}


