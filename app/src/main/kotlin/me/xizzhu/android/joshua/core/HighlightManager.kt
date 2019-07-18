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

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import me.xizzhu.android.joshua.core.repository.HighlightRepository
import me.xizzhu.android.logger.Log

data class Highlight(val verseIndex: VerseIndex, @ColorInt val color: Int, val timestamp: Long) {
    companion object {
        const val COLOR_NONE = Color.TRANSPARENT
        const val COLOR_YELLOW = Color.YELLOW
        const val COLOR_PINK = 0xFFFFC0CB.toInt()
        const val COLOR_PURPLE = 0xFFFF00FF.toInt()
        const val COLOR_GREEN = Color.GREEN
        const val COLOR_BLUE = Color.BLUE
        val AVAILABLE_COLORS = arrayOf(COLOR_NONE, COLOR_YELLOW, COLOR_PINK, COLOR_PURPLE, COLOR_GREEN, COLOR_BLUE)
    }

    fun isValid(): Boolean = timestamp > 0L
}

class HighlightManager(private val highlightRepository: HighlightRepository) {
    companion object {
        private val TAG = HighlightManager::class.java.simpleName
    }

    private val sortOrder: BroadcastChannel<Int> = ConflatedBroadcastChannel()

    suspend fun observeSortOrder(): ReceiveChannel<Int> {
        return sortOrder.openSubscription().apply {
            if (isEmpty) {
                try {
                    sortOrder.send(highlightRepository.readSortOrder())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize bookmark sort order", e)
                    sortOrder.send(Constants.DEFAULT_SORT_ORDER)
                }
            }
        }
    }

    suspend fun saveSortOrder(@Constants.SortOrder sortOrder: Int) {
        highlightRepository.saveSortOrder(sortOrder)
        this.sortOrder.send(sortOrder)
    }

    suspend fun read(bookIndex: Int, chapterIndex: Int): List<Highlight> =
            highlightRepository.read(bookIndex, chapterIndex)

    suspend fun read(verseIndex: VerseIndex): Highlight = highlightRepository.read(verseIndex)

    suspend fun save(highlight: Highlight) {
        highlightRepository.save(highlight)
    }

    suspend fun remove(verseIndex: VerseIndex) {
        highlightRepository.remove(verseIndex)
    }
}
