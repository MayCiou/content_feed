package com.example.content_feed.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.databinding.FragmentSavedBinding
import com.example.content_feed.ui.adapter.SavedArticlesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedViewModel by viewModels()
    private lateinit var savedAdapter: SavedArticlesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObserver()
    }

    private fun setupRecyclerView() {
        savedAdapter = SavedArticlesAdapter(
            onUnsaveClick = { article ->
                viewModel.unsaveArticle(article)
            },
            onItemClick = { article ->
                // Future offline WebView click
            }
        )

        binding.rvSavedContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = savedAdapter
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateSavedList(viewModel.savedArticles.value)
        }
    }

    private fun updateSavedList(list: List<ArticleItem>) {
        if (_binding == null) return
        if (list.isEmpty()) {
            binding.tvEmptySaved.visibility = View.VISIBLE
            binding.rvSavedContent.visibility = View.GONE
        } else {
            binding.tvEmptySaved.visibility = View.GONE
            binding.rvSavedContent.visibility = View.VISIBLE
            savedAdapter.submitList(list)
        }
    }

    private fun setupObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedArticles.collectLatest { list ->
                    updateSavedList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}