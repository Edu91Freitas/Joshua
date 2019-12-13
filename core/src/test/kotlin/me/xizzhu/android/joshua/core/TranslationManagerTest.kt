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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.xizzhu.android.joshua.core.repository.TranslationRepository
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.tests.MockContents
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationManagerTest : BaseUnitTest() {
    @Mock
    private lateinit var translationRepository: TranslationRepository

    @Test
    fun testObserveInitialTranslations() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal())
                    .thenReturn(listOf(MockContents.kjvDownloadedTranslationInfo, MockContents.cuvTranslationInfo))
            val translationManager = TranslationManager(translationRepository)

            assertEquals(listOf(MockContents.kjvDownloadedTranslationInfo),
                    translationManager.observeDownloadedTranslations().first())
            assertEquals(listOf(MockContents.cuvTranslationInfo),
                    translationManager.observeAvailableTranslations().first())
        }
    }

    @Test
    fun testObserveInitialTranslationsWithException() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal()).thenThrow(RuntimeException("Random exception"))
            val translationManager = TranslationManager(translationRepository)

            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())
        }
    }

    @Test
    fun testUpdateTranslations() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal()).thenReturn(emptyList())
            val translationManager = TranslationManager(translationRepository)

            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())

            translationManager.updateTranslations(listOf(MockContents.kjvTranslationInfo,
                    MockContents.kjvDownloadedTranslationInfo, MockContents.kjvDownloadedTranslationInfo,
                    MockContents.kjvTranslationInfo, MockContents.kjvTranslationInfo,
                    MockContents.kjvDownloadedTranslationInfo))

            assertEquals(listOf(MockContents.kjvDownloadedTranslationInfo), translationManager.observeDownloadedTranslations().first())
            assertEquals(listOf(MockContents.kjvTranslationInfo), translationManager.observeAvailableTranslations().first())
        }
    }

    @Test
    fun testNotifyTranslationsUpdated() = testDispatcher.runBlockingTest {
        `when`(translationRepository.readTranslationsFromLocal()).thenReturn(emptyList())
        val translationManager = TranslationManager(translationRepository)

        val availableAsync = async { translationManager.observeAvailableTranslations().take(4).toList() }
        val downloadedAsync = async { translationManager.observeDownloadedTranslations().take(4).toList() }

        translationManager.notifyTranslationsUpdated(listOf(MockContents.kjvTranslationInfo), listOf(MockContents.kjvDownloadedTranslationInfo))
        translationManager.notifyTranslationsUpdated(listOf(MockContents.kjvTranslationInfo), emptyList())
        translationManager.notifyTranslationsUpdated(emptyList(), listOf(MockContents.kjvDownloadedTranslationInfo))

        assertEquals(
                listOf(emptyList(), listOf(MockContents.kjvTranslationInfo), listOf(MockContents.kjvTranslationInfo), emptyList()),
                availableAsync.await()
        )
        assertEquals(
                listOf(emptyList(), listOf(MockContents.kjvDownloadedTranslationInfo), emptyList(), listOf(MockContents.kjvDownloadedTranslationInfo)),
                downloadedAsync.await()
        )
    }

    @Test
    fun testDownloadTranslation() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal()).thenReturn(emptyList())
            val translationManager = TranslationManager(translationRepository)

            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())

            translationManager.updateTranslations(listOf(MockContents.cuvTranslationInfo, MockContents.kjvTranslationInfo))
            translationManager.downloadTranslation(MockContents.kjvTranslationInfo).collect()

            assertEquals(listOf(MockContents.cuvTranslationInfo),
                    translationManager.observeAvailableTranslations().first())
            assertEquals(listOf(MockContents.kjvDownloadedTranslationInfo),
                    translationManager.observeDownloadedTranslations().first())
        }
    }

    @Test
    fun testRemoveNonExistTranslation() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal()).thenReturn(emptyList())
            val translationManager = TranslationManager(translationRepository)

            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())

            translationManager.removeTranslation(TranslationInfo("non_exist", "name", "language", 12345L, false))

            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())
            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
        }
    }

    @Test
    fun testRemoveTranslation() {
        runBlocking {
            `when`(translationRepository.readTranslationsFromLocal()).thenReturn(emptyList())
            val translationManager = TranslationManager(translationRepository)

            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
            assertTrue(translationManager.observeAvailableTranslations().first().isEmpty())

            translationManager.updateTranslations(listOf(MockContents.cuvTranslationInfo, MockContents.kjvDownloadedTranslationInfo))
            translationManager.removeTranslation(MockContents.kjvDownloadedTranslationInfo)

            assertEquals(setOf(MockContents.kjvTranslationInfo, MockContents.cuvTranslationInfo),
                    translationManager.observeAvailableTranslations().first().toSet())
            assertTrue(translationManager.observeDownloadedTranslations().first().isEmpty())
        }
    }
}
