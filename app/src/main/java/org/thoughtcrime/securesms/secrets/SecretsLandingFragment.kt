/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    //binding.helloWorldText.text = getString(R.string.hello_world)

    binding.createNewButton.setOnClickListener { findNavController().navigate(R.id.action_secretsLandingFragment_to_createNewSecretFragment)}

    val shares = mutableListOf<Share>()
    binding.existingSharesList.adapter = ExistingSharesAdapter(shares)
    binding.existingSharesList.layoutManager = LinearLayoutManager(requireContext())

    shares.add(Share("11111111"))
    shares.add(Share("22222222"))

    binding.existingSharesList.adapter?.notifyDataSetChanged()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

class ExistingSharesAdapter(private val shares: List<Share>) : RecyclerView.Adapter<ExistingSharesAdapter.ViewHolder>() {

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val shareButton: Button = itemView.findViewById(R.id.button2)
  }
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_secret_share, parent, false)
    return ViewHolder(itemView)
  }

  override fun getItemCount(): Int = shares.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val share = shares[position]
    holder.shareButton.text = share.hash
    holder.shareButton.setOnClickListener {
      // TODO
    }
  }

}

data class Share(val hash: String) {
  val shares = mutableListOf<Share>()
}



