package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.thoughtcrime.securesms.databinding.FragmentSecretOverviewBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SecretOverviewFragment : Fragment() {
  private var param1: String? = null
  private var param2: String? = null
  private var _binding: FragmentSecretOverviewBinding? = null
  private val binding get() = _binding!!
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


    binding.textViewShareK.text = secret?.n.toString()
    binding.textViewShareN.text = secret?.n.toString()

    binding.textViewSecretHash.text = secret?.hash
    binding.textViewOwnerName.text = secret?.owner

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



