package com.example.snapshots


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.snapshots.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), MainAux {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mActiveFragment: Fragment
    private var mFragmentManager: FragmentManager? = null

    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private var mFirebaseAuth: FirebaseAuth? = null

    private val authResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
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
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupAuth()
    }

    private fun setupAuth() {
        mFirebaseAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { it ->
            if (it.currentUser == null){
                authResult.launch(
                AuthUI.getInstance().createSignInIntentBuilder()
                    .setIsSmartLockEnabled(false)
                    .setAvailableProviders(
                        listOf(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build())
                    )
                    .build()
                )
            } else {
                SnapshotsApplication.currentUser = it.currentUser!!

                val fragmentProfile = mFragmentManager?.findFragmentByTag(ProfileFragment::class.java.name)
                fragmentProfile?.let {
                    (it as FragmentAux).refresh()
                }

                if (mFragmentManager == null) {
                    mFragmentManager = supportFragmentManager
                    setupBottomNav(mFragmentManager!!)
                }
            }
        }
    }

    private fun setupBottomNav(fragmentManager: FragmentManager){
        mFragmentManager?.let {
            for (fragment in it.fragments) {
                it.beginTransaction().remove(fragment!!).commit()
            }
        }
        val homeFragment = HomeFragment()
        val addFragment = AddFragment()
        val profileFragment = ProfileFragment()

        mActiveFragment = homeFragment

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, profileFragment, profileFragment::class.java.name)
            .hide(profileFragment).commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, addFragment, addFragment::class.java.name)
            .hide(addFragment).commit()

        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, homeFragment, HomeFragment::class.java.name).commit()

        mBinding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.action_home -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(homeFragment).commit()
                    mActiveFragment = homeFragment
                    true
                }
                R.id.action_add -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(addFragment).commit()
                    mActiveFragment = addFragment
                    true
                }
                R.id.action_profile -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(profileFragment).commit()
                    mActiveFragment = profileFragment
                    true
                }
                else -> false
            }
        }

        mBinding.bottomNav.setOnItemReselectedListener {
            when(it.itemId){
                R.id.action_home -> (homeFragment as FragmentAux).refresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth?.addAuthStateListener(mAuthListener)
    }

    override fun onPause() {
        super.onPause()
        mFirebaseAuth?.removeAuthStateListener(mAuthListener)
    }

    /*
*   MainAux
* */
    override fun showMessage(resId: Int, duration: Int) {
        Snackbar.make(mBinding.root, resId, duration)
            .setAnchorView(mBinding.bottomNav)
            .show()
    }
}