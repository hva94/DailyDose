package com.hvasoft.dailydose.data.local

import androidx.room.withTransaction
import javax.inject.Inject

fun interface HomeFeedTransactionRunner {
    suspend fun runInTransaction(block: suspend () -> Unit)
}

class RoomHomeFeedTransactionRunner @Inject constructor(
    private val database: DailyDoseDatabase,
) : HomeFeedTransactionRunner {

    override suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction {
            block()
        }
    }
}
