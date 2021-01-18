package io.igrant.mobileagent.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.WriterException
import io.igrant.mobileagent.R
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.utils.PermissionUtils
import org.hyperledger.indy.sdk.IndyException
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.wallet.Wallet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ExecutionException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class QrDemoFragment : Fragment() {

    var ivQrCode: ImageView? = null
    var tvShare: TextView? = null
    var tvScan: TextView? = null
    var etWallet: EditText? = null
    var tvCreate: TextView? = null
    var tvDid: TextView? = null
    var tvKey: TextView? = null
    var etMessage: EditText? = null
    var tvSend: TextView? = null
    var llProgressBar: LinearLayout? = null
    var ivMessage: ImageView? = null
    var tvReceive: TextView? = null
    var tvMessage: TextView? = null

    private var key: String? = null
    private var myDid: String? = null
    private var wallet: Wallet? = null

    private var otherDid = ""
    private var otherKey = ""

    private var bitmap: Bitmap? = null

    private val PICK_IMAGE_REQUEST = 101
    val PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA)

    private val REQUEST_CODE_QR_SCAN = 201
    private val REQUEST_CODE_QR_MESSAGE = 202

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_demo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initListener()
    }

    private fun initViews(view: View) {
        tvScan = view.findViewById(R.id.tvScan)
        tvShare = view.findViewById(R.id.tvShare)
        tvCreate = view.findViewById(R.id.tvCreate)
        etWallet = view.findViewById(R.id.etWalletName)
        ivQrCode = view.findViewById(R.id.ivQrCode)
        tvDid = view.findViewById(R.id.tvDid)
        tvKey = view.findViewById(R.id.tvKey)
        etMessage = view.findViewById(R.id.etSendMessage)
        tvSend = view.findViewById(R.id.tvSend)
        llProgressBar = view.findViewById(R.id.llProgressBar)
        ivMessage = view.findViewById(R.id.ivMessage)
        tvReceive = view.findViewById(R.id.tvRecieveMessage)
        tvMessage = view.findViewById(R.id.tvMessage)
    }

    private fun initListener() {
        tvSend!!.setOnClickListener {
            if (etMessage!!.text.toString() != "") {
                val receivers =
                    JSONArray(arrayOf<String>(otherKey))

                val WALLET = etWallet!!.text.toString()
                val TYPE = "default"
                val WALLET_CREDENTIALS = JSONObject()
                    .put("key", "key")
                    .toString()
                val WALLET_CONFIG = JSONObject()
                    .put("id", WALLET)
                    .put("storage_type", TYPE)
                    .toString()
                wallet = Wallet.openWallet(WALLET_CONFIG, WALLET_CREDENTIALS).get()

                var packedMessage = Crypto.packMessage(
                    wallet,
                    receivers.toString(),
                    key,
                    etMessage!!.text.toString().toByteArray()
                ).get()
                var cryptoMessage: String = String(packedMessage)
                Log.d("milna", "initListener: $cryptoMessage")

                generateQRCode(cryptoMessage)

                if (wallet != null) {
                    try {
                        wallet!!.closeWallet().get()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        tvScan!!.setOnClickListener {
            if (PermissionUtils.hasPermissions(
                    requireActivity(),
                    true,
                    PICK_IMAGE_REQUEST,
                    PERMISSIONS
                )
            ) {
                val i = Intent(context, QrCodeActivity::class.java)
                startActivityForResult(
                    i,
                    REQUEST_CODE_QR_SCAN
                )
            }
        }

        tvReceive!!.setOnClickListener {
            if (PermissionUtils.hasPermissions(
                    requireActivity(),
                    true,
                    PICK_IMAGE_REQUEST,
                    PERMISSIONS
                )
            ) {
                val i = Intent(context, QrCodeActivity::class.java)
                startActivityForResult(
                    i,
                    REQUEST_CODE_QR_MESSAGE
                )
            }
        }

        tvShare!!.visibility = View.GONE
        tvShare!!.setOnClickListener {
            llProgressBar!!.visibility = View.VISIBLE
            val jObjectData = JSONObject()
            // Create Json Object using Facebook Data
            try {
                jObjectData.put("did", myDid)
                jObjectData.put("verifiedKey", key)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            val manager = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = manager.defaultDisplay
            val point = Point()
            display.getSize(point)
            val width = point.x
            val height = point.y
            var smallerDimension = if (width < height) width else height
            smallerDimension = smallerDimension * 3 / 4
            val qrgEncoder = QRGEncoder(
                jObjectData.toString(),
                null,
                QRGContents.Type.TEXT,
                smallerDimension
            )
            try {
                // Getting QR-Code as Bitmap
                bitmap = qrgEncoder.encodeAsBitmap()
                // Setting Bitmap to ImageView
                ivQrCode!!.setImageBitmap(bitmap)
            } catch (e: WriterException) {
                Log.v("milna", e.toString())
            }
            llProgressBar!!.visibility = View.GONE
        }

        tvCreate!!.setOnClickListener {
            llProgressBar!!.visibility = View.VISIBLE
            try {
                val WALLET = etWallet!!.text.toString()
                val TYPE = "default"
                val WALLET_CREDENTIALS = JSONObject()
                    .put("key", "key")
                    .toString()
                val WALLET_CONFIG = JSONObject()
                    .put("id", WALLET)
                    .put("storage_type", TYPE)
                    .toString()
                try {
                    Wallet.createWallet(WALLET_CONFIG, WALLET_CREDENTIALS).get()
                } catch (e: ExecutionException) {
                    println(e.message)
                    if (e.message!!.indexOf("WalletExistsException") >= 0) {
                        // ignore
                    } else {
                        throw RuntimeException(e)
                    }
                }
                wallet = Wallet.openWallet(WALLET_CONFIG, WALLET_CREDENTIALS).get()
                val myDidResult =
                    Did.createAndStoreMyDid(wallet, "{}").get()
                myDid = myDidResult.did
                Snackbar.make(tvCreate!!, "DID Created", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
                key = Did.keyForLocalDid(wallet, myDid).get()
                Snackbar.make(tvCreate!!, "Key Created", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
                Log.d("milna", "keyForLocalDid:$key")
                tvShare!!.visibility = View.VISIBLE
            } catch (e: IndyException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } finally {
                if (wallet != null) {
                    try {
                        wallet!!.closeWallet().get()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            llProgressBar!!.visibility = View.GONE
        }
    }

    private fun generateQRCode(cryptoMessage: String) {
        llProgressBar!!.visibility = View.VISIBLE
        val manager = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        val width = point.x
        val height = point.y
        var smallerDimension = if (width < height) width else height
        smallerDimension = smallerDimension * 3 / 4
        val qrgEncoder = QRGEncoder(
            cryptoMessage,
            null,
            QRGContents.Type.TEXT,
            smallerDimension
        )
        try {
            // Getting QR-Code as Bitmap
            bitmap = qrgEncoder.encodeAsBitmap()
            // Setting Bitmap to ImageView
            ivMessage!!.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Log.v("milna", e.toString())
        }
        tvReceive!!.visibility = View.VISIBLE
        llProgressBar!!.visibility = View.GONE
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null) return
            val json =
                data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult")
            var jsonObject: JSONObject? = null
            try {
                jsonObject = JSONObject(json)
                val did = jsonObject.getString("did")
                val verification_key = jsonObject.getString("verifiedKey")
                otherDid = did
                otherKey = verification_key
                tvDid!!.text = "DID : $did"
                tvKey!!.text = "Key : $verification_key"

                etMessage!!.visibility = View.VISIBLE
                tvSend!!.visibility = View.VISIBLE
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else if (requestCode == REQUEST_CODE_QR_MESSAGE) {
            if (data == null) return
            val packedMessage =
                data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult")

            val WALLET = etWallet!!.text.toString()
            val TYPE = "default"
            val WALLET_CREDENTIALS = JSONObject()
                .put("key", "key")
                .toString()
            val WALLET_CONFIG = JSONObject()
                .put("id", WALLET)
                .put("storage_type", TYPE)
                .toString()
            wallet = Wallet.openWallet(WALLET_CONFIG, WALLET_CREDENTIALS).get()


            var unPackedMessage = Crypto.unpackMessage(wallet, packedMessage!!.toByteArray()).get()
            var cryptoUnMessage: String = String(unPackedMessage)
            Log.d("milna", "initListener: $cryptoUnMessage")

            var jsonObject: JSONObject? = null
            try {
                jsonObject = JSONObject(cryptoUnMessage)
                val message = jsonObject.getString("message")

                tvMessage!!.text = "Message received : $message"
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            if (wallet != null) {
                try {
                    wallet!!.closeWallet().get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}