package com.example.socialmediaproject.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.adapter.FriendRecommendAdapter
import com.example.socialmediaproject.databinding.FragmentSearchBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.socialmediaproject.adapter.ReceivedFriendRequestAdapter
import com.example.socialmediaproject.adapter.SentRequestAdapter

class SearchFragment : Fragment() {
    private val viewModel: SearchViewModel by activityViewModels()
    private lateinit var binding: FragmentSearchBinding
    private lateinit var friendRecommendAdapter: FriendRecommendAdapter
    private lateinit var sentRequestAdapter: SentRequestAdapter
    private lateinit var receivedFriendRequestAdapter: ReceivedFriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        friendRecommendAdapter= FriendRecommendAdapter(
            onAddFriendClick = {
                friend->viewModel.sendFriendRequest(friend) },
            onResendClick = {
                receiverId, callback -> viewModel.resendFriendRequest(receiverId, callback)
            })
        sentRequestAdapter= SentRequestAdapter(
            onResendClick = {
                receiverId, callback -> viewModel.resendFriendRequest(receiverId, callback)
            }
        )
        receivedFriendRequestAdapter= ReceivedFriendRequestAdapter(
            onRejectClick = {
                receiverId, callback -> viewModel.rejectFriendRequest(receiverId, callback)
            },
            onAcceptClick = {
                receiverId, callback -> viewModel.acceptFriendRequest(receiverId, callback)
            }
        )
        binding.recyclerViewFriendRecommend.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendRecommendAdapter
        }
        binding.recyclerViewSentRequest.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sentRequestAdapter
        }
        binding.recyclerViewInvitation.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = receivedFriendRequestAdapter
        }
        viewModel.fetchRecommendations()
        viewModel.fetchSentRequests()
        viewModel.fetchReceivedFriendRequests()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchRecommendations()
            viewModel.fetchSentRequests()
            viewModel.fetchReceivedFriendRequests()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recommendations.collect { recommendations ->
                        friendRecommendAdapter.submitList(recommendations)
                        binding.textViewEmpty.visibility = if (recommendations.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
                        binding.recyclerViewFriendRecommend.visibility = if (recommendations.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.sentRequests.collect { sentRequests ->
                        sentRequestAdapter.submitList(sentRequests)
                        binding.recyclerViewSentRequest.visibility = if (sentRequests.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.receivedRequests.collect { receivedRequests->
                        receivedFriendRequestAdapter.submitList(receivedRequests)
                        binding.recyclerViewInvitation.visibility = if (receivedRequests.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        }
                        if (!isLoading && binding.swipeRefreshLayout.isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                        if(isLoading) {
                            binding.textViewEmpty.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchRecommendations()
            viewModel.fetchSentRequests()
        }
    }
}