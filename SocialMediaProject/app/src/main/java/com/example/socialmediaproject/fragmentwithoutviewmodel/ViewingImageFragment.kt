package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ViewingImageFragment : Fragment() {
    private lateinit var imageview: ImageView
    private var imageurl: String?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_viewing_image, container, false)
        imageview=view.findViewById(R.id.imageViewFullScreen)
        imageurl=arguments?.getString("IMAGE_URL")
        imageurl?.let {
            Glide.with(this).load(it).into(imageview)
        }
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return view
    }

    companion object {
        fun newInstance(imgurl: String): ViewingImageFragment {
            val fragment= ViewingImageFragment()
            val args=Bundle()
            args.putString("IMAGE_URL", imgurl)
            fragment.arguments=args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }
}