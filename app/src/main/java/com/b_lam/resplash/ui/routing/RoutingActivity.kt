package com.b_lam.resplash.ui.routing

import android.content.Intent
import android.os.Bundle
import com.b_lam.resplash.R
import com.b_lam.resplash.ui.base.BaseActivity
import com.b_lam.resplash.ui.base.BaseViewModel
import com.b_lam.resplash.ui.collection.detail.CollectionDetailActivity
import com.b_lam.resplash.ui.main.MainActivity
import com.b_lam.resplash.ui.photo.detail.PhotoDetailActivity
import com.b_lam.resplash.ui.user.UserActivity
import com.b_lam.resplash.util.toast

class RoutingActivity : BaseActivity() {

    override val viewModel: BaseViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        routeIntent(intent)
    }

    private fun routeIntent(intent: Intent?) {
        intent?.data?.let {
            val firstPathSegment = it.pathSegments.first()
            when {
                firstPathSegment == "photos" && it.pathSegments.size > 1 -> {
                    Intent(this, PhotoDetailActivity::class.java).apply {
                        putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, it.pathSegments[1])
                        startActivity(this)
                    }
                }
                firstPathSegment == "collections" && it.pathSegments.size > 1 -> {
                    Intent(this, CollectionDetailActivity::class.java).apply {
                        putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, it.pathSegments[1])
                        startActivity(this)
                    }
                }
                firstPathSegment.startsWith("@") -> {
                    Intent(this, UserActivity::class.java).apply {
                        putExtra(UserActivity.EXTRA_USERNAME, firstPathSegment.removePrefix("@"))
                        startActivity(this)
                    }
                }
                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        } ?: run {
            toast(R.string.oops)
        }

        finish()
    }
}