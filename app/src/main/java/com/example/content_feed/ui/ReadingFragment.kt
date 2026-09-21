package com.example.content_feed.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.content_feed.R
import com.example.content_feed.databinding.FragmentReadingBinding
import com.example.content_feed.ui.adapter.ArticleLoadStateAdapter
import com.example.content_feed.ui.adapter.ArticlePagingAdapter
import com.example.content_feed.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReadingFragment : Fragment() {

    @Inject
    lateinit var networkUtil: NetworkUtil

    @Inject
    lateinit var savedSharedEvents: SavedSharedEvents

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReadingViewModel by viewModels()
    private lateinit var articleAdapter: ArticlePagingAdapter

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            viewModel.fetchDataWithLocation()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupWeatherObserver()
        setupUnsavedSyncObserver()
        if (checkNetworkAndHandleOffline()) {
            setupArticlesObserver()
            checkAndRequestLocationPermission()
        }
    }

    private fun setupUnsavedSyncObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                savedSharedEvents.removedArticleIds.collect { removedId ->
                    updateItemSavedStatus(removedId, isSaved = false)
                }
            }
        }
    }

    private fun updateItemSavedStatus(articleId: Int, isSaved: Boolean) {
        val itemCount = articleAdapter.itemCount
        for (i in 0 until itemCount) {
            val item = articleAdapter.peek(i)
            if (item?.id == articleId) {
                val viewHolder = binding.rvReadingContent.findViewHolderForAdapterPosition(i) as? ArticlePagingAdapter.ArticleViewHolder
                viewHolder?.binding?.btnSave?.isSelected = isSaved
                break
            }
        }
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticlePagingAdapter { article ->
            viewModel.toggleSaveArticle(article)
        }

        binding.rvReadingContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            adapter = articleAdapter.withLoadStateFooter(
                footer = ArticleLoadStateAdapter()
            )
        }

        articleAdapter.addLoadStateListener { loadState ->
            val refreshState = loadState.refresh

            // Control initial articles loading shimmer
            if (refreshState is LoadState.Loading && articleAdapter.itemCount == 0) {
                showArticlesLoading(true)
            } else {
                showArticlesLoading(false)
            }

            val appendState = loadState.append

            when {
                appendState is LoadState.Error -> {
                    // Pagination Error: Show blank RecyclerView and alert
                    binding.rvReadingContent.visibility = View.INVISIBLE
                    val errorMsg = appendState.error.localizedMessage ?: getString(R.string.pagination_error)
                    showPaginationAlertDialog(
                        title = "Error",
                        message = errorMsg,
                        positiveButtonText = "OK"
                    )
                }
                appendState.endOfPaginationReached && articleAdapter.itemCount > 0 -> {
                    // No more data: Show blank RecyclerView and alert
                    binding.rvReadingContent.visibility = View.INVISIBLE
                    showPaginationAlertDialog(
                        title = "Notice",
                        message = getString(R.string.pagination_no_more),
                        positiveButtonText = "OK"
                    )
                }
            }
        }
    }

    private fun showArticlesLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.layoutArticlesLoading.visibility = View.VISIBLE
            val shimmerAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.weather_loading_shimmer)
            binding.viewArticlesLoadingGradient.startAnimation(shimmerAnimation)
        } else {
            binding.viewArticlesLoadingGradient.clearAnimation()
            binding.layoutArticlesLoading.visibility = View.GONE
        }
    }

    private fun showPaginationAlertDialog(
        title: String,
        message: String,
        positiveButtonText: String,
        onPositiveClick: (() -> Unit)? = null
    ) {
        if (!isAdded || activity?.isFinishing == true) return

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                onPositiveClick?.invoke()
            }
            .setCancelable(true)
            .show()
    }

    private var articlesJob: Job? = null

    private fun setupArticlesObserver() {
        if (articlesJob != null) return
        articlesJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.articlesPagingData.collectLatest { pagingData ->
                articleAdapter.submitData(pagingData)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            if (checkNetworkAndHandleOffline()) {
                setupArticlesObserver()
                checkAndRequestLocationPermission()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            if (checkNetworkAndHandleOffline()) {
                setupArticlesObserver()
                checkAndRequestLocationPermission()
            }
        }
    }

    private fun checkNetworkAndHandleOffline(): Boolean {
        val isConnected = networkUtil.isNetworkAvailable()
        return if (isConnected) {
            _binding?.layoutWeatherCard?.root?.visibility = View.VISIBLE
            _binding?.rvReadingContent?.visibility = View.VISIBLE
            true
        } else {
            showWeatherLoading(isLoading = false)
            showArticlesLoading(isLoading = false)
            _binding?.layoutWeatherCard?.root?.visibility = View.GONE
            _binding?.rvReadingContent?.visibility = View.GONE
            false
        }
    }

    private fun setupWeatherObserver() {
        val weatherBinding = binding.layoutWeatherCard

        viewModel.weatherUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherUiState.Loading -> {
                    weatherBinding.root.visibility = View.VISIBLE
                    showWeatherLoading(isLoading = true)
                }
                is WeatherUiState.Success -> {
                    showWeatherLoading(isLoading = false)
                    weatherBinding.root.visibility = View.VISIBLE
                    weatherBinding.groupWeatherContent.visibility = View.VISIBLE
                    weatherBinding.layoutStatusNotice.visibility = View.GONE
                    weatherBinding.tvCity.text = state.city
                    weatherBinding.tvTemperature.text = state.temperature
                    weatherBinding.tvWeatherInfo.text = state.weatherInfo
                }
                is WeatherUiState.PermissionDenied -> {
                    showWeatherLoading(isLoading = false)
                    weatherBinding.root.visibility = View.VISIBLE
                    weatherBinding.groupWeatherContent.visibility = View.GONE
                    weatherBinding.layoutStatusNotice.visibility = View.VISIBLE
                    weatherBinding.ivStatusIcon.setImageResource(R.drawable.ic_cloud_off)
                    weatherBinding.tvStatusTitle.setText(R.string.weather_permission_needed_title)
                    weatherBinding.tvStatusDescription.setText(R.string.weather_permission_needed_desc)
                }
                is WeatherUiState.LocationUnavailable -> {
                    showWeatherLoading(isLoading = false)
                    weatherBinding.root.visibility = View.VISIBLE
                    weatherBinding.groupWeatherContent.visibility = View.GONE
                    weatherBinding.layoutStatusNotice.visibility = View.VISIBLE
                    weatherBinding.ivStatusIcon.setImageResource(R.drawable.ic_cloud_off)
                    weatherBinding.tvStatusTitle.setText(R.string.weather_location_unavailable_title)
                    weatherBinding.tvStatusDescription.setText(R.string.weather_location_unavailable_desc)
                }
            }
        }
    }

    private fun showWeatherLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.cardWeatherLoading.visibility = View.VISIBLE
            val shimmerAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.weather_loading_shimmer)
            binding.viewWeatherLoadingGradient.startAnimation(shimmerAnimation)
        } else {
            binding.viewWeatherLoadingGradient.clearAnimation()
            binding.cardWeatherLoading.visibility = View.GONE
        }
    }

    private fun checkAndRequestLocationPermission() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.fetchDataWithLocation()
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        articlesJob?.cancel()
        articlesJob = null
        _binding = null
    }
}
