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

package me.xizzhu.android.joshua.annotated.notes

import kotlinx.coroutines.channels.ReceiveChannel
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.core.*
import me.xizzhu.android.joshua.reading.ReadingActivity
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesInteractor

class NotesInteractor(private val notesActivity: NotesActivity,
                      private val bibleReadingManager: BibleReadingManager,
                      private val noteManager: NoteManager,
                      private val navigator: Navigator,
                      settingsManager: SettingsManager)
    : BaseAnnotatedVersesInteractor(notesActivity, bibleReadingManager, navigator, settingsManager, IS_LOADING) {
    suspend fun readNotes(@Constants.SortOrder sortOrder: Int): List<Note> = noteManager.read(sortOrder)

    override suspend fun openVerse(verseIndex: VerseIndex) {
        bibleReadingManager.saveCurrentVerseIndex(verseIndex)
        navigator.navigate(notesActivity, Navigator.SCREEN_READING, ReadingActivity.bundleForOpenNote())
    }

    override suspend fun observeSortOrder(): ReceiveChannel<Int> = noteManager.observeSortOrder()

    override suspend fun saveSortOrder(@Constants.SortOrder sortOrder: Int) {
        noteManager.saveSortOrder(sortOrder)
    }
}
