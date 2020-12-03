package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import io.igrant.mobileagent.activty.InitializeActivity
import io.igrant.mobileagent.handlers.PoolHandler
import io.igrant.mobileagent.indy.PoolUtils
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.pool.Pool

class PoolTask(private val poolHandler: PoolHandler) : AsyncTask<Void, Void, Void>() {
    lateinit var pool: Pool
    override fun doInBackground(vararg p0: Void?): Void? {
        Pool.deletePoolLedgerConfig("default")

        val pool = PoolUtils.createAndOpenPoolLedger()

        val acceptanceMech = Ledger.buildGetAcceptanceMechanismsRequest(null, -1, null).get()

        val response = Ledger.submitRequest(pool, acceptanceMech).get()

        val agreementResponse = Ledger.buildGetTxnAuthorAgreementRequest(null, null).get()

        val response2 = Ledger.submitRequest(pool, agreementResponse).get()

        this.pool = pool
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        poolHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        poolHandler.taskCompleted(pool)
    }
}