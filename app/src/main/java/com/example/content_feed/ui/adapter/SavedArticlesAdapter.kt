package com.example.content_feed.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.content_feed.R
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.databinding.ArticleItemBinding

class SavedArticlesAdapter(
    private val onUnsaveClick: (ArticleItem) -> Unit,
    private val onItemClick: (ArticleItem) -> Unit = {}
) : ListAdapter<ArticleItem, SavedArticlesAdapter.SavedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val binding = ArticleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedViewHolder(binding, onUnsaveClick, onItemClick)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SavedViewHolder(
        private val binding: ArticleItemBinding,
        private val onUnsaveClick: (ArticleItem) -> Unit,
        private val onItemClick: (ArticleItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArticleItem) {
            binding.tvTitle.text = item.title
            binding.tvPublishedDate.text = item.publishedDate
            binding.btnSave.isSelected = true

            val imageUrl = item.imageUrl.trim().let { url ->
                if (url.startsWith("http://")) url.replaceFirst("http://", "https://") else url
            }

            Glide.with(binding.ivArticle)
                .load(imageUrl)
                .placeholder(R.drawable.sl_nav_item_bg)
                .error(R.drawable.ic_cloud_off)
                .override(200, 200)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .dontAnimate()
                .into(binding.ivArticle)

            binding.btnSave.setOnClickListener {
                onUnsaveClick(item)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ArticleItem>() {
            override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
