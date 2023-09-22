package com.hvasoft.dailydose.presentation.screens.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.databinding.FragmentHomeBinding
import com.hvasoft.dailydose.domain.model.Snapshot
import com.hvasoft.dailydose.presentation.screens.common.HomeFragmentListener
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import com.hvasoft.dailydose.presentation.screens.common.showPopUpMessage
import com.hvasoft.dailydose.presentation.screens.home.adapter.HomePagingAdapter
import com.hvasoft.dailydose.presentation.screens.home.adapter.OnClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(), HomeFragmentListener, OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var homePagingAdapter: HomePagingAdapter

    private var hostActivityListener: HostActivityListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        hostActivityListener = activity as HostActivityListener
    }

    override fun onResume() {
        super.onResume()
        onRefresh()
    }

    override fun onDetach() {
        super.onDetach()
        hostActivityListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hostActivityListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        homePagingAdapter = HomePagingAdapter(this)

        binding.homeRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.homePagingAdapter
        }
    }

    private fun setupViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                with(binding) {
                    homeViewModel.snapshotsState.collectLatest { homeState ->
                        when (homeState) {
                            is HomeState.Loading -> {
                                progressBar.isVisible = true
                            }

                            is HomeState.Empty -> {
                                progressBar.isVisible = false
                                emptyStateLayout.isVisible = true
                                homePagingAdapter.submitData(PagingData.empty())
                            }

                            is HomeState.Success -> {
                                progressBar.isVisible = false
                                emptyStateLayout.isVisible = false
                                homePagingAdapter.submitData(homeState.pagingData)
                                homePagingAdapter.refresh()
                            }

                            is HomeState.Failure -> {
                                progressBar.isVisible = false
                                showPopUpMessage(
                                    homeState.errorMessage
                                        ?: R.string.error_unknown,
                                    isError = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  OnClickListener
     * */
    override fun onSetLikeSnapshot(snapshot: Snapshot, isLike: Boolean) {
        homeViewModel.setLikeSnapshot(snapshot, isLike)
    }

    override fun onDeleteSnapshot(snapshot: Snapshot) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.dialog_delete_title)
                .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                    homeViewModel.deleteSnapshot(snapshot)
                }
                .setNegativeButton(R.string.dialog_delete_cancel, null)
                .show()
        }
    }

    override fun onShareSnapshot(snapshot: Snapshot) {
        shareSnapshot(snapshot)
    }

    /**
     *   FragmentAux
     * */
    override fun onRefresh() {
        if (homePagingAdapter.itemCount > 0) homeViewModel.fetchSnapshots()
        binding.homeRecyclerView.smoothScrollToPosition(0)
    }

    private fun shareSnapshot(snapshot: Snapshot) {
        val shareText = getString(R.string.home_description_button_share, snapshot.title)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val shareIntent =
            Intent.createChooser(intent, getString(R.string.home_description_title_share))
        startActivity(shareIntent)
    }

}