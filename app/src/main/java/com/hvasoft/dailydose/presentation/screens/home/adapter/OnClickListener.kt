package com.hvasoft.dailydose.presentation.screens.home.adapter

import com.hvasoft.dailydose.data.model.Snapshot

interface OnClickListener {
    fun onSetLikeSnapshot(snapshot: Snapshot, isLike: Boolean)
    fun onDeleteSnapshot(snapshot: Snapshot)
    fun onShareSnapshot(snapshot: Snapshot)
}