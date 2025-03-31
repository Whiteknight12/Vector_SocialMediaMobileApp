package com.example.socialmediaproject.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.R
import com.google.android.material.imageview.ShapeableImageView

class FeedAdapter(
    private val context: Context,
    private var postList: List<PostViewModel>,
    private val listener: OnPostInteractionListener
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    interface OnPostInteractionListener {
        fun onLikeClicked(position: Int)
        fun onCommentClicked(position: Int)
        fun onShareClicked(position: Int)
        fun onUserClicked(position: Int)
        fun onMoreOptionsClicked(position: Int, anchorView: View)
        fun onImageClicked(postPosition: Int, imagePosition: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.feed_item_layout, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.textViewUsername.text = post.userName
        holder.textViewTimestamp.text = post.getTimeAgo()

        holder.textViewPostContent.text = post.content

        holder.textViewLikeCount.text = post.likeCount.toString()
        holder.textViewCommentCount.text = post.commentCount.toString()

        if (post.userAvatarUrl.isNotEmpty()) {
            Glide.with(context)
                .load(post.userAvatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.imageViewUserAvatar)
        } else {
            holder.imageViewUserAvatar.setImageResource(R.drawable.avataricon)
        }

        if (post.imageUrls.isNotEmpty()) {
            holder.recyclerViewImages.visibility = View.VISIBLE
            setupImagesRecyclerView(holder.recyclerViewImages, post.imageUrls, position)
        } else {
            holder.recyclerViewImages.visibility = View.GONE
        }

        holder.imageViewLike.setImageResource(
            if (post.isLiked) R.drawable.heartediconbutton else R.drawable.heartbutton
        )

        setupClickListeners(holder, position)
    }

    private fun setupImagesRecyclerView(recyclerView: RecyclerView, imageUrls: List<String>, postPosition: Int) {
        val imageAdapter = ImagePostAdapter(imageUrls) { imagePosition ->
            listener.onImageClicked(postPosition, imagePosition)
        }
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.setHasFixedSize(true)
        recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
        recyclerView.adapter = imageAdapter
    }

    private fun setupClickListeners(holder: PostViewHolder, position: Int) {
        holder.layoutLike.setOnClickListener {
            listener.onLikeClicked(position)
        }

        holder.layoutComment.setOnClickListener {
            listener.onCommentClicked(position)
        }

        holder.layoutShare.setOnClickListener {
            listener.onShareClicked(position)
        }

        holder.imageViewUserAvatar.setOnClickListener {
            listener.onUserClicked(position)
        }

        holder.textViewUsername.setOnClickListener {
            listener.onUserClicked(position)
        }

        holder.buttonMore.setOnClickListener {
            listener.onMoreOptionsClicked(position, it)
        }
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newPosts: List<PostViewModel>) {
        this.postList = newPosts
        notifyDataSetChanged()
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewUserAvatar: ShapeableImageView = itemView.findViewById(R.id.imageViewUserAvatar)
        val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val textViewPostContent: TextView = itemView.findViewById(R.id.textViewPostContent)
        val recyclerViewImages: RecyclerView = itemView.findViewById(R.id.recyclerViewImages)
        val imageViewLike: ImageView = itemView.findViewById(R.id.imageViewLike)
        val textViewLikeCount: TextView = itemView.findViewById(R.id.textViewLikeCount)
        val textViewCommentCount: TextView = itemView.findViewById(R.id.textViewCommentCount)
        val textViewShareCount: TextView = itemView.findViewById(R.id.textViewShareCount)
        val buttonMore: ImageButton = itemView.findViewById(R.id.buttonMore)
        val layoutLike: LinearLayout = itemView.findViewById(R.id.layoutLike)
        val layoutComment: LinearLayout = itemView.findViewById(R.id.layoutComment)
        val layoutShare: LinearLayout = itemView.findViewById(R.id.layoutShare)
    }
}