package com.example.socialmediaproject.adapter

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    var comments: MutableList<Comment>,
    private val currentUserId: String,
    private val onReplyClicked: (Comment) -> Unit,
    private val onLikeClicked: (Comment) -> Unit,
    private val onReplyLikeClicked: (Comment) -> Unit,
    private val highlightCommentId: String? = null,
    private val onCommentClicked: (String) -> Unit,
    private val expandedCommentIds: MutableSet<String>,
    private val onDeleteCommentClicked: (Comment) -> Unit,
    private val onEditCommentClicked: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar = itemView.findViewById<ShapeableImageView>(R.id.ivUserAvatar)
        val username = itemView.findViewById<TextView>(R.id.tvUsername)
        val time = itemView.findViewById<TextView>(R.id.tvCommentTime)
        val content = itemView.findViewById<TextView>(R.id.tvCommentContent)
        val likeCount = itemView.findViewById<TextView>(R.id.tvLikeCount)
        val btnLike = itemView.findViewById<TextView>(R.id.btnLike)
        val btnReply = itemView.findViewById<TextView>(R.id.btnReply)
        val rvReplies = itemView.findViewById<RecyclerView>(R.id.rvReplies)
        val cardComment = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardComment)
        val deleteComment=itemView.findViewById<ImageView>(R.id.deleteComment)
        val editComment=itemView.findViewById<ImageView>(R.id.editComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int = comments.size

    fun updateFullComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    private val auth=FirebaseAuth.getInstance()
    private val db=FirebaseFirestore.getInstance()

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        if (comment.id == highlightCommentId) {
            holder.cardComment.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.highlight_color))
            holder.cardComment.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.pulse))
        } else {
            holder.cardComment.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.normal_comment_background))
        }
        holder.username.text = comment.username
        holder.content.text = comment.content
        holder.likeCount.text = comment.likes.size.toString()
        holder.time.text = getTimeAgo(comment.timestamp)
        if (comment.avatarurl.isNotEmpty()) {
            Glide.with(holder.avatar.context)
                .load(comment.avatarurl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.avatar)
        }
        holder.avatar.setOnClickListener {
            onCommentClicked(comment.userId)
        }
        val isLiked = comment.likes.contains(currentUserId)
        val iconRes = if (isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
        setDrawableStartWithSize(holder.btnLike, iconRes, 24)
        holder.btnLike.setOnClickListener { onLikeClicked(comment) }
        holder.btnReply.setOnClickListener { onReplyClicked(comment) }
        val repliesToShow = if (expandedCommentIds.contains(comment.id)) {
            comment.replies
        } else {
            if (comment.replies.size > 2) comment.replies.take(2) else comment.replies
        }
        val replyAdapter = ReplyAdapter(
            replies = repliesToShow,
            currentUserId = currentUserId,
            onReplyClicked = onReplyClicked,
            onLikeClicked = onReplyLikeClicked,
            onCommentClicked={userId->
                val bundle=Bundle()
                bundle.putString("wall_user_id", userId)
                findNavController(holder.itemView).navigate(R.id.navigation_mainpage, bundle)
            },
            highlightReplyId = highlightCommentId,
            onDeleteReplyClicked = onDeleteCommentClicked,
            onEditReplyClicked = onEditCommentClicked
        )
        holder.rvReplies.adapter = replyAdapter
        holder.rvReplies.layoutManager = LinearLayoutManager(holder.itemView.context)
        if (comment.replies.size > 2 && !expandedCommentIds.contains(comment.id)) {
            holder.itemView.findViewById<TextView>(R.id.tvShowMoreReplies).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    expandedCommentIds.add(comment.id)
                    notifyItemChanged(position)
                }
            }
        } else {
            holder.itemView.findViewById<TextView>(R.id.tvShowMoreReplies).visibility = View.GONE
        }
        db.collection("Users").document(auth.currentUser?.uid?:"").get().addOnSuccessListener {
            result->if (result.exists()) {
                if (result.getString("role") == "admin") {
                    holder.deleteComment.visibility = View.VISIBLE
                    holder.editComment.visibility = View.GONE
                }
            }
        }
        if (comment.userId==auth.currentUser?.uid) {
            holder.deleteComment.visibility=View.VISIBLE
            holder.editComment.visibility=View.VISIBLE
        }
        holder.deleteComment.setOnClickListener {
            onDeleteCommentClicked(comment)
        }
        holder.editComment.setOnClickListener {
            onEditCommentClicked(comment)
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 7 -> "$days ngày trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun setDrawableStartWithSize(textView: TextView, drawableResId: Int, sizeDp: Int) {
        val context = textView.context
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), context.resources.displayMetrics
        ).toInt()

        val drawable = ContextCompat.getDrawable(context, drawableResId)
        drawable?.setBounds(0, 0, sizePx, sizePx)
        textView.setCompoundDrawables(drawable, null, null, null)
    }
}