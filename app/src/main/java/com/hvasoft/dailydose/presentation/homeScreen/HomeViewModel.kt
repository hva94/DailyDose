package com.hvasoft.dailydose.presentation.homeScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.data.SnapshotsRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: SnapshotsRepository = SnapshotsRepository()
) : ViewModel() {

    private val snapshots = repository.snapshots
    fun getSnapshots(): LiveData<MutableList<Snapshot>?> = snapshots

    private val _snackbarMsg = MutableLiveData<Int>()
    val snackbarMsg: LiveData<Int> = _snackbarMsg

    fun refreshSnapshots() {
        viewModelScope.launch {
            repository.refreshSnapshots()
        }
    }

    fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean) {
        viewModelScope.launch {
            repository.setLikeSnapshot(snapshot, checked)
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshot) { isDeleted ->
                if (!isDeleted) _snackbarMsg.value = R.string.home_delete_photo_error
            }
        }
    }
}