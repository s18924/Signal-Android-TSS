package org.thoughtcrime.securesms.secrets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
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
import pjatk.secret.crypto.RsaCryptoUtils
import kotlin.io.encoding.ExperimentalEncodingApi

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FragmentShareOverview : Fragment() {
  private var param1: String? = null
  private var param2: String? = null

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
    return inflater.inflate(R.layout.fragment_share_overview, container, false)
  }

  companion object {
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
      FragmentShareOverview().apply {
        arguments = Bundle().apply {
          putString(ARG_PARAM1, param1)
          putString(ARG_PARAM2, param2)
        }
      }
  }
}

class ShareAdapter(private val shares: List<Share>) : RecyclerView.Adapter<ShareAdapter.ViewHolder>() {

  private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
  private var isOwner: Boolean = false;


  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val shareHashTextView: TextView = itemView.findViewById(R.id.textView_shareHash)
    val shareButton: Button = itemView.findViewById(R.id.button_shareShare)
    val downloadButton: Button = itemView.findViewById(R.id.button_downloadShare)
    val removeButton: Button = itemView.findViewById(R.id.button_removeShare)
    val contactsSpinner: Spinner = itemView.findViewById(R.id.spinner_contacts)
    val isShared: TextView = itemView.findViewById(R.id.textView_isShared)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_share_overview, parent, false)
    return ViewHolder(view)
  }

  @OptIn(ExperimentalEncodingApi::class)
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val share = shares[position]
    holder.shareHashTextView.text = share.hash
    isOwner = share.owner == Recipient.self().aci.get().toString()

    if (isOwner) {
      holder.isShared.text = if (share.isShared) "Already shared" else "Not shared"
      holder.shareButton.text = if (share.isShared) "request" else "share"
    } else {
      holder.isShared.text = if (share.isShared) "Shared with you" else ""
      if (share.isRequested) {
        holder.isShared.text = "Share requested!"
        holder.shareButton.isEnabled = true;
      } else {
        holder.shareButton.isEnabled = false;
      }
      holder.shareButton.text = if (share.isRequested) "Fetch key. " else "Not requested"
    }
//    share.


    val adapter = RecipientAdapter(holder.itemView.context, SignalDatabase.recipients.getRegistered().toTypedArray())
    val spinner = holder.contactsSpinner
    spinner.adapter = adapter

    if (isOwner) {
      holder.shareButton.setOnClickListener {
        if (share.isShared) {
          //Request share
          requestShare(share, holder)
//          requestDecryptionKey(holder, share)
        } else {
          shareShareWithTrustee(share, holder)
        }
      }
    } else {
      holder.shareButton.setOnClickListener {

        if (share.isRequested) {
          requestDecryptionKey(holder, share) //request key from access machine
        } else {
          //nothing?
//          shareShareWithTrustee(share, holder)

        }
        updateShareStatus(holder, share)
      }

    }


    holder.downloadButton.setOnClickListener {
      val sharedPrefs = holder.itemView.context.getSharedPreferences("share_requests", Context.MODE_PRIVATE)
      //var transactionId = sharedPrefs.getString(share.hash, null)

      val secretKeyRequestJob = coroutineScope.launch {
        val sharedPrefs = holder.itemView.context.getSharedPreferences("share_requests", Context.MODE_PRIVATE)


//        println("Checking secret key request status with transactionId ${transactionId} and data hash ${share.hash}")
        println("Checking secret key request status with transactionId ${sharedPrefs.getString(share.hash, "")} and data hash ${share.hash}")
        val response = SecretServerApiUtils.makeApiRequest {
          SecretServerApiUtils.apiService.secretKeyResponse(sharedPrefs.getString(share.hash, "2e7aca1f98115381555dff8b6dc560cc6e79dbd6083b6ca20f752e1faa43cae8")!!)
        }
        println("!! " + response)
        println(response.body())
        var body = response.body()
        body.toOptional().ifPresent {
          if (it.recryptedKey != null) {
            println("Recrypted key found, persisting it to share request")
            val sharedPreferences = holder.itemView.context.getSharedPreferences("secret_requests", Context.MODE_PRIVATE)
            var shareRequest = Gson().fromJson<ShareRequest>(sharedPreferences.getString(share.hash, ""), ShareRequest::class.java)
            shareRequest.recryptedKey = body?.recryptedKey?.decodeBase64()?.toByteArray()
            sharedPreferences.edit().putString(share.hash, Gson().toJson(shareRequest)).apply()
          }
          if (body != null) {
            holder.isShared.text = body.message
          }
        }

//        holder.itemView.context.getSharedPreferences("secret_requests", Context.MODE_PRIVATE).edit().putString(shareRequest.secretHash, Gson().toJson(shareRequest)).apply()

        if (!response.isSuccessful) {
          println("Error persisting secret")

        }
      }


    }
    holder.removeButton.setOnClickListener { requestShare(share, holder) }
  }

  private fun updateShareStatus(holder: ViewHolder, share: Share) {
    val sharedPrefs = holder.itemView.context.getSharedPreferences("secret_preferences", Context.MODE_PRIVATE)
    var shareString = sharedPrefs.getString(share.hashOfSecret, "")
    var secret = Gson().fromJson<Secret>(shareString, Secret::class.java)
    secret.shares.find { it -> it.hash == share.hash }?.isShared = true
    sharedPrefs.edit().putString(share.hashOfSecret, Gson().toJson(secret)).apply()
    println("!! " + Gson().toJson(secret))
  }

  private fun shareShareWithTrustee(share: Share, holder: ViewHolder) {
    var secret = Secret(
      Recipient.self().profileName.toString(),
      Recipient.self().aci.get().toString(),
      share.k,
      0,
      mutableListOf(share)
    )


    MessageUtils.sendMessage(Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId), "SEKRET " + Gson().toJson(secret))
    share.isShared = true
    holder.isShared.text = if (share.isShared) "Already shared" else "Not shared"

    holder.shareButton.text = "request"

    updateShareStatus(holder, share)
  }

  private fun requestShare(share: Share, holder: ViewHolder) {
    var keys = RsaCryptoUtils.getInstance().generateKeyPair()
    val sharedPrefsShareKey = holder.itemView.context.getSharedPreferences("share_private_keys", Context.MODE_PRIVATE)
    sharedPrefsShareKey.edit().putString(share.hash, keys.private.encoded.toString()).apply()
    println(sharedPrefsShareKey.all)

    var shareRequest = ShareRequest(
      Recipient.self().aci.get().toString(),
      share.hashOfSecret.orEmpty(),
      share.hash,
      keys.public.encoded
    )

    println("Share requested ${shareRequest}")
    MessageUtils.sendMessage(Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId), "REQUEST_SHARE " + Gson().toJson(shareRequest))
  }

  private fun requestDecryptionKey(holder: ViewHolder, share: Share) {
    val rsaCryptoUtils = RsaCryptoUtils.getInstance()
    val keyPair = rsaCryptoUtils.generateKeyPair()
    val sharedPrefsShareKey = holder.itemView.context.getSharedPreferences("share_private_keys", Context.MODE_PRIVATE)
    sharedPrefsShareKey.edit().putString(share.hash, keyPair.private.encoded.toString()).apply()
    val dataHash = rsaCryptoUtils.hash(share.data)
    val secretKeyRequestJob = coroutineScope.launch {

      val response = SecretServerApiUtils.makeApiRequest {
        SecretServerApiUtils.apiService.secretKeyRequest(
          SecretKeyRequest(
            keyPair.public.encoded,
            share.data
          )
        )
      }
      println("Requesting secret key access with public key ${keyPair.public} and data hash ${dataHash.contentToString()}")
      println("!! " + response)
      val sharedPrefs = holder.itemView.context.getSharedPreferences("share_requests", Context.MODE_PRIVATE)
      val transactionId = response.body()
      sharedPrefs.edit().putString(share.hash, transactionId).apply()

      println(transactionId)
      holder.isShared.text = transactionId
      if (!response.isSuccessful) {
        println("Error persisting secret")

      }
    }
  }

  override fun getItemCount(): Int {
    return shares.size
  }
}

class RecipientAdapter(context: Context, recipients: Array<RecipientId>) : ArrayAdapter<RecipientId>(context, 0, recipients) {
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val recipient = getItem(position)
    val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_item, parent, false)
    val textView = view.findViewById<TextView>(android.R.id.text1)

    "${SignalDatabase.recipients.getRecord(recipient!!).signalProfileName} - ${recipient}".also { textView.text = it }

    return view
  }

  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
    return getView(position, convertView, parent)
  }
}
