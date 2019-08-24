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

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ActionMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.reading.ReadingInteractor
import me.xizzhu.android.joshua.reading.detail.VerseDetailPagerAdapter
import me.xizzhu.android.joshua.utils.activities.BaseSettingsPresenter
import me.xizzhu.android.joshua.utils.supervisedAsync
import me.xizzhu.android.logger.Log
import java.lang.StringBuilder
import kotlin.properties.Delegates

class VersePresenter(private val readingInteractor: ReadingInteractor)
    : BaseSettingsPresenter<VerseView>(readingInteractor) {
    @VisibleForTesting
    var selectedVerse: VerseIndex = VerseIndex.INVALID
    @VisibleForTesting
    val selectedVerses: HashSet<Verse> = HashSet()
    private var actionMode: ActionMode? = null
    @VisibleForTesting
    val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_verse_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_copy -> {
                    coroutineScope.launch(Dispatchers.Main) {
                        if (readingInteractor.copyToClipBoard(selectedVerses)) {
                            view?.onVersesCopied()
                        } else {
                            view?.onVersesCopyShareFailed()
                        }
                        mode.finish()
                    }
                    true
                }
                R.id.action_share -> {
                    coroutineScope.launch(Dispatchers.Main) {
                        if (!readingInteractor.share(selectedVerses)) {
                            view?.onVersesCopyShareFailed()
                        }
                        mode.finish()
                    }
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            for (verse in selectedVerses) {
                view?.onVerseDeselected(verse.verseIndex)
            }
            selectedVerses.clear()

            actionMode = null
        }
    }

    private var currentTranslation: String by Delegates.observable("") { _, _, new ->
        if (new.isNotEmpty()) {
            view?.onCurrentTranslationUpdated(new)
        }
    }
    private var currentVerseIndex: VerseIndex by Delegates.observable(VerseIndex.INVALID) { _, old, new ->
        if (!new.isValid()) {
            return@observable
        }
        actionMode?.let {
            if (old.bookIndex != new.bookIndex || old.chapterIndex != new.chapterIndex) {
                it.finish()
            }
        }
        view?.onCurrentVerseIndexUpdated(new)
    }
    private var parallelTranslations: List<String> by Delegates.observable(emptyList()) { _, _, new ->
        view?.onParallelTranslationsUpdated(new)
    }

    override fun onViewAttached() {
        super.onViewAttached()

        coroutineScope.launch(Dispatchers.Main) {
            readingInteractor.observeCurrentTranslation().collect { currentTranslation = it }
        }
        coroutineScope.launch(Dispatchers.Main) {
            readingInteractor.observeCurrentVerseIndex().collect { currentVerseIndex = it }
        }
        coroutineScope.launch(Dispatchers.Main) {
            readingInteractor.observeParallelTranslations().collect { parallelTranslations = it }
        }
        coroutineScope.launch(Dispatchers.Main) {
            readingInteractor.observeVerseDetailOpenState().collect {
                if (selectedVerse.isValid()) {
                    view?.onVerseDeselected(selectedVerse)
                    selectedVerse = VerseIndex.INVALID
                }
                if (it.first.isValid()) {
                    selectedVerse = it.first
                    view?.onVerseSelected(selectedVerse)
                }
            }
        }
        coroutineScope.launch(Dispatchers.Main) {
            readingInteractor.observeVerseUpdates().collect { (verseIndex, update) ->
                view?.onVerseUpdated(verseIndex, update)
            }
        }
    }

    fun selectChapter(bookIndex: Int, chapterIndex: Int) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                readingInteractor.saveCurrentVerseIndex(VerseIndex(bookIndex, chapterIndex, 0))
            } catch (e: Exception) {
                Log.e(tag, "Failed to update chapter selection", e)
                view?.onChapterSelectionFailed(bookIndex, chapterIndex)
            }
        }
    }

    fun saveCurrentVerseIndex(verseIndex: VerseIndex) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                readingInteractor.saveCurrentVerseIndex(verseIndex)
            } catch (e: Exception) {
                Log.e(tag, "Failed to save current verse", e)
            }
        }
    }

    fun loadVerses(bookIndex: Int, chapterIndex: Int) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                val versesAsync = supervisedAsync {
                    if (parallelTranslations.isEmpty()) {
                        readingInteractor.readVerses(currentTranslation, bookIndex, chapterIndex)
                    } else {
                        readingInteractor.readVerses(currentTranslation, parallelTranslations, bookIndex, chapterIndex)
                    }
                }
                val bookNameAsync = supervisedAsync { readingInteractor.readBookNames(currentTranslation)[bookIndex] }
                val highlightsAsync = supervisedAsync { readingInteractor.readHighlights(bookIndex, chapterIndex) }
                val items = if (readingInteractor.observeSettings().first().simpleReadingModeOn) {
                    toSimpleVerseItems(versesAsync.await(), bookNameAsync.await(), highlightsAsync.await())
                } else {
                    val bookmarksAsync = supervisedAsync { readingInteractor.readBookmarks(bookIndex, chapterIndex) }
                    val notesAsync = supervisedAsync { readingInteractor.readNotes(bookIndex, chapterIndex) }
                    toVerseItems(versesAsync.await(), bookNameAsync.await(), bookmarksAsync.await(), highlightsAsync.await(), notesAsync.await())
                }
                view?.onVersesLoaded(bookIndex, chapterIndex, items)
            } catch (e: Exception) {
                Log.e(tag, "Failed to load verses", e)
                view?.onVersesLoadFailed(bookIndex, chapterIndex)
            }
        }
    }

    @VisibleForTesting
    fun toSimpleVerseItems(verses: List<Verse>, bookName: String, highlights: List<Highlight>): List<SimpleVerseItem> =
            ArrayList<SimpleVerseItem>(verses.size).apply {
                val verseIterator = verses.iterator()
                var verse: Verse? = null
                val highlightIterator = highlights.iterator()
                var highlight: Highlight? = null
                while (verse != null || verseIterator.hasNext()) {
                    verse = verse ?: verseIterator.next()

                    val (nextVerse, parallel, followingEmptyVerseCount) = verseIterator.nextNonEmpty(verse)

                    val verseIndex = verse.verseIndex.verseIndex
                    if (highlight == null || highlight.verseIndex.verseIndex < verseIndex) {
                        while (highlightIterator.hasNext()) {
                            highlight = highlightIterator.next()
                            if (highlight.verseIndex.verseIndex >= verseIndex) {
                                break
                            }
                        }
                    }
                    val highlightColor = highlight
                            ?.let { if (it.verseIndex.verseIndex == verseIndex) it.color else Highlight.COLOR_NONE }
                            ?: Highlight.COLOR_NONE

                    add(SimpleVerseItem(verse.transform(parallel), bookName, verses.size,
                            followingEmptyVerseCount, highlightColor, this@VersePresenter::onVerseClicked,
                            this@VersePresenter::onVerseLongClicked))

                    verse = nextVerse
                }
            }

    // skips the empty verses, and concatenates the parallels
    private fun Iterator<Verse>.nextNonEmpty(current: Verse): Triple<Verse?, Array<StringBuilder>, Int> {
        val parallel = Array(current.parallel.size) { StringBuilder() }.append(current.parallel)

        var nextVerse: Verse? = null
        while (hasNext()) {
            nextVerse = next()
            if (nextVerse.text.text.isEmpty()) {
                parallel.append(nextVerse.parallel)
                nextVerse = null
            } else {
                break
            }
        }

        val followingEmptyVerseCount = nextVerse
                ?.let { it.verseIndex.verseIndex - 1 - current.verseIndex.verseIndex }
                ?: 0

        return Triple(nextVerse, parallel, followingEmptyVerseCount)
    }

    private fun Array<StringBuilder>.append(texts: List<Verse.Text>): Array<StringBuilder> {
        texts.forEachIndexed { index, text ->
            with(get(index)) {
                if (isNotEmpty()) append(' ')
                append(text.text)
            }
        }
        return this
    }

    private fun Verse.transform(concatenatedParallel: Array<StringBuilder>): Verse {
        if (parallel.isEmpty() || concatenatedParallel.isEmpty()) return this

        val parallelTexts = ArrayList<Verse.Text>(concatenatedParallel.size)
        parallel.forEachIndexed { index, text ->
            parallelTexts.add(Verse.Text(text.translationShortName, concatenatedParallel[index].toString()))
        }
        return copy(parallel = parallelTexts)
    }

    @VisibleForTesting
    fun toVerseItems(verses: List<Verse>, bookName: String, bookmarks: List<Bookmark>,
                     highlights: List<Highlight>, notes: List<Note>): List<VerseItem> =
            ArrayList<VerseItem>(verses.size).apply {
                val verseIterator = verses.iterator()
                var verse: Verse? = null
                val bookmarkIterator = bookmarks.iterator()
                var bookmark: Bookmark? = null
                val highlightIterator = highlights.iterator()
                var highlight: Highlight? = null
                val noteIterator = notes.iterator()
                var note: Note? = null
                while (verse != null || verseIterator.hasNext()) {
                    verse = verse ?: verseIterator.next()

                    val (nextVerse, parallel, followingEmptyVerseCount) = verseIterator.nextNonEmpty(verse)

                    val verseIndex = verse.verseIndex.verseIndex
                    if (note == null || note.verseIndex.verseIndex < verseIndex) {
                        while (noteIterator.hasNext()) {
                            note = noteIterator.next()
                            if (note.verseIndex.verseIndex >= verseIndex) {
                                break
                            }
                        }
                    }
                    val hasNote = note?.let { it.verseIndex.verseIndex == verseIndex } ?: false

                    if (highlight == null || highlight.verseIndex.verseIndex < verseIndex) {
                        while (highlightIterator.hasNext()) {
                            highlight = highlightIterator.next()
                            if (highlight.verseIndex.verseIndex >= verseIndex) {
                                break
                            }
                        }
                    }
                    val highlightColor = highlight
                            ?.let { if (it.verseIndex.verseIndex == verseIndex) it.color else Highlight.COLOR_NONE }
                            ?: Highlight.COLOR_NONE

                    if (bookmark == null || bookmark.verseIndex.verseIndex < verseIndex) {
                        while (bookmarkIterator.hasNext()) {
                            bookmark = bookmarkIterator.next()
                            if (bookmark.verseIndex.verseIndex >= verseIndex) {
                                break
                            }
                        }
                    }
                    val hasBookmark = bookmark?.let { it.verseIndex.verseIndex == verseIndex }
                            ?: false

                    add(VerseItem(verse.transform(parallel), bookName, followingEmptyVerseCount,
                            hasNote, highlightColor, hasBookmark, this@VersePresenter::onVerseClicked,
                            this@VersePresenter::onVerseLongClicked, this@VersePresenter::onNoteClicked,
                            this@VersePresenter::onHighlightClicked, this@VersePresenter::onBookmarkClicked))

                    verse = nextVerse
                }
            }

    @VisibleForTesting
    fun onVerseClicked(verse: Verse) {
        if (actionMode == null) {
            readingInteractor.openVerseDetail(verse.verseIndex, VerseDetailPagerAdapter.PAGE_VERSES)
            return
        }

        if (selectedVerses.contains(verse)) {
            // de-select the verse
            selectedVerses.remove(verse)
            if (selectedVerses.isEmpty()) {
                actionMode?.finish()
            }

            view?.onVerseDeselected(verse.verseIndex)
        } else {
            // select the verse
            selectedVerses.add(verse)

            view?.onVerseSelected(verse.verseIndex)
        }
    }

    @VisibleForTesting
    fun onVerseLongClicked(verse: Verse) {
        if (actionMode == null) {
            actionMode = readingInteractor.startActionMode(actionModeCallback)
        }

        onVerseClicked(verse)
    }

    @VisibleForTesting
    fun onNoteClicked(verseIndex: VerseIndex) {
        readingInteractor.openVerseDetail(verseIndex, VerseDetailPagerAdapter.PAGE_NOTE)
    }

    @VisibleForTesting
    fun onHighlightClicked(verseIndex: VerseIndex, @ColorInt currentHighlightColor: Int) {
        view?.onHighlightColorRequested(verseIndex, currentHighlightColor)
    }

    @VisibleForTesting
    fun onBookmarkClicked(verseIndex: VerseIndex, hasBookmark: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            if (hasBookmark) {
                readingInteractor.removeBookmark(verseIndex)
            } else {
                readingInteractor.addBookmark(verseIndex)
            }
        }
    }

    fun updateHighlight(verseIndex: VerseIndex, @ColorInt highlightColor: Int) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                if (highlightColor == Highlight.COLOR_NONE) {
                    readingInteractor.removeHighlight(verseIndex)
                } else {
                    readingInteractor.saveHighlight(verseIndex, highlightColor)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to update highlight", e)
                // TODO
            }
        }
    }
}
