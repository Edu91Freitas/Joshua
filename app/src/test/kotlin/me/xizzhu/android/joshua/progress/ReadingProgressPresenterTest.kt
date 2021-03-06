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

package me.xizzhu.android.joshua.progress

import android.view.View
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.core.Bible
import me.xizzhu.android.joshua.core.ReadingProgress
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.infra.arch.ViewData
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.ui.fadeIn
import me.xizzhu.android.joshua.ui.recyclerview.CommonRecyclerView
import org.mockito.Mock
import org.mockito.Mockito.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReadingProgressPresenterTest : BaseUnitTest() {
    @Mock
    private lateinit var readingProgressActivity: ReadingProgressActivity
    @Mock
    private lateinit var navigator: Navigator
    @Mock
    private lateinit var readingProgressInteractor: ReadingProgressInteractor
    @Mock
    private lateinit var readingProgressListView: CommonRecyclerView

    private lateinit var readingProgressViewHolder: ReadingProgressViewHolder
    private lateinit var readingProgressPresenter: ReadingProgressPresenter

    @BeforeTest
    override fun setup() {
        super.setup()

        runBlocking {
            `when`(readingProgressInteractor.settings()).thenReturn(emptyFlow())
            `when`(readingProgressInteractor.bookNames()).thenReturn(ViewData.success(List(Bible.BOOK_COUNT) { i -> i.toString() }))
            `when`(readingProgressInteractor.readingProgress()).thenReturn(ViewData.success(ReadingProgress(0, 0L, emptyList())))

            readingProgressViewHolder = ReadingProgressViewHolder(readingProgressListView)
            readingProgressPresenter = ReadingProgressPresenter(readingProgressActivity, navigator, readingProgressInteractor, testDispatcher)
        }
    }

    @Test
    fun testObserveSettings() = testDispatcher.runBlockingTest {
        val settings = Settings(false, true, 1, true, true)
        `when`(readingProgressInteractor.settings()).thenReturn(flowOf(ViewData.loading(), ViewData.success(settings), ViewData.error()))

        readingProgressPresenter.create(readingProgressViewHolder)
        verify(readingProgressListView, times(1)).setSettings(settings)
        verify(readingProgressListView, never()).setSettings(Settings.DEFAULT)

        readingProgressPresenter.destroy()
    }

    @Test
    fun testLoadReadingProgress() = testDispatcher.runBlockingTest {
        `when`(readingProgressInteractor.readingProgress()).thenReturn(ViewData.success(ReadingProgress(5, 4321L, emptyList())))

        readingProgressPresenter.create(readingProgressViewHolder)
        readingProgressPresenter.start()

        with(inOrder(readingProgressInteractor, readingProgressListView)) {
            verify(readingProgressInteractor, times(1)).updateLoadingState(ViewData.loading())
            verify(readingProgressListView, times(1)).visibility = View.GONE
            verify(readingProgressListView, times(1)).setItems(any())
            verify(readingProgressListView, times(1)).fadeIn()
            verify(readingProgressInteractor, times(1)).updateLoadingState(ViewData.success(null))
        }

        readingProgressPresenter.stop()
        readingProgressPresenter.destroy()
    }

    @Test
    fun testLoadReadingProgressWithBookNamesException() = testDispatcher.runBlockingTest {
        val exception = RuntimeException("Random exception")
        `when`(readingProgressInteractor.bookNames()).thenThrow(exception)

        readingProgressPresenter.create(readingProgressViewHolder)
        readingProgressPresenter.start()

        with(inOrder(readingProgressInteractor, readingProgressListView)) {
            verify(readingProgressInteractor, times(1)).updateLoadingState(ViewData.loading())
            verify(readingProgressListView, times(1)).visibility = View.GONE
            verify(readingProgressInteractor, times(1)).updateLoadingState(ViewData.error(exception = exception))
        }
        verify(readingProgressListView, never()).setItems(any())
        verify(readingProgressListView, never()).fadeIn()
        verify(readingProgressInteractor, never()).updateLoadingState(ViewData.success(null))

        readingProgressPresenter.stop()
        readingProgressPresenter.destroy()
    }

    @Test
    fun testOpenChapter() = testDispatcher.runBlockingTest {
        readingProgressPresenter.create(readingProgressViewHolder)

        val bookIndex = 1
        val chapterIndex = 2
        readingProgressPresenter.openChapter(bookIndex, chapterIndex)
        verify(readingProgressInteractor, times(1)).saveCurrentVerseIndex(VerseIndex(bookIndex, chapterIndex, 0))
        verify(navigator, times(1)).navigate(readingProgressActivity, Navigator.SCREEN_READING)

        readingProgressPresenter.destroy()
    }

    @Test
    fun testOpenChapterWithException() = testDispatcher.runBlockingTest {
        `when`(readingProgressInteractor.saveCurrentVerseIndex(any())).thenThrow(RuntimeException("Random exception"))

        readingProgressPresenter.create(readingProgressViewHolder)
        readingProgressPresenter.openChapter(0, 0)
        readingProgressPresenter.destroy()
    }
}
