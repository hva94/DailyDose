package com.hvasoft.dailydose.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.di.IoDispatcher
import com.hvasoft.dailydose.domain.common.response_handling.Resource
import com.hvasoft.dailydose.domain.common.response_handling.Status
import com.hvasoft.dailydose.domain.interactor.home.GetSnapshotsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSnapshotsUseCase: GetSnapshotsUseCase,
//    private val isLikeChangedUseCase: IsLikeChangedUseCase,
//    private val deleteSnapshotUseCase: DeleteSnapshotUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

//    private val _snapshotsState = MutableLiveData<HomeState>(HomeState.Loading)
//    val snapshotsState: LiveData<HomeState> = _snapshotsState

    private var _snapshotsState = MutableStateFlow<HomeState>(HomeState.Loading)
    val snapshotsState = _snapshotsState.asStateFlow()

    init {
        getSnapshots()
    }

    fun getSnapshots() {
        _snapshotsState.value = HomeState.Loading
        viewModelScope.launch(dispatcher) {
            handleUseCaseResult(getSnapshotsUseCase())
        }
    }

    fun setLikeSnapshot(snapshot: Snapshot, checked: Boolean) {
        viewModelScope.launch(dispatcher) {
//            handleResultBoolean(homeUseCases.isLikeChanged(snapshot, checked))
        }
    }

    fun deleteSnapshot(snapshot: Snapshot) {
        viewModelScope.launch(dispatcher) {
//            handleResultSnapshotList(homeUseCases.deleteSnapshot(snapshot))
        }
    }

    private fun handleUseCaseResult(resource: Resource<List<Snapshot>>) {
        try {
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { snapshots ->
                        Log.d("hva_test", "handleUseCaseResult: ${snapshots.size}")
                        if (snapshots.isEmpty())
                            _snapshotsState.update { HomeState.Empty }
                        else
                            _snapshotsState.update { HomeState.Success(snapshots) }
                    }
                }
                Status.ERROR -> _snapshotsState.update { HomeState.Failure(resource.errorMessage) }
                Status.LOADING -> _snapshotsState.update { HomeState.Loading }
            }
        } catch (e: Exception) {
//            val error = handleError(e.toError())
            _snapshotsState.update { HomeState.Failure(e.localizedMessage) }
        }
    }
}