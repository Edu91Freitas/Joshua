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

package me.xizzhu.android.joshua.reading

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.reading.chapter.ChapterListPresenter
import me.xizzhu.android.joshua.reading.chapter.ChapterListView
import me.xizzhu.android.joshua.reading.toolbar.ReadingToolbar
import me.xizzhu.android.joshua.reading.toolbar.ToolbarPresenter
import me.xizzhu.android.joshua.reading.verse.VersePresenter
import me.xizzhu.android.joshua.reading.verse.VerseViewPager
import me.xizzhu.android.joshua.utils.BaseActivity
import javax.inject.Inject

class ReadingActivity : BaseActivity() {
    @Inject
    lateinit var readingDrawerPresenter: ReadingDrawerPresenter

    @Inject
    lateinit var toolbarPresenter: ToolbarPresenter

    @Inject
    lateinit var chapterListPresenter: ChapterListPresenter

    @Inject
    lateinit var versePresenter: VersePresenter

    @Inject
    lateinit var searchButtonPresenter: SearchButtonPresenter

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: ReadingDrawerLayout
    private lateinit var toolbar: ReadingToolbar
    private lateinit var chapterListView: ChapterListView
    private lateinit var verseViewPager: VerseViewPager
    private lateinit var search: SearchFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reading)
        drawerLayout = findViewById(R.id.drawer_layout)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setPresenter(toolbarPresenter)

        chapterListView = findViewById(R.id.chapter_list_view)
        chapterListView.setPresenter(chapterListPresenter)

        verseViewPager = findViewById(R.id.verse_view_pager)
        verseViewPager.setPresenter(versePresenter)

        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(drawerToggle)

        search = findViewById(R.id.search)
        search.setPresenter(searchButtonPresenter)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onStart() {
        super.onStart()

        readingDrawerPresenter.attachView(drawerLayout)
        toolbarPresenter.attachView(toolbar)
        chapterListPresenter.attachView(chapterListView)
        versePresenter.attachView(verseViewPager)
        searchButtonPresenter.attachView(search)
    }

    override fun onStop() {
        readingDrawerPresenter.detachView()
        toolbarPresenter.detachView()
        chapterListPresenter.detachView()
        versePresenter.detachView()
        searchButtonPresenter.detachView()

        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }
}
