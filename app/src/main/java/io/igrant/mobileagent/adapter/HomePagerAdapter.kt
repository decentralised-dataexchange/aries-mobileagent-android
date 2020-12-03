package io.igrant.mobileagent.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.igrant.mobileagent.fragment.ConnectionListFragment
import io.igrant.mobileagent.fragment.PreferenceFragment
import io.igrant.mobileagent.fragment.WalletFragment


class HomePagerAdapter(fragmentManager: FragmentManager?) :
    FragmentPagerAdapter(fragmentManager!!) {
    // Returns total number of pages
    override fun getCount(): Int {
        return NUM_ITEMS
    }

    // Returns the fragment to display for that page
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> WalletFragment.newInstance()
            1 -> PreferenceFragment.newInstance()

            else -> WalletFragment()
        }
    }

    // Returns the page title for the top indicator
    override fun getPageTitle(position: Int): CharSequence? {
        return "Page $position"
    }

    companion object {
        private const val NUM_ITEMS = 3
    }
}