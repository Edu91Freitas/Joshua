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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.annotation.UiThread
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.reading.chapter.ChapterListPresenter
import me.xizzhu.android.joshua.reading.chapter.ChapterListView
import me.xizzhu.android.joshua.reading.detail.VerseDetailPagerAdapter
import me.xizzhu.android.joshua.reading.detail.VerseDetailPresenter
import me.xizzhu.android.joshua.reading.detail.VerseDetailViewLayout
import me.xizzhu.android.joshua.reading.toolbar.ReadingToolbar
import me.xizzhu.android.joshua.reading.toolbar.ToolbarPresenter
import me.xizzhu.android.joshua.reading.verse.VersePresenter
import me.xizzhu.android.joshua.reading.verse.VerseViewPager
import me.xizzhu.android.joshua.utils.activities.BaseSettingsActivity
import me.xizzhu.android.joshua.utils.activities.BaseSettingsInteractor
import me.xizzhu.android.joshua.utils.createChooserForSharing
import javax.inject.Inject

class ReadingActivity : BaseSettingsActivity() {
    companion object {
        private const val KEY_OPEN_NOTE = "me.xizzhu.android.joshua.KEY_OPEN_NOTE"

        fun bundleForOpenNote(): Bundle = Bundle().apply { putBoolean(KEY_OPEN_NOTE, true) }
    }

    @Inject
    lateinit var readingInteractor: ReadingInteractor

    @Inject
    lateinit var readingDrawerPresenter: ReadingDrawerPresenter

    @Inject
    lateinit var toolbarPresenter: ToolbarPresenter

    @Inject
    lateinit var chapterListPresenter: ChapterListPresenter

    @Inject
    lateinit var versePresenter: VersePresenter

    @Inject
    lateinit var verseDetailPresenter: VerseDetailPresenter

    @Inject
    lateinit var searchButtonPresenter: SearchButtonPresenter

    private val drawerToggle: ActionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0) }
    private val drawerLayout: ReadingDrawerLayout by bindView(R.id.drawer_layout)
    private val toolbar: ReadingToolbar by bindView(R.id.toolbar)
    private val chapterListView: ChapterListView by bindView(R.id.chapter_list_view)
    private val verseViewPager: VerseViewPager by bindView(R.id.verse_view_pager)
    private val verseDetailView: VerseDetailViewLayout by bindView(R.id.verse_detail_view)
    private val search: SearchFloatingActionButton by bindView(R.id.search)

    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reading)
        toolbar.setPresenter(toolbarPresenter)
        chapterListView.setPresenter(chapterListPresenter)
        verseViewPager.setPresenter(versePresenter)
        verseDetailView.setPresenter(verseDetailPresenter)
        search.setPresenter(searchButtonPresenter)
        drawerLayout.addDrawerListener(drawerToggle)

        openNoteIfNeeded()
    }

    private fun openNoteIfNeeded() {
        if (intent.getBooleanExtra(KEY_OPEN_NOTE, false)) {
            coroutineScope.launch(Dispatchers.Main) {
                readingInteractor.openVerseDetail(readingInteractor.observeCurrentVerseIndex().first(),
                        VerseDetailPagerAdapter.PAGE_NOTE)
            }
        }
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
        verseDetailPresenter.attachView(verseDetailView)
        searchButtonPresenter.attachView(search)
    }

    override fun onResume() {
        super.onResume()
        coroutineScope.launch(Dispatchers.Main) { readingInteractor.startTrackingReadingProgress() }
    }

    override fun onPause() {
        // uses GlobalScope to make sure this will be executed without being canceled
        // uses Dispatchers.Main.immediate to make sure this will be executed immediately
        GlobalScope.launch(Dispatchers.Main.immediate) { readingInteractor.stopTrackingReadingProgress() }
        super.onPause()
    }

    override fun onStop() {
        readingDrawerPresenter.detachView()
        toolbarPresenter.detachView()
        chapterListPresenter.detachView()
        versePresenter.detachView()
        verseDetailPresenter.detachView()
        searchButtonPresenter.detachView()

        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (!readingInteractor.closeVerseDetail() && !drawerLayout.hide()) {
            super.onBackPressed()
        }
    }

    override fun getBaseSettingsInteractor(): BaseSettingsInteractor = readingInteractor

    @UiThread
    fun copy(label: String, text: String) {
        // On older devices, this only works on the threads with loopers.
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun share(textToShare: String): Boolean {
        return createChooserForSharing(this, getString(R.string.text_share_with), textToShare)
                ?.let {
                    startActivity(it)
                    true
                } ?: false
    }

    fun startActionModeIfNeeded(@MenuRes menuRes: Int, onActionItemClicked: (Int) -> Boolean, onDestroyActionMode: () -> Unit) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.menuInflater.inflate(menuRes, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = onActionItemClicked(item.itemId)

                override fun onDestroyActionMode(mode: ActionMode) {
                    onDestroyActionMode()
                    actionMode = null
                }
            })
        }
    }

    fun isActionModeStarted(): Boolean = actionMode != null

    fun finishActionMode() {
        actionMode?.finish()
    }
}
