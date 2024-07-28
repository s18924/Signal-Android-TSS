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
import com.google.gson.Gson
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.attachments.UriAttachment
import org.thoughtcrime.securesms.database.AttachmentTable
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.mms.OutgoingMessage
import org.thoughtcrime.securesms.mms.SlideDeck
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.sms.MessageSender
import org.whispersystems.signalservice.api.util.OptionalUtil.asOptional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CreateNewSecretFragment : Fragment() {
  private var secretContent: String? = null
  private var fileUri: Uri? = null

  private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    if (uri != null) {
      val content = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.bufferedReader().use { it.readText() }
      }
      val tag = "FileContent"
      Log.d(tag, content ?: "Unable to read file content")
      Log.d(tag, uri.path)

      view?.findViewById<AppCompatTextView>(R.id.selectedFileName)?.text = getFileNameFromUri(uri)

      view?.findViewById<TextView>(R.id.secretNameEditText)?.text = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} ${getFileNameFromUri(uri)}"
      secretContent = content;
      fileUri = uri;
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
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

    for (i in 1..10) {
      SignalDatabase.secrets.add(Secret("" + i, "" + LocalDateTime.now(), "Me", 5 + i, 1 + i))
    }

    generateButton.setOnClickListener {

      val n = secretShareNumberEditText.text.toString().toInt()
      val k = secretRecoveryShareNumberEditText.text.toString().toInt()
      var traceableSecretSharingClient = pjatk.secret.TraceableSecretSharingClient(n, k, secretContent)
      var toTypedArray = traceableSecretSharingClient.traceableDataShares.values.toMutableList()
      val newSecret = Secret(
        "!",
        "" + LocalDateTime.now(),
        Recipient.self().nickname.toString() + Recipient.self().id,
        n,
        k,
        toTypedArray.map { it -> Share(it.shareHash.toString(), it.encryptedShare) }.toMutableList()
      )
      SignalDatabase.secrets.add(newSecret)
      println(SignalDatabase.secrets)

      var cursor = SignalDatabase.messages.getConversation(4);
      var messageId = 0;

      if (cursor.moveToFirst()) {
        do {
          val messageBody = cursor.getString(cursor.getColumnIndex("body"))
          messageId = cursor.getInt(cursor.getColumnIndex("_id"))
          println(">> " + messageId + " " + messageBody)
          // ... access other columns
        } while (cursor.moveToNext())
      }
      cursor.close()

      //RecipientUtil.setAndSendUniversalExpireTimerIfNecessary(AppDependencies.application, Recipient.resolved(RecipientId.from(6)), 1L )
//      val attachment = org.thoughtcrime.securesms.backup.proto.Attachment(fileUri,)

      var attachmentMessage = OutgoingMessage(

        Recipient.self(),
        SlideDeck(UriAttachment(
          fileUri!!,
          "text/plain8",
          AttachmentTable.TRANSFER_PROGRESS_DONE,
          secretContent?.length?.toLong() ?: 100,
          0,
          0,
          fileUri?.let { it1 -> getFileNameFromUri(it1) },
          null,
          false,
          false,
          false,
          false,
          null,
          null,
          null,
          null,
          null
        )),
        Gson().toJson(newSecret),
        System.currentTimeMillis()

      )
      println("!! " + Gson().toJson(newSecret))
      var message = OutgoingMessage.text(
        Recipient.self(),
//        Recipient.resolved(RecipientId.from(6)),
        "Udostępniony został fragment sekretu. Znajdziesz go w panelu secrets w aplikacji. " + messageId + "X".repeat(2048),
        0
      )
      MessageSender.send(
        AppDependencies.application,
        attachmentMessage,
        -1,
        MessageSender.SendType.SIGNAL,
        null,
        null
      )
      message = OutgoingMessage.text(
        Recipient.self(),
//        Recipient.resolved(RecipientId.from(6)),
        "SEKRET" + messageId,
        0
      )
      MessageSender.send(
        AppDependencies.application,
        message,
        -1,
        MessageSender.SendType.SIGNAL,
        null,
        null
      )
      cursor = SignalDatabase.messages.getConversation(SignalDatabase.threads.getThreadIdFor(Recipient.self().id).asOptional().orElse(0));

      cursor.moveToFirst()
      messageId = cursor.getInt(cursor.getColumnIndex("_id"))
      println("Last message ID = " + messageId)
      MessageSender.sendRemoteDelete(messageId.toLong())
      cursor.close()
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