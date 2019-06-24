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

package me.xizzhu.android.joshua.translations

import android.content.Context
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.TranslationInfo
import me.xizzhu.android.joshua.tests.BaseUnitTest
import me.xizzhu.android.joshua.tests.MockContents
import me.xizzhu.android.joshua.ui.SwipeRefresherState
import me.xizzhu.android.joshua.ui.recyclerview.TitleItem
import me.xizzhu.android.joshua.ui.recyclerview.TranslationItem
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class TranslationPresenterTest : BaseUnitTest() {
    @Mock
    private lateinit var translationInteractor: TranslationInteractor
    @Mock
    private lateinit var translationView: TranslationView
    @Mock
    private lateinit var context: Context

    private lateinit var translationPresenter: TranslationPresenter
    private lateinit var settingsChannel: ConflatedBroadcastChannel<Settings>
    private lateinit var translationLoadingStateChannel: ConflatedBroadcastChannel<SwipeRefresherState>
    private lateinit var translationsLoadingRequest: BroadcastChannel<Unit>
    private lateinit var availableTranslationsChannel: ConflatedBroadcastChannel<List<TranslationInfo>>
    private lateinit var downloadedTranslationsChannel: ConflatedBroadcastChannel<List<TranslationInfo>>
    private lateinit var currentTranslationChannel: ConflatedBroadcastChannel<String>

    @Before
    override fun setup() {
        super.setup()

        runBlocking {
            settingsChannel = ConflatedBroadcastChannel(Settings.DEFAULT)
            `when`(translationInteractor.observeSettings()).thenReturn(settingsChannel.openSubscription())

            translationLoadingStateChannel = ConflatedBroadcastChannel(SwipeRefresherState.IS_REFRESHING)
            `when`(translationInteractor.observeTranslationsLoadingState()).then { translationLoadingStateChannel.openSubscription() }

            translationsLoadingRequest = ConflatedBroadcastChannel()
            `when`(translationInteractor.observeTranslationsLoadingRequest()).then { translationsLoadingRequest.openSubscription() }

            availableTranslationsChannel = ConflatedBroadcastChannel(emptyList())
            `when`(translationInteractor.observeAvailableTranslations()).then { availableTranslationsChannel.openSubscription() }

            downloadedTranslationsChannel = ConflatedBroadcastChannel(emptyList())
            `when`(translationInteractor.observeDownloadedTranslations()).then { downloadedTranslationsChannel.openSubscription() }

            currentTranslationChannel = ConflatedBroadcastChannel("")
            `when`(translationInteractor.observeCurrentTranslation()).then { currentTranslationChannel.openSubscription() }

            `when`(context.getString(anyInt())).thenReturn("")

            translationPresenter = TranslationPresenter(translationInteractor, context)
        }
    }

    @Test
    fun testObserveTranslations() {
        runBlocking {
            translationPresenter.attachView(translationView)

            currentTranslationChannel.send(MockContents.kjvShortName)
            availableTranslationsChannel.send(listOf(MockContents.cuvTranslationInfo))
            downloadedTranslationsChannel.send(listOf(MockContents.kjvDownloadedTranslationInfo))

            val expected = listOf(
                    TranslationItem(MockContents.kjvDownloadedTranslationInfo, true, translationPresenter::onTranslationClicked, translationPresenter::onTranslationLongClicked),
                    TitleItem("", false),
                    TranslationItem(MockContents.cuvTranslationInfo, false, translationPresenter::onTranslationClicked, translationPresenter::onTranslationLongClicked)
            )
            verify(translationView, times(1)).onTranslationsUpdated(expected)

            translationPresenter.detachView()
        }
    }

    @Test
    fun testObserveTranslationsLoadingState() {
        runBlocking {
            translationPresenter.attachView(translationView)
            verify(translationView, times(1)).onTranslationsLoadingStarted()

            translationLoadingStateChannel.send(SwipeRefresherState.NOT_REFRESHING)
            verify(translationView, times(1)).onTranslationsLoadingCompleted()

            translationPresenter.detachView()
        }
    }

    @Test
    fun testRefreshRequest() {
        runBlocking {
            translationPresenter.attachView(translationView)

            verify(translationInteractor, never()).reload(true)
            verify(translationInteractor, times(1)).reload(false)

            translationsLoadingRequest.send(Unit)
            verify(translationInteractor, times(1)).reload(true)
            verify(translationInteractor, times(1)).reload(false)

            translationPresenter.detachView()
        }
    }

    @Test
    fun testLoadTranslationList() {
        runBlocking {
            translationPresenter.attachView(translationView)

            verify(translationInteractor, never()).reload(true)
            verify(translationInteractor, times(1)).reload(false)
            verify(translationView, never()).onTranslationsLoadingFailed(anyBoolean())

            translationPresenter.detachView()
        }
    }

    @Test
    fun testLoadTranslationListWithException() {
        runBlocking {
            `when`(translationInteractor.reload(anyBoolean())).thenThrow(RuntimeException("Random exception"))

            translationPresenter.attachView(translationView)

            verify(translationInteractor, never()).reload(true)
            verify(translationInteractor, times(1)).reload(false)
            verify(translationView, times(1)).onTranslationsLoadingFailed(false)
            verify(translationView, never()).onTranslationsLoadingFailed(true)

            translationPresenter.detachView()
        }
    }

    @Test
    fun testDownloadTranslation() {
        runBlocking {
            translationPresenter.attachView(translationView)

            val downloadProgressChannel = Channel<Int>()
            translationPresenter.downloadTranslation(MockContents.kjvTranslationInfo, downloadProgressChannel)

            val progress = 89
            downloadProgressChannel.send(progress)
            downloadProgressChannel.close()

            verify(translationInteractor, times(1))
                    .downloadTranslation(downloadProgressChannel, MockContents.kjvTranslationInfo)
            verify(translationView, times(1))
                    .onTranslationDownloadProgressed(progress)
            verify(translationInteractor, times(1))
                    .saveCurrentTranslation(MockContents.kjvShortName)
            verify(translationView, times(1))
                    .onTranslationDownloaded()

            translationPresenter.detachView()
        }
    }

    @Test
    fun testDownloadTranslationWithException() {
        runBlocking {
            val downloadProgressChannel = Channel<Int>()
            `when`(translationInteractor.downloadTranslation(downloadProgressChannel, MockContents.kjvTranslationInfo))
                    .thenThrow(RuntimeException("Random exception"))

            translationPresenter.attachView(translationView)
            translationPresenter.downloadTranslation(MockContents.kjvTranslationInfo, downloadProgressChannel)
            downloadProgressChannel.close()

            verify(translationInteractor, times(1))
                    .downloadTranslation(downloadProgressChannel, MockContents.kjvTranslationInfo)
            verify(translationView, never()).onTranslationDownloaded()
            verify(translationView, times(1)).onTranslationDownloadFailed(MockContents.kjvTranslationInfo)

            translationPresenter.detachView()
        }
    }

    @Test
    fun testNonExistRemoveTranslation() {
        translationPresenter.attachView(translationView)

        val translationToRemove = TranslationInfo("non_exist", "name", "language", 12345L, false)
        translationPresenter.removeTranslation(translationToRemove)
        verify(translationView, times(1)).onTranslationDeleteStarted()
        verify(translationView, times(1)).onTranslationDeleted()
        verify(translationView, never()).onTranslationDeleteFailed(translationToRemove)

        translationPresenter.detachView()
    }

    @Test
    fun testDownloadThenRemoveTranslation() {
        runBlocking {
            translationPresenter.attachView(translationView)

            val downloadProgressChannel = Channel<Int>()
            translationPresenter.downloadTranslation(MockContents.kjvTranslationInfo, downloadProgressChannel)
            downloadProgressChannel.close()
            verify(translationView, times(1)).onTranslationDownloaded()

            translationPresenter.removeTranslation(MockContents.kjvTranslationInfo)
            verify(translationView, times(1)).onTranslationDeleteStarted()
            verify(translationView, times(1)).onTranslationDeleted()
            verify(translationView, never()).onTranslationDeleteFailed(MockContents.kjvTranslationInfo)

            translationPresenter.detachView()
        }
    }

    @Test
    fun testRemoveTranslationWithException() {
        runBlocking {
            `when`(translationInteractor.removeTranslation(MockContents.kjvTranslationInfo))
                    .thenThrow(RuntimeException("Random exception"))

            translationPresenter.attachView(translationView)

            translationPresenter.removeTranslation(MockContents.kjvTranslationInfo)
            verify(translationView, times(1)).onTranslationDeleteStarted()
            verify(translationView, times(1)).onTranslationDeleteFailed(MockContents.kjvTranslationInfo)
            verify(translationView, never()).onTranslationDeleted()

            translationPresenter.detachView()
        }
    }

    @Test
    fun testUpdateCurrentTranslationWithException() {
        runBlocking {
            `when`(translationInteractor.saveCurrentTranslation(MockContents.kjvShortName))
                    .thenThrow(RuntimeException("Random exception"))

            translationPresenter.attachView(translationView)

            translationPresenter.updateCurrentTranslation(MockContents.kjvShortName)
            verify(translationView, times(1)).onCurrentTranslationUpdateFailed(MockContents.kjvShortName)

            translationPresenter.detachView()
        }
    }
}
