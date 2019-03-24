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

package me.xizzhu.android.joshua.utils

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.tests.BaseUnitTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

class BaseSettingsPresenterTest : BaseUnitTest() {
    @Mock
    private lateinit var baseSettingsInteractor: BaseSettingsInteractor
    @Mock
    private lateinit var baseSettingsView: BaseSettingsView

    private lateinit var currentSettings: BroadcastChannel<Settings>
    private lateinit var baseSettingsPresenter: BaseSettingsPresenter<BaseSettingsView>

    @Before
    override fun setup() {
        super.setup()

        currentSettings = ConflatedBroadcastChannel()
        Mockito.`when`(baseSettingsInteractor.observeSettings()).thenReturn(currentSettings.openSubscription())

        baseSettingsPresenter = object : BaseSettingsPresenter<BaseSettingsView>(baseSettingsInteractor) {}
        baseSettingsPresenter.attachView(baseSettingsView)
    }

    @After
    override fun tearDown() {
        baseSettingsPresenter.detachView()
        super.tearDown()
    }

    @Test
    fun testObserveSettings() {
        runBlocking {
            Mockito.verify(baseSettingsView, Mockito.never()).onSettingsUpdated(any())

            currentSettings.send(Settings.DEFAULT)
            Mockito.verify(baseSettingsView, Mockito.times(1)).onSettingsUpdated(Settings.DEFAULT)
        }
    }
}
