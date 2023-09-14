package com.hvasoft.dailydose.presentation.homeScreen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.model.Snapshot
import com.hvasoft.dailydose.databinding.FragmentHomeBinding
import com.hvasoft.dailydose.presentation.homeScreen.adapter.HomeAdapter
import com.hvasoft.dailydose.presentation.homeScreen.adapter.OnClickListener
import com.hvasoft.dailydose.presentation.utils.FragmentAux
import com.hvasoft.dailydose.presentation.utils.MainAux

class HomeFragment : Fragment(), FragmentAux, OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
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

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        homeViewModel.getSnapshots().observe(viewLifecycleOwner) { dailydose ->
            binding.progressBar.visibility = View.GONE
            homeAdapter.submitList(dailydose)
        }
        homeViewModel.snackbarMsg.observe(viewLifecycleOwner) { message ->
            mainAux?.showMessage(message)
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
}