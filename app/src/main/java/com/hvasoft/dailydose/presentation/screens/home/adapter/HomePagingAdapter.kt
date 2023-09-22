package com.hvasoft.dailydose.presentation.screens.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.databinding.ItemSnapshotBinding
import com.hvasoft.dailydose.domain.common.extension_functions.isCurrentUserOwner
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.common.getLikeCountText
import com.hvasoft.dailydose.presentation.screens.common.getPostTimeLabel
import com.hvasoft.dailydose.presentation.screens.common.loadImage
import com.hvasoft.dailydose.presentation.screens.common.setDynamicTint

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
                    cbLike.text = snapshot.likeCount
                    cbLike.isChecked = snapshot.isLikedByCurrentUser
                    cbLike.setDynamicTint(
                        checkedColor = context.getColor(R.color.primaryColor),
                        uncheckedColor = context.getColor(R.color.unSelectedColor)
                    )
                    imgPhoto.loadImage(snapshot.photoUrl)
                    tvUserName.text = snapshot.userName
                    tvPostTimeLabel.getPostTimeLabel(snapshot.dateTime)
                    imgPhotoProfile.loadImage(snapshot.userPhotoUrl, isCircle = true)
                    btnDelete.visibility =
                        if (snapshot.isCurrentUserOwner())
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
                cbLike.setOnClickListener {
                    cbLike.text = cbLike.getLikeCountText()
                    listener.onSetLikeSnapshot(snapshot, cbLike.isChecked)
                }
                btnShare.setOnClickListener {
                    listener.onShareSnapshot(snapshot)
                }
            }
        }
    }

    class SnapshotDiffCallback : DiffUtil.ItemCallback<Snapshot>() {
        override fun areItemsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem.snapshotKey == newItem.snapshotKey

        override fun areContentsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem == newItem
    }
}