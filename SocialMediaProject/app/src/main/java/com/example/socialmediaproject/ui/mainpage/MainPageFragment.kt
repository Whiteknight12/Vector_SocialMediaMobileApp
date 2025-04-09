package com.example.socialmediaproject.ui.mainpage

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentMainPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainPageFragment : Fragment() {
    private val viewModel: MainPageViewModel by viewModels()
    private lateinit var binding: FragmentMainPageBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentMainPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wallUserId=arguments?.getString("wall_user_id") ?: ""
        binding.progressBar.visibility=View.VISIBLE
        db.collection("Users").document(wallUserId).get().addOnSuccessListener {
            result->if (result.exists()) {
                Glide.with(requireContext()).load(result.getString("avatarurl"))
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.profileAvatar)
                binding.profileUsername.text = result.getString("name")
                if (result.getString("bio")=="") binding.profileBio.visibility=View.GONE
                else binding.profileBio.text = result.getString("bio")
                Glide.with(requireContext()).load(result.getString("wallurl"))
                    .placeholder(R.color.white)
                    .error(R.color.white)
                    .into(binding.wallImage)
                val currentUserId=auth.currentUser?.uid ?: ""
                if (currentUserId==wallUserId) {
                    binding.buttonAddFriend.visibility=View.GONE
                }
                else {
                    db.collection("Users").document(currentUserId).get().addOnSuccessListener {
                            newresult->if (newresult.exists()) {
                            val friends=newresult.get("friends") as? List<String>
                            if (friends?.contains(wallUserId) == true) {
                                binding.buttonAddFriend.visibility = View.GONE
                                binding.buttonUnfriend.visibility = View.VISIBLE
                                binding.buttonChat.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                val friendlist=result.get("friends") as? List<String> ?: emptyList()
                binding.profileFollowersCount.text=friendlist.size.toString()
                db.collection("Posts").whereEqualTo("userid", wallUserId).get().addOnSuccessListener {
                    listitem->if (listitem!=null) {
                        binding.profilePostsCount.text=listitem.size().toString()
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}