/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.databinding.SecretsLandingFragmentBinding

/**
 * Fragment for displaying "Secrets" content.
 */
class SecretsLandingFragment : Fragment() {

  private var _binding: SecretsLandingFragmentBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = SecretsLandingFragmentBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
  }

  private fun initializeUI() {
    // Example: Set a text view to "Hello World"
    binding.helloWorldText.text = getString(R.string.hello_world)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}



