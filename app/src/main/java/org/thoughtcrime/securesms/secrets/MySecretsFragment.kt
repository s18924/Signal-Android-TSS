package org.thoughtcrime.securesms.secrets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.databinding.FragmentMySecretsBinding
import org.thoughtcrime.securesms.secrets.database.Secret
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.io.encoding.ExperimentalEncodingApi

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MySecretsFragment : Fragment(), ExistingSecretsAdapter.OnSecretClickListener {
  private var param1: String? = null
  private var param2: String? = null

  private var _binding: FragmentMySecretsBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      param1 = it.getString(ARG_PARAM1)
      param2 = it.getString(ARG_PARAM2)
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    _binding = FragmentMySecretsBinding.inflate(inflater, container, false)

    val secrets = mutableListOf<Secret>()

    binding.existingSecretsList.adapter = ExistingSecretsAdapter(secrets, this)
    binding.existingSecretsList.layoutManager = LinearLayoutManager(requireContext())


    SignalDatabase.secrets.forEach { secret -> secrets.add(secret.value) }

    binding.existingSecretsList

    binding.existingSecretsList.adapter?.notifyDataSetChanged()
    return binding.root
  }

  companion object {
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
      MySecretsFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_PARAM1, param1)
          putString(ARG_PARAM2, param2)
        }
      }
  }

  override fun onSecretClick(secret: Secret) {
    val action = MySecretsFragmentDirections.actionMySecretsToSecretOverviewFragment(secret)
    findNavController().navigate(action)
  }
}