/*
 * Copyright (C) 2019 Xizhi Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xizzhu.android.joshua.reading.detail

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.ui.getBackgroundColor
import me.xizzhu.android.joshua.ui.getPrimaryTextColor
import me.xizzhu.android.joshua.ui.getSecondaryTextColor
import me.xizzhu.android.joshua.utils.BaseSettingsView

interface VerseDetailView : BaseSettingsView {
    fun onVerseDetailLoaded(verseDetail: VerseDetail)

    fun onVerseDetailLoadFailed(verseIndex: VerseIndex)

    fun onVerseTextCopied()

    fun onVerseTextClickFailed()

    fun show(page: Int)

    fun hide()
}

class VerseDetailViewLayout : FrameLayout, VerseDetailView {
    companion object {
        private val ON_COLOR_FILTER = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
        private val OFF_COLOR_FILTER = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private lateinit var presenter: VerseDetailPresenter

    private val adapter = VerseDetailPagerAdapter(context, object : VerseDetailPagerAdapter.Listener {
        override fun onNoteUpdated(note: String) {
            presenter.updateNote(note)
        }
    })
    private val divider: View
    private val header: LinearLayout
    private val tabLayout: TabLayout
    private val viewPager: ViewPager
    private val bookmark: ImageView

    init {
        View.inflate(context, R.layout.inner_verse_detail_view, this)
        divider = findViewById(R.id.divider)
        header = findViewById(R.id.header)
        viewPager = findViewById<ViewPager>(R.id.view_pager).apply { adapter = this@VerseDetailViewLayout.adapter }
        tabLayout = findViewById<TabLayout>(R.id.tab_layout).apply { setupWithViewPager(viewPager) }
        bookmark = findViewById<ImageView>(R.id.bookmark).apply {
            setOnClickListener { presenter.updateBookmark() }
        }

        setOnClickListener { presenter.hide() }

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                translationY = height.toFloat()
            }
        })
    }

    fun setPresenter(presenter: VerseDetailPresenter) {
        this.presenter = presenter
    }

    override fun onSettingsUpdated(settings: Settings) {
        adapter.setSettings(settings)

        header.setBackgroundColor(settings.getBackgroundColor())
        val resources = resources
        tabLayout.setTabTextColors(settings.getSecondaryTextColor(resources), settings.getPrimaryTextColor(resources))

        if (settings.nightModeOn) {
            divider.setBackgroundColor(0xFF222222.toInt())
            viewPager.setBackgroundColor(0xFF222222.toInt())
        } else {
            divider.setBackgroundColor(0xFFEEEEEE.toInt())
            viewPager.setBackgroundColor(0xFFEEEEEE.toInt())
        }
    }

    override fun onVerseDetailLoaded(verseDetail: VerseDetail) {
        adapter.setVerseDetail(verseDetail)
        bookmark.colorFilter = if (verseDetail.bookmarked) ON_COLOR_FILTER else OFF_COLOR_FILTER
    }

    override fun onVerseDetailLoadFailed(verseIndex: VerseIndex) {
        DialogHelper.showDialog(context, true, R.string.dialog_load_verse_detail_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.loadVerseDetail(verseIndex)
                })
    }

    override fun onVerseTextCopied() {
        Toast.makeText(context, R.string.toast_verses_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onVerseTextClickFailed() {
        Toast.makeText(context, R.string.toast_unknown_error, Toast.LENGTH_SHORT).show()
    }

    override fun show(page: Int) {
        animate().translationY(0.0F)
        viewPager.currentItem = page
    }

    override fun hide() {
        animate().translationY(height.toFloat())
    }
}
