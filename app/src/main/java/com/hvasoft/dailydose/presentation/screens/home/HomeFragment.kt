package com.hvasoft.dailydose.presentation.screens.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.databinding.FragmentHomeBinding
import com.hvasoft.dailydose.presentation.screens.home.adapter.HomeAdapter
import com.hvasoft.dailydose.presentation.screens.home.adapter.OnClickListener
import com.hvasoft.dailydose.presentation.screens.utils.FragmentAux
import com.hvasoft.dailydose.presentation.screens.utils.MainAux
import dagger.hilt.android.AndroidEntryPoint

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainAux = activity as MainAux
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mainAux = null
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(this)

        binding.homeRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.homeAdapter
        }
    }

    private fun setupViewModel() {
        with(binding) {
            homeViewModel.snapshotsState.observe(viewLifecycleOwner) { homeState ->
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
//                        homeAdapter.submitList(null)
                        homeAdapter.submitList(homeState.snapshots)
                    }

                    is HomeState.Failure -> {
                        progressBar.isVisible = false
                        showPopUpMessage(
                            homeState.error?.errorMessageRes
                                ?: R.string.error_unknown
                        )
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

    /**
     *   FragmentAux
     * */
    override fun refresh() {
        if (homeAdapter.itemCount > 0) homeViewModel.refreshSnapshots()
        binding.homeRecyclerView.smoothScrollToPosition(0)
    }

    private fun showPopUpMessage(messageRes: Int) {
        view?.let { rootView ->
            val snackBar = Snackbar.make(rootView, messageRes, Snackbar.LENGTH_LONG)
            val params = snackBar.view.layoutParams as ViewGroup.MarginLayoutParams
            val extraBottomMargin = resources.getDimensionPixelSize(R.dimen.common_padding_default)
            params.setMargins(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
//                binding.floatingButton.height + params.bottomMargin + extraBottomMargin
                params.bottomMargin + extraBottomMargin
            )
            snackBar.view.layoutParams = params
            snackBar.show()
        }
    }
}