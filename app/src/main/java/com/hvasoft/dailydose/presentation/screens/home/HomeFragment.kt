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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.network.model.Snapshot
import com.hvasoft.dailydose.databinding.FragmentHomeBinding
import com.hvasoft.dailydose.presentation.screens.home.adapter.HomeAdapter
import com.hvasoft.dailydose.presentation.screens.home.adapter.OnClickListener
import com.hvasoft.dailydose.presentation.screens.utils.FragmentAux
import com.hvasoft.dailydose.presentation.screens.utils.MainAux
import com.hvasoft.dailydose.presentation.screens.utils.showPopUpMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(), FragmentAux, OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeAdapter

    private var mainAux: MainAux? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainAux = activity as MainAux
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDetach() {
        super.onDetach()
        mainAux = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mainAux = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupViewModel()
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(this)

        binding.homeRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.homeAdapter
        }
    }

//    private fun setupViewModel() {
//        with(binding) {
//            homeViewModel.snapshotsState.observe(viewLifecycleOwner) { homeState ->
//                when (homeState) {
//                    is HomeState.Loading -> progressBar.isVisible = true
//
//                    is HomeState.Empty -> {
//                        homeAdapter.submitList(null)
//                        progressBar.isVisible = false
//                        emptyStateLayout.isVisible = true
//                    }
//
//                    is HomeState.Success -> {
//                        progressBar.isVisible = false
//                        emptyStateLayout.isVisible = false
//                        homeAdapter.submitList(homeState.snapshots)
//                    }
//
//                    is HomeState.Failure -> {
//                        progressBar.isVisible = false
//                        showPopUpMessage(
//                            homeState.error?.errorMessageRes
//                                ?: R.string.error_unknown
//                        )
//                    }
//                }
//            }
//        }
//    }

    private fun setupViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                with(binding) {
                    homeViewModel.snapshotsState.collectLatest { homeState ->
                        when (homeState) {
                            is HomeState.Loading -> progressBar.isVisible = true

                            is HomeState.Empty -> {
                                homeAdapter.submitList(null)
                                progressBar.isVisible = false
                                emptyStateLayout.isVisible = true
                            }

                            is HomeState.Success -> {
                                progressBar.isVisible = false
                                emptyStateLayout.isVisible = false
                                homeAdapter.submitList(homeState.snapshots)
                            }

                            is HomeState.Failure -> {
                                progressBar.isVisible = false
                                showPopUpMessage(
                                    homeState.errorMessage
                                        ?: R.string.error_unknown
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
    override fun refresh() {
        if (homeAdapter.itemCount > 0) {
//            Log.d("hva_test", "refresh: homeViewModel.getSnapshots was called")
            homeViewModel.getSnapshots()
        } //else
//            Log.d("hva_test", "refresh: ELSE .itemCount = ${homeAdapter.itemCount}")
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