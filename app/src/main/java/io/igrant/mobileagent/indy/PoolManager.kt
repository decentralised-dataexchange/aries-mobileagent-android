package io.igrant.mobileagent.indy

import org.hyperledger.indy.sdk.pool.Pool

object PoolManager {
    fun setPool(pool: Pool) {
        this.pool = pool
    }

    private val TAG = "PoolManager"
    private var pool: Pool? = null

    private lateinit var walletConfig: String
    private lateinit var walletCredentials: String
    private lateinit var type: String
    private lateinit var walletName: String

    val getPool: Pool?
        get() {
            if (pool == null) {
//
            }
            return pool
        }
}