package org.thoughtcrime.securesms.secrets

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateNewSecretFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateNewSecretFragment : Fragment() {
  // TODO: Rename and change types of parameters
  private var param1: String? = null
  private var param2: String? = null

  private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri != null) {
      val content = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.bufferedReader().use { it.readText() }
      }
      val tag = "FileContent"
      Log.d(tag, content ?: "Unable to read file content")
      Log.d(tag, uri.path)

      view?.findViewById<AppCompatTextView>(R.id.selectedFileName)?.text = getFileNameFromUri(uri)

      view?.findViewById<TextView>(R.id.secretNameEditText)?.text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + getFileNameFromUri(uri)

    }
  }

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
  ): View? {
    return inflater.inflate(R.layout.fragment_create_new_secret, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val selectSecretFileButton = view.findViewById<Button>(R.id.selectFileButton)
    val generateButton = view.findViewById<Button>(R.id.createSecretSharesButton)
    val secretShareNumberEditText = view.findViewById<EditText>(R.id.secretShareNumberEditTextNumber)
    val secretRecoveryShareNumberEditText = view.findViewById<EditText>(R.id.secretRecoveryShareNumberEditTextNumber)

    val textWatcher = object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
      override fun afterTextChanged(s: Editable?) {
        generateButton.isEnabled = when {
          (secretShareNumberEditText.text.isNullOrEmpty() || secretRecoveryShareNumberEditText.text.isNullOrEmpty()) -> false
          secretRecoveryShareNumberEditText.text.toString().toInt() < 2 -> false
          secretRecoveryShareNumberEditText.text.toString().toInt() > secretShareNumberEditText.text.toString().toInt() - 1 -> false
          else -> true
        }
      }
    }

    secretShareNumberEditText.addTextChangedListener(textWatcher)
    secretRecoveryShareNumberEditText.addTextChangedListener(textWatcher)

    selectSecretFileButton.setOnClickListener {
      selectFileLauncher.launch("*/*")
    }

  }

  private fun getFileNameFromUri(uri: Uri): String? {
      val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
      cursor?.use {
        if (it.moveToFirst()) {
          val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
          return it.getString(nameIndex) + " " + it.getString(sizeIndex)
        }
      }
      return null
  }

  companion object {
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreateNewSecretFragment.
     */
    // TODO: Rename and change types and number of parameters
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
      CreateNewSecretFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_PARAM1, param1)
          putString(ARG_PARAM2, param2)
        }
      }
  }
}