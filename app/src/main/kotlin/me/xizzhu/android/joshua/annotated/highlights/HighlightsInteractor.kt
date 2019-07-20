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

package me.xizzhu.android.joshua.annotated.highlights

import kotlinx.coroutines.channels.ReceiveChannel
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesInteractor
import me.xizzhu.android.joshua.core.*

class HighlightsInteractor(highlightsActivity: HighlightsActivity,
                           bibleReadingManager: BibleReadingManager,
                           private val highlightsManager: HighlightManager,
                           navigator: Navigator,
                           settingsManager: SettingsManager)
    : BaseAnnotatedVersesInteractor(highlightsActivity, bibleReadingManager, navigator, settingsManager, IS_LOADING) {
    suspend fun readHighlights(@Constants.SortOrder sortOrder: Int): List<Highlight> = highlightsManager.read(sortOrder)

    override suspend fun observeSortOrder(): ReceiveChannel<Int> = highlightsManager.observeSortOrder()

    override suspend fun saveSortOrder(@Constants.SortOrder sortOrder: Int) {
        highlightsManager.saveSortOrder(sortOrder)
    }
}
