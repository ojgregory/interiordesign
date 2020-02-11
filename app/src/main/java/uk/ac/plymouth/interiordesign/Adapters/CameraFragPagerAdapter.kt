package uk.ac.plymouth.interiordesign.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import uk.ac.plymouth.interiordesign.Fragments.CameraFragment

class CamFragmentPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment {
        TODO("Fragments will be added in future")
    }

    override fun getCount() = 2
}