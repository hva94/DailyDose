package com.hvasoft.dailydose.presentation.screens.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.utils.Constants
import com.hvasoft.dailydose.databinding.ItemSnapshotBinding
import com.hvasoft.dailydose.domain.common.extension_functions.getLikeCountText
import com.hvasoft.dailydose.domain.common.extension_functions.isLikeChecked
import com.hvasoft.dailydose.domain.model.Snapshot

class HomePagingAdapter(private val listener: OnClickListener) :
    PagingDataAdapter<Snapshot, ViewHolder>(SnapshotDiffCallback()) {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_snapshot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val snapshot = getItem(position)

        with(holder as ViewHolder) {
            snapshot?.let { snapshot ->
                setListener(snapshot)
                with(binding) {
                    tvTitle.text = snapshot.title
                    cbLike.text = snapshot.getLikeCountText()
                    cbLike.isChecked = snapshot.isLikeChecked()
                    Glide.with(context)
                        .load(snapshot.photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(imgPhoto)
                    tvUserName.text = snapshot.userName
                    Glide.with(context)
                        .load(snapshot.userPhotoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .circleCrop()
                        .into(imgPhotoProfile)
                    btnDelete.visibility =
                        if (snapshot.idUserOwner == Constants.currentUser.uid)
                            View.VISIBLE else View.INVISIBLE
                }
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemSnapshotBinding.bind(itemView)

        fun setListener(snapshot: Snapshot) {
            with(binding) {
                btnDelete.setOnClickListener {
                    listener.onDeleteSnapshot(snapshot)
                }
                cbLike.setOnCheckedChangeListener { compoundButton, checked ->
                    if (compoundButton.isPressed) {
                        val oldLikes = cbLike.text.toString().toInt()
                        val newLikes = if (checked) oldLikes + 1 else oldLikes - 1
                        cbLike.text = newLikes.toString()
                        listener.onSetLikeSnapshot(snapshot, checked)
                    }
                }
                btnShare.setOnClickListener {
                    listener.onShareSnapshot(snapshot)
                }
            }
        }
    }

    class SnapshotDiffCallback : DiffUtil.ItemCallback<Snapshot>() {
        override fun areItemsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem.snapshotId == newItem.snapshotId

        override fun areContentsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem == newItem
    }
}