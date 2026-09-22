package com.example.content_feed.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.content_feed.R
import com.example.content_feed.databinding.ItemArticleFooterBinding

class ArticleLoadStateAdapter : LoadStateAdapter<ArticleLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = ItemArticleFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LoadStateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(
        private val binding: ItemArticleFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Loading -> {
                    binding.root.visibility = View.VISIBLE
                    binding.pbFooterLoading.visibility = View.VISIBLE
                    binding.tvFooterMessage.visibility = View.GONE
                }
                is LoadState.Error -> {
                    binding.root.visibility = View.VISIBLE
                    binding.pbFooterLoading.visibility = View.GONE
                    binding.tvFooterMessage.visibility = View.VISIBLE
                    binding.tvFooterMessage.text = loadState.error.localizedMessage
                        ?: binding.root.context.getString(R.string.pagination_error)
                }
                is LoadState.NotLoading -> {
                    if (loadState.endOfPaginationReached) {
                        binding.root.visibility = View.VISIBLE
                        binding.pbFooterLoading.visibility = View.GONE
                        binding.tvFooterMessage.visibility = View.VISIBLE
                        binding.tvFooterMessage.text =
                            binding.root.context.getString(R.string.pagination_no_more)
                    } else {
                        binding.root.visibility = View.GONE
                    }
                }
            }
        }
    }
}
