package com.example.content_feed.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.content_feed.R
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.databinding.ArticleItemBinding

class ArticlePagingAdapter(
    private val onSaveClick: (ArticleItem) -> Unit = {},
    private val onItemClick: (ArticleItem) -> Unit = {}
) : PagingDataAdapter<ArticleItem, ArticlePagingAdapter.ArticleViewHolder>(ArticleDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ArticleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding, onSaveClick, onItemClick)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class ArticleViewHolder(
        val binding: ArticleItemBinding,
        private val onSaveClick: (ArticleItem) -> Unit,
        private val onItemClick: (ArticleItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArticleItem) {
            binding.tvTitle.text = item.title
            binding.tvPublishedDate.text = item.publishedDate
            binding.btnSave.isSelected = item.isSaved

            Log.d(
                "ArticleViewHolder",
                "id=${item.title}, imageUrl=${item.imageUrl}"
            )

            Glide.with(binding.ivArticle)
                .clear(binding.ivArticle)

            val imageUrl = item.imageUrl
                .trim()
                .let { url ->
                    if (url.startsWith("http://")) {
                        url.replaceFirst("http://", "https://")
                    } else {
                        url
                    }
                }

            if (imageUrl.isBlank()) {
                binding.ivArticle.setImageResource(R.drawable.ic_cloud_off)
            } else {
                Glide.with(binding.ivArticle)
                    .load(GlideUrl(
                        imageUrl,
                        LazyHeaders.Builder()
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (Android; Mobile)"
                            )
                            .build()
                    ))
                    .placeholder(R.drawable.sl_nav_item_bg)
                    .error(R.drawable.ic_cloud_off)
                    .override(200, 200)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .dontAnimate()
                    .into(binding.ivArticle)
            }

            binding.btnSave.setOnClickListener {
                binding.btnSave.isSelected = !binding.btnSave.isSelected
                onSaveClick(item.copy(isSaved = binding.btnSave.isSelected))
            }

            binding.root.setOnClickListener {
                onItemClick(item)
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
