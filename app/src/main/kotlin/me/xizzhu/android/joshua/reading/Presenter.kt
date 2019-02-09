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

package me.xizzhu.android.joshua.reading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.xizzhu.android.joshua.model.BibleReadingManager
import me.xizzhu.android.joshua.utils.MVPPresenter
import java.lang.Exception

class ReadingPresenter(private val bibleReadingManager: BibleReadingManager) : MVPPresenter<ReadingView>() {
    fun loadCurrentTranslation() {
        launch(Dispatchers.Main) {
            try {
                val currentTranslation = withContext(Dispatchers.IO) {
                    bibleReadingManager.currentTranslation
                }
                if (currentTranslation.isEmpty()) {
                    view?.onNoCurrentTranslation()
                } else {
                    view?.onCurrentTranslationLoaded(currentTranslation)
                }
            } catch (e: Exception) {
                view?.onCurrentTranslationLoadFailed()
            }
        }
    }
}
