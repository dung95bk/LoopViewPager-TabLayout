package com.example.myapplication

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val subMenus = ArrayList<SubMenu>()
        arrayOf("1000", "2000", "3000", "4000", "5000").forEach {
            subMenus.add(SubMenu(it.toString()))
        }
        val fakeSubMenus = ArrayList<SubMenu>().apply {
            add(subMenus.last())
            addAll(subMenus)
            add(subMenus.first())
        }

        val popularTabAdapter = PopularTabAdapter(fragmentManager = supportFragmentManager, subMenus)
        binding.pager.adapter = popularTabAdapter
        binding.tabLayout.setItemsTitle(subMenus.map { it.title })
        binding.pager.offscreenPageLimit = subMenus.size
        binding.pager.currentItem = 1
        binding.tabLayout.setInfinityLoop(true)
        binding.tabLayout.setUpWithViewPager(binding.pager)


    }


}