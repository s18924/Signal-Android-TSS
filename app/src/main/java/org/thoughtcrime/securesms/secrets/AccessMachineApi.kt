/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import retrofit2.Response
import retrofit2.http.GET

interface AccessMachineApi {

  @GET("/api/wallet")
  suspend fun getWallet(): Response<String>

}
