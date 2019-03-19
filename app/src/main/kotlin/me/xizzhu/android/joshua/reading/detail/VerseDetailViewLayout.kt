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
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.utils.MVPView

interface VerseDetailView : MVPView {
    fun showVerse(verseDetail: VerseDetail)

    fun show()

    fun hide()
}

class VerseDetailViewLayout : FrameLayout, VerseDetailView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private lateinit var presenter: VerseDetailPresenter

    private val onClickListener = View.OnClickListener { presenter.hide() }

    private val adapter: VerseDetailPagerAdapter
    private val tabLayout: TabLayout
    private val viewPager: ViewPager

    init {
        View.inflate(context, R.layout.inner_verse_detail_view, this)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        adapter = VerseDetailPagerAdapter(context)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        setOnClickListener(onClickListener)

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

    override fun showVerse(verseDetail: VerseDetail) {
        adapter.setVerse(verseDetail)
    }

    override fun show() {
        animate().translationY(0.0F)
    }

    override fun hide() {
        animate().translationY(height.toFloat())
    }
}
