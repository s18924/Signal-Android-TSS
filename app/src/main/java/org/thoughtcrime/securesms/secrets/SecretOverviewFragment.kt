package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.databinding.FragmentSecretOverviewBinding
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.secrets.database.Secret
import org.whispersystems.signalservice.api.push.ServiceId

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SecretOverviewFragment : Fragment() {
  private var param1: String? = null
  private var param2: String? = null
  private var _binding: FragmentSecretOverviewBinding? = null
  private val binding get() = _binding!!

  private var ownsSecret: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      param1 = it.getString(ARG_PARAM1)
      param2 = it.getString(ARG_PARAM2)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSecretOverviewBinding.inflate(inflater, container, false)
    val secret = arguments?.getParcelable<Secret>("secret")


    binding.textViewShareK.text = "Number of shares needed to recreate: ${secret?.k.toString()}"
    binding.textViewShareN.text = "Total shares: ${secret?.n.toString()}"

    var sharesAvailable = secret!!.shares.stream().filter { it.isReturned }.count()
    if(sharesAvailable < secret.k){
      binding.buttonRecreate.isEnabled = false
      binding.buttonRecreate.text = "${sharesAvailable}/${secret.k} shares available - can not be recreated "
    }else{
      binding.buttonRecreate.isEnabled = true
      binding.buttonRecreate.text = "${sharesAvailable}/${secret.k} shares available - recreate!"

    }

    binding.textViewSecretHash.text = secret?.hash

    viewLifecycleOwner.lifecycleScope.launch {
      val serviceId = ServiceId.parseOrThrow(secret!!.owner)
      val recipientId = RecipientId.from(serviceId)
      if (Recipient.self().id.equals(recipientId)) {
        ownsSecret = true
      }
      binding.textViewOwnerName.text = "Owner: ${if(ownsSecret) "(You) " else ""}${Recipient.resolved(recipientId).profileName} (${Recipient.resolved(recipientId).e164.orElse("")})"

    }

    val recyclerView = binding.recyclerView
    recyclerView.adapter = ShareAdapter(secret!!.shares)
    recyclerView.layoutManager = LinearLayoutManager(context)
    return binding.root
  }

  companion object {
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
      SecretOverviewFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_PARAM1, param1)
          putString(ARG_PARAM2, param2)
        }
      }
  }
}



