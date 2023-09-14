package com.hvasoft.dailydose.presentation.homeScreen.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.utils.DataConstants
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.databinding.ItemSnapshotBinding

class HomeAdapter(private val listener: OnClickListener) :
    ListAdapter<Snapshot, ViewHolder>(SnapshotDiffCallback()) {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_snapshot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val snapshot = getItem(position)

        with(holder as ViewHolder) {
            setListener(snapshot)

            with(binding) {
                tvTitle.text = snapshot.title
                cbLike.text = snapshot.likeList.keys.size.toString()
                cbLike.isChecked =
                    snapshot.likeList.containsKey(DataConstants.currentUser.uid)

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
                    if (snapshot.idUserOwner == DataConstants.currentUser.uid)
                        View.VISIBLE else View.INVISIBLE
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemSnapshotBinding.bind(itemView)

        fun setListener(snapshot: Snapshot) {
            binding.btnDelete.setOnClickListener {
                listener.onDeleteSnapshot(snapshot)
            }
            binding.cbLike.setOnCheckedChangeListener { compoundButton, checked ->
                if (compoundButton.isPressed) {
                    val oldLikes = binding.cbLike.text.toString().toInt()
                    val newLikes = if (checked) oldLikes + 1 else oldLikes - 1
                    binding.cbLike.text = newLikes.toString()
                    listener.onSetLikeSnapshot(snapshot, checked)
                }
            }
        }
    }

    class SnapshotDiffCallback : DiffUtil.ItemCallback<Snapshot>() {
        override fun areItemsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Snapshot, newItem: Snapshot): Boolean =
            oldItem == newItem
    }
}