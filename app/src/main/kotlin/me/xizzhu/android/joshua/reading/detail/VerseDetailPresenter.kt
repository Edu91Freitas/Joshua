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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Highlight
import me.xizzhu.android.joshua.core.StrongNumber
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.infra.arch.ViewHolder
import me.xizzhu.android.joshua.infra.arch.onEachSuccess
import me.xizzhu.android.joshua.infra.interactors.BaseSettingsAwarePresenter
import me.xizzhu.android.joshua.reading.ReadingActivity
import me.xizzhu.android.joshua.reading.VerseDetailRequest
import me.xizzhu.android.joshua.reading.verse.toStringForSharing
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.ui.ToastHelper
import me.xizzhu.android.joshua.ui.TranslationInfoComparator
import me.xizzhu.android.joshua.utils.supervisedAsync
import me.xizzhu.android.logger.Log
import java.lang.StringBuilder
import kotlin.math.max

data class VerseDetailViewHolder(val verseDetailViewLayout: VerseDetailViewLayout) : ViewHolder

class VerseDetailPresenter(private val readingActivity: ReadingActivity,
                           verseDetailInteractor: VerseDetailInteractor,
                           dispatcher: CoroutineDispatcher = Dispatchers.Main)
    : BaseSettingsAwarePresenter<VerseDetailViewHolder, VerseDetailInteractor>(verseDetailInteractor, dispatcher) {
    private val translationComparator = TranslationInfoComparator(
            TranslationInfoComparator.SORT_ORDER_LANGUAGE_THEN_SHORT_NAME)

    @VisibleForTesting
    var verseDetail: VerseDetail? = null
    private var updateBookmarkJob: Job? = null
    private var updateHighlightJob: Job? = null
    private var updateNoteJob: Job? = null

    @UiThread
    override fun onCreate(viewHolder: VerseDetailViewHolder) {
        super.onCreate(viewHolder)

        with(viewHolder.verseDetailViewLayout) {
            setOnClickListener { close() }
            setOnBookmarkClickedListener { updateBookmark() }
            setOnHighlightClickedListener {
                DialogHelper.showDialog(readingActivity, R.string.text_pick_highlight_color,
                        resources.getStringArray(R.array.text_colors),
                        max(0, Highlight.AVAILABLE_COLORS.indexOf(verseDetail?.highlightColor
                                ?: Highlight.COLOR_NONE)),
                        DialogInterface.OnClickListener { dialog, which ->
                            updateHighlight(Highlight.AVAILABLE_COLORS[which])

                            dialog.dismiss()
                        })
            }
            setOnNoteUpdatedListener { updateNote(it) }

            interactor.settings().onEachSuccess { viewHolder.verseDetailViewLayout.setSettings(it) }.launchIn(coroutineScope)
            interactor.verseDetailRequest().onEach { showVerseDetail(it.verseIndex, it.content) }.launchIn(coroutineScope)
            interactor.currentVerseIndex().onEach { close() }.launchIn(coroutineScope)

            post { hide() }
        }
    }

    private fun updateBookmark() {
        updateBookmarkJob?.cancel()
        updateBookmarkJob = coroutineScope.launch {
            verseDetail?.let { detail ->
                if (detail.bookmarked) {
                    interactor.removeBookmark(detail.verseIndex)
                    verseDetail = detail.copy(bookmarked = false)
                } else {
                    interactor.addBookmark(detail.verseIndex)
                    verseDetail = detail.copy(bookmarked = true)
                }
                viewHolder?.verseDetailViewLayout?.setVerseDetail(verseDetail!!)
            }

            updateBookmarkJob = null
        }
    }

    private fun updateHighlight(@Highlight.Companion.AvailableColor highlightColor: Int) {
        updateHighlightJob?.cancel()
        updateHighlightJob = coroutineScope.launch {
            verseDetail?.let { detail ->
                if (highlightColor == Highlight.COLOR_NONE) {
                    interactor.removeHighlight(detail.verseIndex)
                } else {
                    interactor.saveHighlight(detail.verseIndex, highlightColor)
                }
                verseDetail = detail.copy(highlightColor = highlightColor)
                viewHolder?.verseDetailViewLayout?.setVerseDetail(verseDetail!!)
            }

            updateHighlightJob = null
        }
    }

    private fun updateNote(note: String) {
        updateNoteJob?.cancel()
        updateNoteJob = coroutineScope.launch {
            verseDetail?.let { detail ->
                if (note.isEmpty()) {
                    interactor.removeNote(detail.verseIndex)
                } else {
                    interactor.saveNote(detail.verseIndex, note)
                }
                verseDetail = detail.copy(note = note)
            }

            updateNoteJob = null
        }
    }

    private fun showVerseDetail(verseIndex: VerseIndex, @VerseDetailRequest.Companion.Content content: Int) {
        loadVerseDetail(verseIndex)
        viewHolder?.verseDetailViewLayout?.show(content)
    }

    private fun loadVerseDetail(verseIndex: VerseIndex) {
        coroutineScope.launch {
            try {
                viewHolder?.verseDetailViewLayout?.setVerseDetail(VerseDetail.INVALID)

                val bookmarkAsync = supervisedAsync { interactor.readBookmark(verseIndex) }
                val highlightAsync = supervisedAsync { interactor.readHighlight(verseIndex) }
                val noteAsync = supervisedAsync { interactor.readNote(verseIndex) }
                val strongNumberAsync = supervisedAsync { interactor.readStrongNumber(verseIndex) }

                verseDetail = VerseDetail(verseIndex, buildVerseTextItems(verseIndex),
                        bookmarkAsync.await().isValid(), highlightAsync.await().color,
                        noteAsync.await().note, strongNumberAsync.await().toStrongNumberItems(verseIndex))
                viewHolder?.verseDetailViewLayout?.setVerseDetail(verseDetail!!)
            } catch (e: Exception) {
                Log.e(tag, "Failed to load verse detail", e)
                DialogHelper.showDialog(readingActivity, true, R.string.dialog_load_verse_detail_error,
                        DialogInterface.OnClickListener { _, _ -> loadVerseDetail(verseIndex) })
            }
        }
    }

    @VisibleForTesting
    suspend fun buildVerseTextItems(verseIndex: VerseIndex): List<VerseTextItem> {
        val currentTranslation = interactor.currentTranslation()
        val parallelTranslations = interactor.downloadedTranslations()
                .sortedWith(translationComparator)
                .filter { it.shortName != currentTranslation }
                .map { it.shortName }
        val verses = interactor.readVerses(currentTranslation, parallelTranslations,
                verseIndex.bookIndex, verseIndex.chapterIndex)

        // 1. finds the verse
        var start: VerseIndex? = null
        for (verse in verses) {
            if (verse.text.text.isNotEmpty()) start = verse.verseIndex // we need to consider the empty verses
            if (verse.verseIndex.verseIndex >= verseIndex.verseIndex) break
        }

        val verseIterator = verses.iterator()
        var verse: Verse? = null
        while (verseIterator.hasNext()) {
            val v = verseIterator.next()
            if (v.verseIndex == start) {
                verse = v
                break
            }
        }
        if (verse == null) throw IllegalStateException("Failed to find target verse")

        // 2. builds the parallel
        val parallel = Array(parallelTranslations.size) { StringBuilder() }
        val parallelBuilder: (index: Int, Verse.Text) -> Unit = { index, text ->
            with(parallel[index]) {
                if (isNotEmpty()) append(' ')
                append(text.text)
            }
        }
        verse.parallel.forEachIndexed(parallelBuilder)

        var followingEmptyVerseCount = 0
        while (verseIterator.hasNext()) {
            val v = verseIterator.next()
            if (v.text.text.isNotEmpty()) break
            v.parallel.forEachIndexed(parallelBuilder)
            followingEmptyVerseCount++
        }

        // 3. constructs VerseTextItems
        val verseTextItems = ArrayList<VerseTextItem>(parallelTranslations.size + 1)
        verseTextItems.add(VerseTextItem(verse.verseIndex, followingEmptyVerseCount, verse.text,
                interactor.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex],
                this@VerseDetailPresenter::onVerseClicked, this@VerseDetailPresenter::onVerseLongClicked))

        parallelTranslations.forEachIndexed { index, translation ->
            verseTextItems.add(VerseTextItem(verse.verseIndex, followingEmptyVerseCount,
                    Verse.Text(translation, parallel[index].toString()),
                    interactor.readBookNames(translation)[verse.verseIndex.bookIndex],
                    this@VerseDetailPresenter::onVerseClicked, this@VerseDetailPresenter::onVerseLongClicked))
        }

        return verseTextItems
    }

    @VisibleForTesting
    fun onVerseClicked(translation: String) {
        coroutineScope.launch {
            try {
                if (translation != interactor.currentTranslation()) {
                    interactor.saveCurrentTranslation(translation)
                    close()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to select translation", e)
                ToastHelper.showToast(readingActivity, R.string.toast_unknown_error)
            }
        }
    }

    @VisibleForTesting
    fun onVerseLongClicked(verse: Verse) {
        coroutineScope.launch {
            try {
                val bookName = interactor.readBookNames(verse.text.translationShortName)[verse.verseIndex.bookIndex]
                // On older devices, this only works on the threads with loopers.
                (readingActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText(verse.text.translationShortName + " " + bookName,
                                verse.toStringForSharing(bookName)))
                ToastHelper.showToast(readingActivity, R.string.toast_verses_copied)
            } catch (e: Exception) {
                Log.e(tag, "Failed to copy", e)
                ToastHelper.showToast(readingActivity, R.string.toast_unknown_error)
            }
        }
    }

    private fun List<StrongNumber>.toStrongNumberItems(verseIndex: VerseIndex): List<StrongNumberItem> {
        // TODO
        return map { StrongNumberItem(verseIndex, it) }
    }

    /**
     * @return true if verse detail view was open, or false otherwise
     * */
    fun close(): Boolean {
        viewHolder?.verseDetailViewLayout?.hide()

        return verseDetail?.let {
            interactor.closeVerseDetail(it.verseIndex)
            verseDetail = null
            true
        } ?: false
    }
}
