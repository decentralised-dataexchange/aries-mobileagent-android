package io.igrant.mobileagent.indy

import org.hyperledger.indy.sdk.pool.Pool

object PoolManager {
    fun setPool(pool: Pool) {
        this.pool = pool
    }

    private var pool: Pool? = null

    val getPool: Pool?
        get() {
            if (pool == null) {
//
            }
            return pool
        }
}