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
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.utils.BaseSettingsView

interface VerseDetailView : BaseSettingsView {
    fun onVerseDetailLoaded(verseDetail: VerseDetail)

    fun onVerseDetailLoadFailed(verseIndex: VerseIndex)

    fun show()

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

    private val onClickListener = View.OnClickListener { presenter.hide() }

    private val adapter: VerseDetailPagerAdapter
    private val tabLayout: TabLayout
    private val viewPager: ViewPager

    private val bookmark: ImageView

    init {
        View.inflate(context, R.layout.inner_verse_detail_view, this)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        adapter = VerseDetailPagerAdapter(context, object : VerseDetailPagerAdapter.Listener {
            override fun onNoteUpdated(note: String) {
                presenter.updateNote(note)
            }
        })
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        setOnClickListener(onClickListener)

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                translationY = height.toFloat()
            }
        })

        bookmark = findViewById<ImageView>(R.id.bookmark).apply {
            setOnClickListener {
                presenter.updateBookmark()
            }
        }
    }

    fun setPresenter(presenter: VerseDetailPresenter) {
        this.presenter = presenter
    }

    override fun onSettingsUpdated(settings: Settings) {
        adapter.setSettings(settings)
    }

    override fun onVerseDetailLoaded(verseDetail: VerseDetail) {
        adapter.setVerse(verseDetail)
        bookmark.colorFilter = if (verseDetail.bookmarked) ON_COLOR_FILTER else OFF_COLOR_FILTER
    }

    override fun onVerseDetailLoadFailed(verseIndex: VerseIndex) {
        DialogHelper.showDialog(context, true, R.string.dialog_load_verse_detail_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.loadVerseDetail(verseIndex)
                })
    }

    override fun show() {
        animate().translationY(0.0F)
    }

    override fun hide() {
        animate().translationY(height.toFloat())
    }
}
