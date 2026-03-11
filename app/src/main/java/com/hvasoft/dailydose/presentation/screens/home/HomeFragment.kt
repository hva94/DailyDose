package com.hvasoft.dailydose.presentation.screens.home

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class HomeFragment : Fragment(), HomeFragmentListener, OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var homePagingAdapter: HomePagingAdapter
    private var shouldScrollToTopAfterRefresh = false

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

        homePagingAdapter.addLoadStateListener { loadStates ->
            val isEmpty = loadStates.refresh is LoadState.NotLoading &&
                    homePagingAdapter.itemCount == 0
            binding.emptyStateLayout.isVisible = isEmpty
            binding.homeRecyclerView.isVisible = !isEmpty
            binding.progressBar.isVisible = loadStates.refresh is LoadState.Loading

            if (shouldScrollToTopAfterRefresh &&
                loadStates.refresh is LoadState.NotLoading &&
                homePagingAdapter.itemCount > 0
            ) {
                shouldScrollToTopAfterRefresh = false
                binding.homeRecyclerView.post {
                    binding.homeRecyclerView.scrollToPosition(0)
                }
            }
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
                                homeRecyclerView.isVisible = false
                                homePagingAdapter.submitData(PagingData.empty())
                            }

                            is HomeState.Success -> {
                                homePagingAdapter.submitData(homeState.pagingData)
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
    override fun onSetLikeSnapshot(snapshot: Snapshot, isLiked: Boolean) {
        homeViewModel.setLikeSnapshot(snapshot, isLiked)
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
        shouldScrollToTopAfterRefresh = true
        homeViewModel.fetchSnapshots()
    }

    private fun shareSnapshot(snapshot: Snapshot) {
        val shareText = getString(R.string.home_description_button_share, snapshot.title)

        val imageUrl = snapshot.photoUrl
        if (imageUrl.isNullOrBlank()) {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    },
                    getString(R.string.home_description_title_share)
                )
            )
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val imageUri = withContext(Dispatchers.IO) {
                    createShareableImageUri(imageUrl)
                }

                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            clipData = ClipData.newUri(
                                requireContext().contentResolver,
                                "snapshot_image",
                                imageUri
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        getString(R.string.home_description_title_share)
                    )
                )
            } catch (e: Exception) {
                showPopUpMessage(R.string.home_share_image_error, isError = true)
            }
        }
    }

    private fun createShareableImageUri(imageUrl: String): Uri {
        val downloadedImage = Glide.with(this)
            .asFile()
            .load(imageUrl)
            .submit()
            .get()

        val shareDirectory = File(requireContext().cacheDir, "shared_images").apply {
            mkdirs()
        }
        val shareFile = File(shareDirectory, downloadedImage.nameWithoutExtension + ".jpg")
        downloadedImage.copyTo(shareFile, overwrite = true)

        return FileProvider.getUriForFile(
            requireContext(),
            "com.hvasoft.fileprovider",
            shareFile
        )
    }
}
