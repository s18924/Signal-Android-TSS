/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import lombok.EqualsAndHashCode
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

    binding.createNewButton.setOnClickListener { findNavController().navigate(R.id.action_secretsLandingFragment_to_createNewSecretFragment)}
    binding.mySecrets.setOnClickListener { findNavController().navigate(R.id.action_secretsLandingFragment_to_mySecretsFragment)}

  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

class ExistingSecretsAdapter(private val secrets: List<Secret>, private val listener: OnSecretClickListener) : RecyclerView.Adapter<ExistingSecretsAdapter.ViewHolder>() {

  interface OnSecretClickListener {
    fun onSecretClick(secret: Secret)
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val secretButton: Button = itemView.findViewById(R.id.button2)
  }
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_secret, parent, false)
    return ViewHolder(itemView)
  }

  override fun getItemCount(): Int = secrets.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val secret = secrets[position]
    holder.secretButton.text = secret.name
    holder.secretButton.setOnClickListener {
      listener.onSecretClick(secret)
//      findNavController().navigate(R.id.action_mySecretsFragment_to_shareFragment)
    }
  }

}

@Parcelize
@EqualsAndHashCode
data class Share(val hash: String, val data: ByteArray) : Parcelable {
  @IgnoredOnParcel
  val shares = mutableListOf<Share>()
}