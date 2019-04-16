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

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.xizzhu.android.joshua.core.logger.Log
import kotlin.coroutines.CoroutineContext

abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
    private val tag: String = javaClass.simpleName

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate()")
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        Log.i(tag, "onStart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        Log.i(tag, "onResume()")
    }

    @CallSuper
    override fun onPause() {
        Log.i(tag, "onPause()")
        super.onPause()
    }

    @CallSuper
    override fun onStop() {
        Log.i(tag, "onStop()")
        super.onStop()
    }

    @CallSuper
    override fun onDestroy() {
        Log.i(tag, "onDestroy()")
        job.cancel()
        super.onDestroy()
    }
}
