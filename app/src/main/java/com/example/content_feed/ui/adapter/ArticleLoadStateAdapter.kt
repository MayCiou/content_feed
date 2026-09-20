package com.example.content_feed.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.content_feed.R
import com.example.content_feed.databinding.ArticleItemBinding

class ArticleLoadStateAdapter : LoadStateAdapter<ArticleLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = ArticleItemBinding.inflate(
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
        private val binding: ArticleItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Loading -> {
                    binding.root.visibility = View.VISIBLE
                    binding.ivArticle.setImageResource(R.drawable.sl_nav_item_bg)
                    binding.tvTitle.text = binding.root.context.getString(R.string.app_name)
                    binding.tvPublishedDate.text = ""
                    binding.btnSave.visibility = View.INVISIBLE
                }
                is LoadState.Error -> {
                    binding.root.visibility = View.VISIBLE
                    binding.ivArticle.setImageResource(R.drawable.ic_cloud_off)
                    binding.tvTitle.text = loadState.error.localizedMessage
                        ?: binding.root.context.getString(R.string.pagination_error)
                    binding.tvPublishedDate.text = ""
                    binding.btnSave.visibility = View.INVISIBLE
                }
                is LoadState.NotLoading -> {
                    if (loadState.endOfPaginationReached) {
                        binding.root.visibility = View.VISIBLE
                        binding.ivArticle.setImageResource(R.drawable.ic_reading)
                        binding.tvTitle.text = binding.root.context.getString(R.string.pagination_no_more)
                        binding.tvPublishedDate.text = ""
                        binding.btnSave.visibility = View.INVISIBLE
                    } else {
                        binding.root.visibility = View.GONE
                    }
                }
            }
        }
    }
}
