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

package me.xizzhu.android.joshua.ui

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.runBlocking
import me.xizzhu.android.joshua.tests.BaseUnitTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class SwipeRefresherPresenterTest : BaseUnitTest() {
    @Mock
    private lateinit var swipeRefresherView: SwipeRefresherView
    @Mock
    private lateinit var refreshRequest: BroadcastChannel<Unit>

    private lateinit var swipeRefresherState: BroadcastChannel<SwipeRefresherState>
    private lateinit var swipeRefresherPresenter: SwipeRefresherPresenter

    @Before
    override fun setup() {
        super.setup()

        swipeRefresherState = ConflatedBroadcastChannel()
        swipeRefresherPresenter = SwipeRefresherPresenter(swipeRefresherState.openSubscription(), refreshRequest)

        swipeRefresherPresenter.attachView(swipeRefresherView)
    }

    @After
    override fun tearDown() {
        swipeRefresherPresenter.detachView()
        super.tearDown()
    }

    @Test
    fun testRefresherState() {
        runBlocking {
            verify(swipeRefresherView, never()).show()
            verify(swipeRefresherView, never()).hide()

            swipeRefresherState.send(SwipeRefresherState.IS_REFRESHING)
            verify(swipeRefresherView, times(1)).show()
            verify(swipeRefresherView, never()).hide()

            swipeRefresherState.send(SwipeRefresherState.NOT_REFRESHING)
            verify(swipeRefresherView, times(1)).show()
            verify(swipeRefresherView, times(1)).hide()
        }
    }

    @Test
    fun testRefresh() {
        runBlocking {
            verify(refreshRequest, never()).send(any())

            swipeRefresherPresenter.refresh()
            verify(refreshRequest, times(1)).send(Unit)
        }
    }
}
