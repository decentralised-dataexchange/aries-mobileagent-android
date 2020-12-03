package io.igrant.mobileagent.tasks

import android.content.Context
import android.os.AsyncTask
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager

class OpenWalletTask(private val commonHandler: CommonHandler) :
    AsyncTask<Void, Void, Void>() {

    private val TAG = "OpenWalletTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        WalletManager.getWallet
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.taskCompleted()
    }
}