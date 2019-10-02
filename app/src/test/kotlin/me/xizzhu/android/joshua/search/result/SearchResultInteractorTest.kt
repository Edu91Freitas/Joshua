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

package me.xizzhu.android.joshua.search.result

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.infra.arch.ViewData
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.tests.MockContents
import org.mockito.Mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultInteractorTest : BaseUnitTest() {
    @Mock
    private lateinit var bibleReadingManager: BibleReadingManager
    @Mock
    private lateinit var settingsManager: SettingsManager

    private lateinit var searchResultInteractor: SearchResultInteractor

    @BeforeTest
    override fun setup() {
        super.setup()

        searchResultInteractor = SearchResultInteractor(bibleReadingManager, settingsManager, testDispatcher)
    }

    @Test
    fun testSearch() = testDispatcher.runBlockingTest {
        val currentTranslation = MockContents.kjvShortName
        val query = "query"
        val verses = MockContents.kjvVerses
        `when`(bibleReadingManager.observeCurrentTranslation()).thenReturn(flowOf(currentTranslation))
        `when`(bibleReadingManager.search(currentTranslation, query)).thenReturn(verses)

        val searchResultAsync = async { searchResultInteractor.searchResult().take(2).toList() }
        searchResultInteractor.search(query)
        assertEquals(
                listOf(
                        ViewData.loading(SearchResult(query)),
                        ViewData.success(SearchResult(query, verses))
                ),
                searchResultAsync.await()
        )
    }

    @Test
    fun testSearchWithException() = testDispatcher.runBlockingTest {
        val exception = RuntimeException("random error")
        `when`(bibleReadingManager.observeCurrentTranslation()).thenThrow(exception)

        val searchResultAsync = async { searchResultInteractor.searchResult().take(2).toList() }

        val query = "query"
        searchResultInteractor.search(query)
        assertEquals(
                listOf(
                        ViewData.loading(SearchResult(query)),
                        ViewData.error(SearchResult(query), exception)
                ),
                searchResultAsync.await()
        )
    }
}
