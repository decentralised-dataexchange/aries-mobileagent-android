package io.igrant.mobileagent.handlers

import org.hyperledger.indy.sdk.pool.Pool

interface PoolHandler {
    fun taskCompleted(pool: Pool)
    fun taskStarted()
}