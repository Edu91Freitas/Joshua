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

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import me.xizzhu.android.joshua.core.internal.repository.TranslationRepository

class TranslationManager(private val translationRepository: TranslationRepository) {
    @WorkerThread
    fun readTranslations(forceRefresh: Boolean): List<TranslationInfo> =
            translationRepository.readTranslations(forceRefresh)

    @WorkerThread
    fun readDownloadedTranslations(): List<TranslationInfo> =
            translationRepository.readDownloadedTranslations()

    fun downloadTranslation(scope: CoroutineScope, dispatcher: CoroutineDispatcher,
                            translationInfo: TranslationInfo): ReceiveChannel<Int> =
            scope.produce(dispatcher) {
                translationRepository.downloadTranslation(this, translationInfo)
            }
}
