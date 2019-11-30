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

package me.xizzhu.android.joshua.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.core.repository.VerseAnnotationRepository
import me.xizzhu.android.joshua.tests.BaseUnitTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class NoteManagerTest : BaseUnitTest() {
    @Mock
    private lateinit var noteRepository: VerseAnnotationRepository<Note>

    private lateinit var noteManager: NoteManager

    @Test
    fun testObserveInitialSortOrder() {
        runBlocking {
            `when`(noteRepository.readSortOrder()).thenReturn(Constants.SORT_BY_BOOK)
            noteManager = NoteManager(noteRepository)

            assertEquals(Constants.SORT_BY_BOOK, noteManager.observeSortOrder().first())
        }
    }

    @Test
    fun testObserveInitialSortOrderWithException() {
        runBlocking {
            `when`(noteRepository.readSortOrder()).thenThrow(RuntimeException("Random exception"))
            noteManager = NoteManager(noteRepository)

            assertEquals(Constants.DEFAULT_SORT_ORDER, noteManager.observeSortOrder().first())
        }
    }

    @Test
    fun testSaveSortOrder() {
        runBlocking {
            `when`(noteRepository.readSortOrder()).thenReturn(Constants.DEFAULT_SORT_ORDER)
            noteManager = NoteManager(noteRepository)

            assertEquals(Constants.DEFAULT_SORT_ORDER, noteManager.observeSortOrder().first())

            noteManager.saveSortOrder(Constants.SORT_BY_BOOK)
            verify(noteRepository, times(1)).saveSortOrder(Constants.SORT_BY_BOOK)
            assertEquals(Constants.SORT_BY_BOOK, noteManager.observeSortOrder().first())
        }
    }
}
