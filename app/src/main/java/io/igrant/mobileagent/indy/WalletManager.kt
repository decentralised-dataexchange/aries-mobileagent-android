package io.igrant.mobileagent.indy

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import io.igrant.mobileagent.utils.DeviceUtils
import org.hyperledger.indy.sdk.IndyException
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.hyperledger.indy.sdk.wallet.Wallet
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ExecutionException


object WalletManager {

    private val TAG = "WalletManager"
    private var wallet: Wallet? = null

    private lateinit var walletConfig: String
    private lateinit var walletCredentials: String
    private lateinit var type: String
    private lateinit var walletName: String

    private var gson: Gson? = null

    val getGson: Gson
        get() {
            if (gson == null) {
                gson = Gson()
            }
            return gson ?: Gson()
        }


    val getWallet: Wallet?
        get() {
            if (wallet == null) {
                try {
                    walletName = DeviceUtils.getDeviceName()?:""
                    type = "default"
                    walletCredentials = JSONObject()
                        .put("key", "key")
                        .toString()
                    walletConfig = JSONObject()
                        .put("id", walletName)
                        .put("storage_type", type)
                        .toString()
                    try {
                        Wallet.createWallet(walletConfig, walletCredentials).get()
                    } catch (e: Exception) {
//                        Log.d(TAG, "${e.message}")
//                        if (e.message!!.indexOf("WalletExistsException") >= 0) {
//                            // ignore
//                        } else {
//                            throw RuntimeException(e)
//                        }
                    }
                    wallet = Wallet.openWallet(walletConfig, walletCredentials).get()

                    val respns =
                        Anoncreds.proverCreateMasterSecret(wallet, "IGrantMobileAgent-000001").get()

                    Log.d(TAG, "proverCreateMasterSecret : $respns")
//                    getMediatorConfig()
                } catch (e: IndyException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } finally {
//            if (wallet != null) {
//                try {
//                    wallet!!.closeWallet().get()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
                }
            }
            return wallet
        }

    val closeWallet: Int
        get() {
            if (wallet != null) {
                try {
                    wallet!!.closeWallet().get()
                    wallet = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return 0
        }

    fun closeSearchHandle(searchHandle: WalletSearch) {
        WalletSearch.closeSearch(searchHandle)
    }
}