package com.example.socialmediaproject.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import eightbitlab.com.blurview.BlurView

class FeedAdapter(
    private val context: Context,
    private var postList: MutableList<PostViewModel>,
    private val listener: OnPostInteractionListener
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    private val db:FirebaseFirestore=FirebaseFirestore.getInstance()
    private val expandedPositions = mutableSetOf<Int>()


    interface OnPostInteractionListener {
        fun onLikeClicked(position: Int)
        fun onCommentClicked(position: Int)
        fun onShareClicked(position: Int)
        fun onUserClicked(position: Int)
        fun onMoreOptionsClicked(position: Int, anchorView: View)
        fun onImageClicked(postPosition: Int, imagePosition: Int)
        fun onLikeCountClicked(postPosition: Int)
        fun onExpandClick(postPosition: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.feed_item_layout, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        db.collection("comments")
            .whereEqualTo("postId", post.id)
            .get()
            .addOnSuccessListener { snapshot ->
                val commentCount = snapshot.size()
                holder.textViewCommentCount.text = "$commentCount"
            }
            .addOnFailureListener {
                holder.textViewCommentCount.text = "0"
            }
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
            setupImagesRecyclerView(holder.recyclerViewImages, post.imageUrls, position, holder.imageIndicator)
        } else {
            holder.recyclerViewImages.visibility = View.GONE
        }
        holder.imageViewLike.setImageResource(
            if (post.isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
        )
        val isExpanded = expandedPositions.contains(position)
        holder.textViewPostContent.apply {
            text = post.content
            maxLines = if (isExpanded) Int.MAX_VALUE else 3
            ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
        }
        holder.textViewReadMore.visibility = View.GONE
        holder.textViewPostContent.post {
            val layout = holder.textViewPostContent.layout
            if (layout != null) {
                val isTruncated = layout.getEllipsisCount(2) > 0
                holder.textViewReadMore.apply {
                    visibility = if (isExpanded || isTruncated) View.VISIBLE else View.GONE
                    text = "Xem thêm"
                }
            }
        }
        if (post.privacy=="Công khai") holder.privacyIcon.setImageResource(R.drawable.icon_global)
        else if (post.privacy=="Bạn bè") holder.privacyIcon.setImageResource(R.drawable.iconfriends)
        else holder.privacyIcon.setImageResource(R.drawable.icon_private)
        setupClickListeners(holder, position)
    }

    private fun setupImagesRecyclerView(recyclerView: RecyclerView,
                                        imageUrls: List<String>,
                                        postPosition: Int,
                                        indicator: TabLayout) {
        val imageAdapter = ImagePostAdapter(imageUrls) { imagePosition ->
            listener.onImageClicked(postPosition, imagePosition)
        }
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.setHasFixedSize(true)
        recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
        recyclerView.adapter = imageAdapter
        if (imageUrls.size > 1) {
            indicator.visibility=View.VISIBLE
            indicator.removeAllTabs()
            repeat(imageUrls.size) {
                val tab = indicator.newTab()
                tab.setCustomView(R.layout.dot_indicator_tab)
                indicator.addTab(tab)
            }
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
                    for (i in 0 until indicator.tabCount) {
                        val view = indicator.getTabAt(i)?.customView
                        view?.alpha = if (i == firstVisible) 1.0f else 0.4f
                    }
                }
            })
        }
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

        holder.textViewLikeCount.setOnClickListener {
            listener.onLikeCountClicked(position)
        }

        holder.textViewReadMore.setOnClickListener {
            listener.onExpandClick(position)
        }
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newPosts: List<PostViewModel>) {
        this.postList.clear()
        this.postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun addPosts(newPosts: List<PostViewModel>) {
        val positionStart = this.postList.size
        this.postList.addAll(newPosts)
        notifyItemRangeInserted(positionStart, newPosts.size)
    }

    fun removeAt(position: Int) {
        postList.removeAt(position)
        notifyItemRemoved(position)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewUserAvatar: ShapeableImageView = itemView.findViewById(R.id.imageViewUserAvatar)
        val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val textViewPostContent: TextView = itemView.findViewById(R.id.textViewPostContent)
        val textViewReadMore: TextView = itemView.findViewById(R.id.textViewReadMore)
        val recyclerViewImages: RecyclerView = itemView.findViewById(R.id.recyclerViewImages)
        val imageViewLike: ImageView = itemView.findViewById(R.id.imageViewLike)
        val textViewLikeCount: TextView = itemView.findViewById(R.id.textViewLikeCount)
        val textViewCommentCount: TextView = itemView.findViewById(R.id.textViewCommentCount)
        val buttonMore: ImageButton = itemView.findViewById(R.id.buttonMore)
        val layoutLike: LinearLayout = itemView.findViewById(R.id.layoutLike)
        val layoutComment: LinearLayout = itemView.findViewById(R.id.layoutComment)
        val layoutShare: LinearLayout = itemView.findViewById(R.id.layoutShare)
        val privacyIcon: ImageView = itemView.findViewById(R.id.privacy_icon)
        val blurViewHeader: BlurView = itemView.findViewById(R.id.blurViewHeader)
        val imageIndicator: TabLayout = itemView.findViewById(R.id.imageIndicator)

        init {
            setupBlurView()
        }
        
        private fun setupBlurView() {
            val overlayDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(0x1A000000)
            }
            
            try {
                blurViewHeader.setupWith(itemView.rootView as ViewGroup)
                    .setBlurRadius(20f)
                
                blurViewHeader.background = overlayDrawable
                blurViewHeader.clipToOutline = true
                blurViewHeader.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            } catch (e: Exception) {
                blurViewHeader.background = overlayDrawable
            }
        }
    }
}