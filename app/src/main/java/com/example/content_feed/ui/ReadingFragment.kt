package com.example.content_feed.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.content_feed.R
import com.example.content_feed.databinding.FragmentReadingBinding
import com.example.content_feed.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReadingFragment : Fragment() {

    @Inject
    lateinit var networkUtil: NetworkUtil

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReadingViewModel by viewModels()

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

        setupWeatherObserver()
        if (checkNetworkAndHandleOffline()) {
            checkAndRequestLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            if (checkNetworkAndHandleOffline()) {
                checkAndRequestLocationPermission()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            if (checkNetworkAndHandleOffline()) {
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
        _binding = null
    }
}
