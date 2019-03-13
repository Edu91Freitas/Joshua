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

package me.xizzhu.android.joshua.reading.verse

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.utils.MVPView

interface VerseView : MVPView {
    fun onCurrentVerseIndexUpdated(currentVerseIndex: VerseIndex)

    fun onCurrentTranslationUpdated(currentTranslation: String)

    fun onChapterSelectionFailed(bookIndex: Int, chapterIndex: Int)

    fun onVersesLoaded(bookIndex: Int, chapterIndex: Int, verses: List<Verse>)

    fun onVersesLoadFailed(translationShortName: String, bookIndex: Int, chapterIndex: Int)

    fun onVerseSelected(verseIndex: VerseIndex)

    fun onVerseDeselected(verseIndex: VerseIndex)
}

class VerseViewPager : ViewPager, VerseView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val versePagerAdapterListener = object : VersePagerAdapter.Listener {
        override fun onChapterRequested(bookIndex: Int, chapterIndex: Int) {
            presenter.loadVerses(currentTranslation, bookIndex, chapterIndex)
        }

        override fun onVerseClicked(verseIndex: VerseIndex) {
            presenter.onVerseClicked(verseIndex)
        }

        override fun onVerseLongClicked(verseIndex: VerseIndex) {
            presenter.onVerseLongClicked(verseIndex)
        }
    }
    private val adapter = VersePagerAdapter(context, versePagerAdapterListener)
    private val onPageChangeListener = object : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            if (currentVerseIndex.toPagePosition() == position) {
                return
            }
            presenter.selectChapter(position.toBookIndex(), position.toChapterIndex())
        }
    }

    init {
        setAdapter(adapter)
        addOnPageChangeListener(onPageChangeListener)
    }

    private lateinit var presenter: VersePresenter

    private var currentTranslation = ""
    private var currentVerseIndex = VerseIndex.INVALID

    fun setPresenter(presenter: VersePresenter) {
        this.presenter = presenter
    }

    override fun onCurrentVerseIndexUpdated(currentVerseIndex: VerseIndex) {
        this.currentVerseIndex = currentVerseIndex
        refreshUi()
    }

    private fun refreshUi() {
        if (currentTranslation.isEmpty() || !currentVerseIndex.isValid()) {
            return
        }

        adapter.currentTranslation = currentTranslation
        adapter.notifyDataSetChanged()
        setCurrentItem(currentVerseIndex.toPagePosition(), false)
    }

    override fun onCurrentTranslationUpdated(currentTranslation: String) {
        this.currentTranslation = currentTranslation
        refreshUi()
    }

    override fun onChapterSelectionFailed(bookIndex: Int, chapterIndex: Int) {
        DialogHelper.showDialog(context, true, R.string.dialog_chapter_selection_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.selectChapter(bookIndex, chapterIndex)
                })
    }

    override fun onVersesLoaded(bookIndex: Int, chapterIndex: Int, verses: List<Verse>) {
        adapter.setVerses(bookIndex, chapterIndex, verses)
    }

    override fun onVersesLoadFailed(translationShortName: String, bookIndex: Int, chapterIndex: Int) {
        DialogHelper.showDialog(context, true, R.string.dialog_verse_load_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.loadVerses(translationShortName, bookIndex, chapterIndex)
                })
    }

    override fun onVerseSelected(verseIndex: VerseIndex) {
        adapter.selectVerse(verseIndex)
    }

    override fun onVerseDeselected(verseIndex: VerseIndex) {
        adapter.deselectVerse(verseIndex)
    }
}
