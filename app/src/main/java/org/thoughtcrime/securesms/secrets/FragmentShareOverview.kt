package org.thoughtcrime.securesms.secrets

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeBase64
import org.signal.core.util.toOptional
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.secrets.database.Secret
import org.thoughtcrime.securesms.secrets.database.Share
import org.thoughtcrime.securesms.secrets.model.ShareRequest
import org.thoughtcrime.securesms.secrets.model.Status
import org.whispersystems.signalservice.api.push.ServiceId
import pjatk.secret.crypto.AesCryptoUtils
import pjatk.secret.crypto.RsaCryptoUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FragmentShareOverview : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_share_overview, container, false)
  }
}

class ShareAdapter(private val shares: List<Share>) : RecyclerView.Adapter<ShareAdapter.ViewHolder>() {

  private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
  private var isOwner: Boolean = false

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val shareHashTextView: TextView = itemView.findViewById(R.id.textView_shareHash)
    val shareButton: Button = itemView.findViewById(R.id.button_shareShare)
    val downloadButton: Button = itemView.findViewById(R.id.button_downloadShare)
    val removeButton: Button = itemView.findViewById(R.id.button_removeShare)
    val updateLastAccessButton: Button = itemView.findViewById(R.id.button_lastAccess)
    val contactsSpinner: Spinner = itemView.findViewById(R.id.spinner_contacts)
    val isShared: TextView = itemView.findViewById(R.id.textView_isShared)
    val lastAccess: TextView = itemView.findViewById(R.id.textView_lastAccess)
    val checkboxKey: CheckBox = itemView.findViewById(R.id.checkBox_key)
    val checkboxShare: CheckBox = itemView.findViewById(R.id.checkBox_share)

    //    val shareRequestsPreferences: SharedPreferences = itemView.context.getSharedPreferences("share_requests", Context.MODE_PRIVATE)/**/
    val privateRsaKeysForShares: SharedPreferences = itemView.context.getSharedPreferences("share_private_keys", Context.MODE_PRIVATE)
    val secretSharedPreferences: SharedPreferences = itemView.context.getSharedPreferences("secret_preferences", Context.MODE_PRIVATE)
    val decryptionKeyRequestPreferences: SharedPreferences = itemView.context.getSharedPreferences("decryptionKey_requests", Context.MODE_PRIVATE)

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_share_overview, parent, false)
    return ViewHolder(view)
  }

  @OptIn(ExperimentalEncodingApi::class)
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val share = shares[position]

//    (holder.itemView.context as? LifecycleOwner)?.let { //TODO
//      SignalDatabase.liveSecrets.observe(it) {
//      }
//    } ?: run { println("No lifecycle owner found. ") }

    val adapter = RecipientAdapter(holder.itemView.context, SignalDatabase.recipients.getRegistered().toTypedArray())
    val spinner = holder.contactsSpinner
    spinner.adapter = adapter

    updateView(holder, share)
    SignalDatabase.liveSecrets.observeForever { //TODO
      println("!! Updating view")
      updateView(holder, share)
    }

    holder.shareButton.setOnClickListener {
      if (isOwner) {
        if (share.isShared) {  //Request share
          requestShare(share, holder)
        } else {
          shareShareWithTrustee(share, holder) //Share share
        }
      } else {
        if (share.isRequested && !holder.checkboxKey.isChecked) { //TODO
          requestDecryptionKey(holder, share) //request key from access machine
        } else {
          sendBack(share, holder)
        }
      }
      updateView(holder, share)
    }

    holder.updateLastAccessButton.setOnClickListener {
      coroutineScope.launch {
        val response = SecretServerApiUtils.makeApiRequest {
          SecretServerApiUtils.apiService.lastShareAccess(URLEncoder.encode(share.hash, StandardCharsets.UTF_8.toString()))
        }
        println("!! $response ${response.body()}")
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        holder.lastAccess.text = "Accessed: ${ZonedDateTime.parse(response.body()?.get(0)?.instant).format(pattern)} "
      }
      updateView(holder, share)
    }


    holder.downloadButton.setOnClickListener {
      val request = holder.decryptionKeyRequestPreferences.getString(share.hash, "")
      val requestObject = Gson().fromJson(request, ShareRequest::class.java)
      coroutineScope.launch {
        println("Checking secret key request status with transactionId ${requestObject?.transactionId} and data hash ${share.hash}")
        val response = SecretServerApiUtils.makeApiRequest {
          SecretServerApiUtils.apiService.secretKeyResponse(requestObject.transactionId!!)
        }

        println("!! $response ${response.body()}")

        if (response.body() != null) {
          Toast.makeText(holder.itemView.context, response.body()?.message, Toast.LENGTH_LONG).show()
        }

        response.body()?.let { body ->
          holder.isShared.text = body.message

          body.recryptedKey?.let { key ->
            println("Recrypted key found, persisting it to share request")

            requestObject?.let {
              requestObject.recryptedKey = key.decodeBase64()?.toByteArray()
              requestObject.status = Status.SUCCESS
              holder.decryptionKeyRequestPreferences.edit().putString(share.hash, Gson().toJson(requestObject)).apply()
              holder.checkboxKey.isChecked = true
            }
          }
        }
        updateView(holder, share)
      }
    }

    holder.removeButton.setOnClickListener {
//      decrypt key

      val request = Gson().fromJson(holder.decryptionKeyRequestPreferences.getString(share.hash, ""), ShareRequest::class.java)
      val privateRecryptionKey = holder.privateRsaKeysForShares.getString(share.hash, "")
      var decoded = Base64.decode(privateRecryptionKey!!)

      val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(decoded)

      var privateKey = KeyFactory.getInstance("RSA").generatePrivate(pkcS8EncodedKeySpec)

      var shareDecryptionKey = RsaCryptoUtils.getInstance().decrypt(request.recryptedKey!!, privateKey)
      var decryptionKeyObject = SecretKeySpec(shareDecryptionKey, "AES")
      var decryptedShare = AesCryptoUtils.getInstance().decrypt(share.data, decryptionKeyObject)

      share.decryptedData = decryptedShare
      share.isDecrypted = true

      println("DECRYPTED! ")

      //requestShare(share, holder)
      updateView(holder, share)
    }
  }

  private fun sendBack(share: Share, holder: ViewHolder) { //TODO
    val response = holder.decryptionKeyRequestPreferences.getString(share.hash, "")
    println("Sending back encrypted key and encrypted share to owner $response")
    MessageUtils.sendMessage(Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId), "RESPONSE_SHARE $response")
  }

  private fun updateView(holder: ViewHolder, share: Share) {
    holder.shareHashTextView.text = share.name.orEmpty()
    isOwner = share.owner == Recipient.self().aci.get().toString()

    /*    if (share.shareState == Share.ShareState.RETURNED) {
          holder.removeButton.isVisible = true
        }

        share.shareStates.forEach {
          holder.isShared.text = "${holder.isShared.text} $it |"
        }

        when (share.shareState) {
          Share.ShareState.CREATED -> {
    //        holder.isShared.text = "${holder.isShared.text} CREATED | "
          }
          Share.ShareState.ENCRYPTED -> {
    //        holder.isShared.text = "${holder.isShared.text} Encrypted | "
          }
          Share.ShareState.SHARED -> {
    //        holder.isShared.text = "${holder.isShared.text} Shared | "
          }
          Share.ShareState.REQUESTED -> TODO()
          Share.ShareState.ACCESS_KEY_REQUESTED -> TODO()
          Share.ShareState.ACCESS_PERSISTED -> TODO()
          Share.ShareState.ACCESS_KEY_RECEIVED -> TODO()
          Share.ShareState.RETURNED -> TODO()
          Share.ShareState.DECRYPTED -> TODO()
          Share.ShareState.RECREATED -> TODO()
          null -> TODO()
        }*/


    if (isOwner) {
      holder.downloadButton.isVisible = false
      holder.checkboxShare.isChecked = !share.isShared
      holder.isShared.text = if (share.isShared) "Status: Already shared" else "Status: Not shared"
      holder.shareButton.text = if (share.isShared) "request" else "share"
      if (share.isRequested) {
        holder.shareButton.text = "Requested"
        holder.shareButton.isEnabled = false
        holder.contactsSpinner.setSelection((holder.contactsSpinner.adapter as RecipientAdapter).getTrusteePosition(share))
        if (share.isReturned) {
          holder.isShared.text = "Returned"
          holder.shareButton.text = "Returned"
          holder.checkboxShare.isChecked = true
          holder.checkboxKey.isChecked = true
          holder.removeButton.isVisible = true;
          if (share.isDecrypted) {
            holder.removeButton.isEnabled = false;
            holder.shareButton.text = "Decrypted"
            holder.isShared.text = "Share decrypted."
          }
        }
      }
    } else {
      holder.shareButton.text = if (share.isRequested) "Fetch key. " else "Not requested"
      holder.downloadButton.isEnabled = false
      holder.checkboxShare.isChecked = true
      holder.contactsSpinner.setSelection((holder.contactsSpinner.adapter as RecipientAdapter).getSharePosition(share))
      holder.contactsSpinner.isEnabled = false
      holder.isShared.text = if (share.isShared) "Shared with you" else ""
      if (share.isRequested) {
        holder.isShared.text = "Share requested!"
        holder.shareButton.isEnabled = true
        holder.downloadButton.text = "Update"
        if (share.isForwardedToAccessMachine) {
          holder.shareButton.isEnabled = false
          holder.shareButton.text = "Send back (key needed)"
          holder.downloadButton.isEnabled = true

          if (holder.checkboxShare.isChecked && holder.checkboxKey.isChecked) {
            holder.shareButton.isEnabled = true
            holder.shareButton.text = "Send back"
          }
        }
      } else {
        holder.shareButton.isEnabled = false;
        holder.downloadButton.isEnabled = false;
      }
    }

  }

  private fun updateShareStatus(holder: ViewHolder, share: Share) {
    SignalDatabase.secrets[share.hashOfSecret]?.let {
      holder.secretSharedPreferences.edit().putString(share.hashOfSecret, Gson().toJson(it)).apply()
      println(Gson().toJson(it))
    }
  }

  private fun shareShareWithTrustee(share: Share, holder: ViewHolder) {
    val secret = Secret(
      share.name?.split("/")?.get(0).toString(),
      Recipient.self().aci.get().toString(),
      share.k,
      0,
      mutableListOf(share)
    )

    val recipient = Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId)

    MessageUtils.sendMessage(
      recipient,
      "SEKRET " + Gson().toJson(secret)
    )

    share.sharedWithServiceId = recipient.aci.get().toString()
    share.isShared = true

    updateShareStatus(holder, share)
  }

  /**
   * Requests a share from the trustee.
   * Sends a message in format "REQUEST_FOR_SHARE_AES_KEY {shareRequest}" containing
   *  - encrypted share hash.
   *  - hash of the secret that share is part of
   *  - requestor (self)
   *  - public key of the requestor (self, generated here)
   */
  @OptIn(ExperimentalEncodingApi::class)
  private fun requestShare(share: Share, holder: ViewHolder) {
    val keys = RsaCryptoUtils.getInstance().generateKeyPair()
    holder.privateRsaKeysForShares.edit().putString(share.hash, Base64.encode(keys.private.encoded)).apply()

    val shareRequest = ShareRequest(
      Recipient.self().aci.get().toString(),
      share.hashOfSecret.orEmpty(),
      share.hash,
      keys.public.encoded
    )

    println("Share requested ${shareRequest}")
    share.isRequested = true
    MessageUtils.sendMessage(Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId), "REQUEST_FOR_SHARE_AES_KEY " + Gson().toJson(shareRequest))

    updateShareStatus(holder, share)
  }

  private fun requestDecryptionKey(holder: ViewHolder, share: Share) {
    val decryptionRequest = Gson().fromJson(holder.decryptionKeyRequestPreferences.getString(share.hash, ""), ShareRequest::class.java)
    coroutineScope.launch {

      val response = SecretServerApiUtils.makeApiRequest {
        SecretServerApiUtils.apiService.secretKeyRequest(
          SecretKeyRequest(
            decryptionRequest.publicKey,
            share.data
          )
        )
      }
      println("Requesting secret key access with public key ${decryptionRequest.publicKey} and data hash ${share.hash}")
      println("!! " + response)

      decryptionRequest.transactionId = response.body()
      decryptionRequest.status = Status.IN_PROGRESS
      holder.decryptionKeyRequestPreferences.edit().putString(share.hash, Gson().toJson(decryptionRequest)).apply()

      println(decryptionRequest)
      holder.isShared.text = decryptionRequest.transactionId

      share.isForwardedToAccessMachine = true
      updateShareStatus(holder, share)
    }
  }

  override fun getItemCount(): Int {
    return shares.size
  }
}

class RecipientAdapter(context: Context, private val recipients: Array<RecipientId>) : ArrayAdapter<RecipientId>(context, 0, recipients) {
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val recipient = getItem(position)
    val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
    val textView = view.findViewById<TextView>(android.R.id.text1)

    "${SignalDatabase.recipients.getRecord(recipient!!).signalProfileName}".also { textView.text = it }

    return view
  }

  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
    return getView(position, convertView, parent)
  }

  fun getSharePosition(share: Share): Int {
    return share.toOptional()
      .map { s -> s.owner }
      .map { s -> recipients.indexOf(RecipientId.from(ServiceId.Companion.parseOrThrow(s!!))) }
      .orElse(0)
  }

  fun getTrusteePosition(share: Share): Int {
    return share.toOptional()
      .map { s -> s.sharedWithServiceId }
      .map { s -> recipients.indexOf(RecipientId.from(ServiceId.Companion.parseOrThrow(s!!))) }
      .orElse(0)
  }

}
