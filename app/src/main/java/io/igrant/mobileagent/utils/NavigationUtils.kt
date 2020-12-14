package io.igrant.mobileagent.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.igrant.mobileagent.R
import io.igrant.mobileagent.fragment.ConnectionListFragment
import io.igrant.mobileagent.activty.ConnectionDetailActivity
import io.igrant.mobileagent.fragment.WalletFragment

object NavigationUtils {

    private fun showFragment(
        fragment: Fragment,
        fragmentManager: FragmentManager,
        addToBackStack: Boolean
    ) {
        val transaction =
            fragmentManager.beginTransaction()
        transaction.replace(R.id.myContainer, fragment)
        if (addToBackStack) {
            transaction.addToBackStack("")
        }
        transaction.commit()
    }

    fun showWalletFragment(fragmentManager: FragmentManager?,addToBackStack: Boolean) {
        val walletFragment: WalletFragment = WalletFragment.newInstance()
        showFragment(walletFragment, fragmentManager!!, false)
    }

    fun showConnectionListFragment(fragmentManager: FragmentManager?) {
        val connectionFragment: ConnectionListFragment = ConnectionListFragment.newInstance()
        showFragment(connectionFragment, fragmentManager!!, true)
    }

    fun popBack(fragmentManager: FragmentManager) {
        fragmentManager.popBackStack()
    }
}