package com.hvasoft.dailydose.presentation.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.di.IoDispatcher
import com.hvasoft.dailydose.domain.common.error_handling.ErrorHandler.handleError
import com.hvasoft.dailydose.domain.common.error_handling.toError
import com.hvasoft.dailydose.domain.common.error_handling.validateErrorCode
import com.hvasoft.dailydose.domain.common.response_handling.Result
import com.hvasoft.dailydose.domain.common.response_handling.fold
import com.hvasoft.dailydose.domain.use_case.home.HomeUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeUseCases: HomeUseCases,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _snapshotsState = MutableLiveData<HomeState>(HomeState.Loading)
    val snapshotsState: LiveData<HomeState> = _snapshotsState

    init {
        getSnapshots()
    }

    private fun getSnapshots() {
        _snapshotsState.value = HomeState.Loading
        viewModelScope.launch(dispatcher) {
            handleResultSnapshotList(homeUseCases.getSnapshots())
        }
    }

    fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean) {
        viewModelScope.launch(dispatcher) {
            handleResultBoolean(homeUseCases.isLikeChanged(snapshot, checked))
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        viewModelScope.launch(dispatcher) {
            handleResultSnapshotList(homeUseCases.deleteSnapshot(snapshot))
        }
    }

    fun refreshSnapshots() {
        getSnapshots()
    }

    private fun handleResultSnapshotList(result: Result<List<Snapshot>>) {
        try {
            result.fold(
                onSuccess = { snapshots ->
                    if (snapshots.isEmpty()) _snapshotsState.postValue(HomeState.Empty)
                    else _snapshotsState.postValue(HomeState.Success(snapshots))
                },
                onError = { code, _ ->
                    val error = handleError(code.validateErrorCode())
                    _snapshotsState.postValue(HomeState.Failure(error))
                },
                onException = {
                    val error = handleError(it.toError())
                    _snapshotsState.postValue(HomeState.Failure(error))
                }
            )
        } catch (e: Exception) {
            val error = handleError(e.toError())
            _snapshotsState.postValue(HomeState.Failure(error))
        }
    }

    private fun handleResultBoolean(result: Result<Boolean>) {
        try {
            result.fold(
                onSuccess = {},
                onError = { code, _ ->
                    val error = handleError(code.validateErrorCode())
                    _snapshotsState.postValue(HomeState.Failure(error))
                },
                onException = {
                    val error = handleError(it.toError())
                    _snapshotsState.postValue(HomeState.Failure(error))
                }
            )
        } catch (e: Exception) {
            val error = handleError(e.toError())
            _snapshotsState.postValue(HomeState.Failure(error))
        }
    }
}