package com.example.content_feed.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.content_feed.R
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.databinding.ArticleItemBinding

class ArticlePagingAdapter(
    private val onSaveClick: (ArticleItem) -> Unit = {}
) : PagingDataAdapter<ArticleItem, ArticlePagingAdapter.ArticleViewHolder>(ArticleDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ArticleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding, onSaveClick)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class ArticleViewHolder(
        private val binding: ArticleItemBinding,
        private val onSaveClick: (ArticleItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArticleItem) {
            binding.tvTitle.text = item.title
            binding.tvPublishedDate.text = item.publishedDate
            binding.btnSave.isSelected = item.isSaved

            val formattedUrl = item.imageUrl.replace("http://", "https://")
            val glideUrl = if (formattedUrl.isNotBlank()) {
                GlideUrl(
                    formattedUrl,
                    LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                        .build()
                )
            } else null

            Glide.with(binding.ivArticle.context)
                .load(glideUrl)
                .placeholder(R.drawable.sl_nav_item_bg)
                .error(R.drawable.ic_cloud_off)
                .centerCrop()
                .into(binding.ivArticle)

            binding.btnSave.setOnClickListener {
                onSaveClick(item)
            }
        }
    }

    companion object {
        private val ArticleDiffCallback = object : DiffUtil.ItemCallback<ArticleItem>() {
            override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
