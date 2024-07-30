package org.thoughtcrime.securesms.secrets

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.secrets.database.Secret
import org.thoughtcrime.securesms.secrets.database.Share

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

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val share = shares[position]
    holder.shareHashTextView.text = share.hash

    holder.isShared.text = if (share.isShared) "Already shared" else "Not shared"


    val adapter = RecipientAdapter(holder.itemView.context, SignalDatabase.recipients.getRegistered().toTypedArray())
    val spinner = holder.contactsSpinner
    spinner.adapter = adapter;

//    val owner = SignalDatabase.recipients.getRecord(Recipient.self().id)

    holder.shareButton.setOnClickListener {

      if(share.isShared){

      } else {

        var secret = Secret(


          share.hashOfSecret!!,
          Recipient.self().profileName.toString(),
          Recipient.self().aci.get().toString(),
          share.k,
          0,
          mutableListOf(share)
        )


        MessageUtils.sendMessage(Recipient.resolved(holder.contactsSpinner.selectedItem as RecipientId), Gson().toJson(secret))
        share.isShared = true;
        holder.isShared.text = if (share.isShared) "Already shared" else "Not shared"

        holder.shareButton.text = "request"

      }

    }
    holder.downloadButton.setOnClickListener {
    }
    holder.removeButton.setOnClickListener {
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
