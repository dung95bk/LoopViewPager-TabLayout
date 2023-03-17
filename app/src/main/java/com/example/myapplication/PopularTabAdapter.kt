package com.example.myapplication

import android.os.Bundle
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.lang.ref.WeakReference

class PopularTabAdapter(
    fragmentManager: FragmentManager,
    private val fakeSubMenu: List<SubMenu>
) :
    FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    val registeredFragments: SparseArray<WeakReference<FirstFragment>> =
        SparseArray<WeakReference<FirstFragment>>()

    override fun getPageTitle(position: Int): CharSequence = fakeSubMenu[position].title

    override fun getItem(position: Int): Fragment =
        if (registeredFragments[position]?.get() == null) {
            val args = Bundle()

            val fragment = FirstFragment(fakeSubMenu[position].title)
            fragment.arguments = args
            registeredFragments.put(position, WeakReference(fragment))
            fragment
        } else {
            registeredFragments[position].get()!!
        }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    override fun getCount(): Int = fakeSubMenu.size

    companion object {
        const val START_INDEX_FRAGMENT = 1
        const val NUMBER_FAKE_FRAGMENT = 2
    }
}
