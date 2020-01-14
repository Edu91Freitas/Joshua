/*
 * Copyright (C) 2020 Xizhi Zhu
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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.infra.arch.ViewData
import me.xizzhu.android.joshua.infra.interactors.BaseSettingsAwareInteractor
import me.xizzhu.android.joshua.reading.VerseDetailRequest
import me.xizzhu.android.joshua.reading.VerseUpdate
import me.xizzhu.android.joshua.utils.currentTimeMillis
import me.xizzhu.android.logger.Log

class VerseDetailInteractor(private val translationManager: TranslationManager,
                            private val bibleReadingManager: BibleReadingManager,
                            private val bookmarkManager: VerseAnnotationManager<Bookmark>,
                            private val highlightManager: VerseAnnotationManager<Highlight>,
                            private val noteManager: VerseAnnotationManager<Note>,
                            private val strongNumberManager: StrongNumberManager,
                            settingsManager: SettingsManager,
                            dispatcher: CoroutineDispatcher = Dispatchers.Default)
    : BaseSettingsAwareInteractor(settingsManager, dispatcher) {
    // TODO migrate when https://github.com/Kotlin/kotlinx.coroutines/issues/1082 is done
    private val verseDetailRequest: BroadcastChannel<VerseDetailRequest> = ConflatedBroadcastChannel()
    private val verseUpdates: BroadcastChannel<VerseUpdate> = ConflatedBroadcastChannel()

    fun verseDetailRequest(): Flow<VerseDetailRequest> = verseDetailRequest.asFlow()

    fun requestVerseDetail(request: VerseDetailRequest) {
        verseDetailRequest.offer(request)
    }

    fun closeVerseDetail(verseIndex: VerseIndex) {
        // NOTE It's a hack here, because the only thing needed by the other end (verse interactor) is to deselect the verse
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.VERSE_DESELECTED))
    }

    fun verseUpdates(): Flow<VerseUpdate> = verseUpdates.asFlow()

    fun currentVerseIndex(): Flow<VerseIndex> = bibleReadingManager.currentVerseIndex()

    suspend fun currentTranslation(): String = bibleReadingManager.currentTranslation().first()

    suspend fun saveCurrentTranslation(translationShortName: String) {
        bibleReadingManager.saveCurrentTranslation(translationShortName)
    }

    suspend fun downloadedTranslations(): List<TranslationInfo> = translationManager.downloadedTranslations().first()

    suspend fun readBookNames(translationShortName: String): List<String> =
            bibleReadingManager.readBookNames(translationShortName)

    suspend fun readVerses(translationShortName: String, parallelTranslations: List<String>,
                           bookIndex: Int, chapterIndex: Int): List<Verse> =
            bibleReadingManager.readVerses(translationShortName, parallelTranslations, bookIndex, chapterIndex)

    suspend fun readBookmark(verseIndex: VerseIndex): Bookmark = bookmarkManager.read(verseIndex)

    suspend fun addBookmark(verseIndex: VerseIndex) {
        bookmarkManager.save(Bookmark(verseIndex, currentTimeMillis()))
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.BOOKMARK_ADDED))
    }

    suspend fun removeBookmark(verseIndex: VerseIndex) {
        bookmarkManager.remove(verseIndex)
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.BOOKMARK_REMOVED))
    }

    suspend fun readHighlight(verseIndex: VerseIndex): Highlight = highlightManager.read(verseIndex)

    suspend fun saveHighlight(verseIndex: VerseIndex, @Highlight.Companion.AvailableColor color: Int) {
        highlightManager.save(Highlight(verseIndex, color, currentTimeMillis()))
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.HIGHLIGHT_UPDATED, color))
    }

    suspend fun removeHighlight(verseIndex: VerseIndex) {
        highlightManager.remove(verseIndex)
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.HIGHLIGHT_UPDATED, Highlight.COLOR_NONE))
    }

    suspend fun readNote(verseIndex: VerseIndex): Note = noteManager.read(verseIndex)

    suspend fun saveNote(verseIndex: VerseIndex, note: String) {
        noteManager.save(Note(verseIndex, note, currentTimeMillis()))
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.NOTE_ADDED))
    }

    suspend fun removeNote(verseIndex: VerseIndex) {
        noteManager.remove(verseIndex)
        verseUpdates.offer(VerseUpdate(verseIndex, VerseUpdate.NOTE_REMOVED))
    }

    suspend fun readStrongNumber(verseIndex: VerseIndex): List<StrongNumber> = strongNumberManager.readStrongNumber(verseIndex)

    fun downloadStrongNumber(): Flow<ViewData<Int>> =
            strongNumberManager.download()
                    .map { progress ->
                        if (progress <= 100) {
                            ViewData.loading(progress)
                        } else {
                            // Ideally, we should use onCompletion() to handle this. However, it doesn't
                            // distinguish between a successful completion and a cancellation.
                            // See https://github.com/Kotlin/kotlinx.coroutines/issues/1693
                            ViewData.success(-1)
                        }
                    }
                    .catch { cause ->
                        Log.e(tag, "Failed to download Strong number", cause)
                        emit(ViewData.error(exception = cause))
                    }
}
