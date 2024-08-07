/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.databinding.SecretsLandingFragmentBinding
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.secrets.database.Secret
import org.whispersystems.signalservice.api.push.ServiceId

/**
 * Fragment for displaying "Secrets" content.
 */
class SecretsLandingFragment : Fragment() {

  private var _binding: SecretsLandingFragmentBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = SecretsLandingFragmentBinding.inflate(inflater, container, false)


//    inflater.context.getSharedPreferences("secret_preferences", Context.MODE_PRIVATE).edit().clear().commit()

    SignalDatabase.secrets.forEach{ it ->
      inflater.context.getSharedPreferences("secret_preferences", Context.MODE_PRIVATE).edit().putString(it.key, Gson().toJson(it.value)).apply()
    }
    inflater.context.getSharedPreferences("secret_preferences", Context.MODE_PRIVATE).all.values.forEach {
      val secret = Gson().fromJson(it as String, Secret::class.java)
      SignalDatabase.secrets.put(secret.hash, secret)
      println("Deserialized secret: ${secret.hash} ${secret.name}")
    }
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


    holder.secretButton.text = "Sekret: ${secret.name} | Owner: ${Recipient.resolved(RecipientId.from(ServiceId.Companion.parseOrThrow(secret.owner))).profileName}" //TODO Can throw
    holder.secretButton.setOnClickListener {
      listener.onSecretClick(secret)
//      findNavController().navigate(R.id.action_mySecretsFragment_to_shareFragment)
    }
  }

}





