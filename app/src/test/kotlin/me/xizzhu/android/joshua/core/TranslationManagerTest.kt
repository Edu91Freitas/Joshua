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

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.core.repository.TranslationRepository
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.tests.MockContents
import me.xizzhu.android.joshua.tests.MockLocalTranslationStorage
import me.xizzhu.android.joshua.tests.MockRemoteTranslationService
import me.xizzhu.android.joshua.utils.onEach
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationManagerTest : BaseUnitTest() {
    private lateinit var translationManager: TranslationManager

    @Before
    override fun setUp() {
        super.setUp()
        translationManager = TranslationManager(TranslationRepository(
                MockLocalTranslationStorage(), MockRemoteTranslationService()))
    }

    @Test
    fun testDefaultAvailableTranslations() {
        val expected = emptyList<TranslationInfo>()
        val actual = runBlocking { translationManager.observeAvailableTranslations().receive() }
        assertEquals(expected, actual)
    }

    @Test
    fun testDefaultDownloadedTranslations() {
        val expected = emptyList<TranslationInfo>()
        val actual = runBlocking { translationManager.observeDownloadedTranslations().receive() }
        assertEquals(expected, actual)
    }

    @Test
    fun testReload() {
        runBlocking {
            val expectedAvailable = listOf(MockContents.kjvTranslationInfo)
            val expectedDownloaded = emptyList<TranslationInfo>()

            translationManager.reload(false)
            val actualAvailable = translationManager.observeAvailableTranslations().receive()
            val actualDownloaded = translationManager.observeDownloadedTranslations().receive()

            assertEquals(expectedAvailable, actualAvailable)
            assertEquals(expectedDownloaded, actualDownloaded)
        }
    }

    @Test
    fun testDownloadTranslation() {
        runBlocking {
            val expectedAvailable = emptyList<TranslationInfo>()
            val expectedDownloaded = listOf(MockContents.kjvDownloadedTranslationInfo)

            val channel = Channel<Int>()
            launch {
                translationManager.downloadTranslation(channel, MockContents.kjvTranslationInfo)
            }
            var called = false
            channel.onEach { called = true }
            assertTrue(called)

            val actualAvailable = translationManager.observeAvailableTranslations().receive()
            val actualDownloaded = translationManager.observeDownloadedTranslations().receive()

            assertEquals(expectedAvailable, actualAvailable)
            assertEquals(expectedDownloaded, actualDownloaded)
        }
    }

    @Test
    fun testDownloadTranslationThenReload() {
        runBlocking {
            val expectedAvailable = emptyList<TranslationInfo>()
            val expectedDownloaded = listOf(MockContents.kjvDownloadedTranslationInfo)

            val channel = Channel<Int>()
            launch {
                translationManager.downloadTranslation(channel, MockContents.kjvTranslationInfo)
            }
            var called = false
            channel.onEach { called = true }
            assertTrue(called)

            translationManager.reload(false)
            val actualAvailable = translationManager.observeAvailableTranslations().receive()
            val actualDownloaded = translationManager.observeDownloadedTranslations().receive()

            assertEquals(expectedAvailable, actualAvailable)
            assertEquals(expectedDownloaded, actualDownloaded)
        }
    }

    @Test
    fun testDownloadTranslationThenReloadWithForceRefresh() {
        runBlocking {
            val expectedAvailable = emptyList<TranslationInfo>()
            val expectedDownloaded = listOf(MockContents.kjvDownloadedTranslationInfo)

            val channel = Channel<Int>()
            launch {
                translationManager.downloadTranslation(channel, MockContents.kjvTranslationInfo)
            }
            var called = false
            channel.onEach { called = true }
            assertTrue(called)

            translationManager.reload(true)
            val actualAvailable = translationManager.observeAvailableTranslations().receive()
            val actualDownloaded = translationManager.observeDownloadedTranslations().receive()

            assertEquals(expectedAvailable, actualAvailable)
            assertEquals(expectedDownloaded, actualDownloaded)
        }
    }
}
