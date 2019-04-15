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

import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.CoroutineContext

interface MVPView

abstract class MVPPresenter<V : MVPView> : CoroutineScope {
    protected val tag: String = javaClass.simpleName

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    protected val receiveChannels = ArrayList<ReceiveChannel<*>>()

    protected var view: V? = null
        private set

    fun attachView(v: V) {
        view = v
        onViewAttached()
    }

    @CallSuper
    protected open fun onViewAttached() {
    }

    fun detachView() {
        onViewDetached()

        job.cancelChildren()

        for (r in receiveChannels) {
            r.cancel()
        }
        receiveChannels.clear()

        view = null
    }

    @CallSuper
    protected open fun onViewDetached() {
    }
}
