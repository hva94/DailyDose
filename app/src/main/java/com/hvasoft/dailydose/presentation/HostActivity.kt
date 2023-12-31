package com.hvasoft.dailydose.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.hvasoft.dailydose.R
import com.hvasoft.dailydose.data.common.Constants
import com.hvasoft.dailydose.databinding.ActivityHostBinding
import com.hvasoft.dailydose.presentation.screens.add.AddFragment
import com.hvasoft.dailydose.presentation.screens.common.HomeFragmentListener
import com.hvasoft.dailydose.presentation.screens.common.HostActivityListener
import com.hvasoft.dailydose.presentation.screens.home.HomeFragment
import com.hvasoft.dailydose.presentation.screens.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HostActivity : AppCompatActivity(), HostActivityListener {

    private var _binding: ActivityHostBinding? = null
    private val binding get() = _binding!!

    private lateinit var activeFragment: Fragment
    private lateinit var authListener: FirebaseAuth.AuthStateListener

    private var firebaseAuth: FirebaseAuth? = null
    private var mFragmentManager: FragmentManager? = null

    private val authResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.main_auth_welcome, Toast.LENGTH_SHORT).show()
            } else {
                if (IdpResponse.fromResultIntent(it.data) == null) {
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuth()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        setupAuth()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { it ->
            if (it.currentUser == null) {
                authResult.launch(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setLogo(R.mipmap.ic_banner)
                        .setTheme(R.style.LoginTheme)
                        .setLockOrientation(true)
                        .setAvailableProviders(
                            listOf(
                                AuthUI.IdpConfig.EmailBuilder().build(),
                                AuthUI.IdpConfig.GoogleBuilder().build()
                            )
                        )
                        .build()
                )
                mFragmentManager = null
            } else {
                Constants.currentUser = it.currentUser!!

                val fragmentProfile =
                    mFragmentManager?.findFragmentByTag(ProfileFragment::class.java.name)
                fragmentProfile?.let {
                    (it as HomeFragmentListener).onRefresh()
                }

                if (mFragmentManager == null) {
                    mFragmentManager = supportFragmentManager
                    setupBottomNav(mFragmentManager!!)
                }
            }
        }
    }

    private fun setupBottomNav(fragmentManager: FragmentManager) {
        mFragmentManager?.let {
            for (fragment in it.fragments) {
                it.beginTransaction().remove(fragment!!).commit()
            }
        }

        val homeFragment = HomeFragment()
        val addFragment = AddFragment()
        val profileFragment = ProfileFragment()

        activeFragment = homeFragment

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, profileFragment, profileFragment::class.java.name)
            .hide(profileFragment).commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, addFragment, addFragment::class.java.name)
            .hide(addFragment).commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, homeFragment, HomeFragment::class.java.name).commit()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.action_home -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(homeFragment)
                        .commit()
                    activeFragment = homeFragment
                    true
                }
                R.id.action_add -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(addFragment)
                        .commit()
                    activeFragment = addFragment
                    true
                }
                R.id.action_profile -> {
                    fragmentManager.beginTransaction().hide(activeFragment).show(profileFragment)
                        .commit()
                    activeFragment = profileFragment
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.action_home -> (homeFragment as HomeFragmentListener).onRefresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth?.addAuthStateListener(authListener)
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth?.removeAuthStateListener(authListener)
    }

    /**
     *   HostActivityListener
     **/
    override fun showPopUpMessage(resId: Int, duration: Int) {
        Snackbar.make(binding.root, resId, duration)
            .setAnchorView(binding.bottomNav)
            .show()
    }

    override fun onSnapshotPosted() {
        val fragmentManager = supportFragmentManager
        val homeFragment = fragmentManager.findFragmentByTag(HomeFragment::class.java.name) as HomeFragment?
        if (homeFragment != null) {
            fragmentManager.beginTransaction().hide(activeFragment).show(homeFragment).commit()
            activeFragment = homeFragment
            binding.bottomNav.selectedItemId = R.id.action_home
        }
    }
}