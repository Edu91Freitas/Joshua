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

package me.xizzhu.android.joshua.progress

import me.xizzhu.android.joshua.core.Bible
import me.xizzhu.android.joshua.core.ReadingProgress
import me.xizzhu.android.joshua.tests.BaseUnitTest
import org.junit.Test
import kotlin.test.assertEquals

class ReadingProgressForDisplayTest : BaseUnitTest() {
    @Test
    fun testToReadingProgressForDisplay() {
        val readingProgress = ReadingProgress(1, 23456L,
                listOf(ReadingProgress.ChapterReadingStatus(0, 1, 1, 500L, 23456L),
                        ReadingProgress.ChapterReadingStatus(0, 2, 2, 600L, 23457L),
                        ReadingProgress.ChapterReadingStatus(2, 0, 3, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(7, 0, 1, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(7, 1, 2, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(7, 2, 3, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(7, 3, 4, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(63, 0, 1, 700L, 23458L),
                        ReadingProgress.ChapterReadingStatus(64, 0, 1, 700L, 23458L)))
        val actual = readingProgress.toReadingProgressForDisplay(Array(Bible.BOOK_COUNT) { "" }.toList())

        assertEquals(1, actual.continuousReadingDays)
        assertEquals(9, actual.chaptersRead)
        assertEquals(3, actual.finishedBooks)
        assertEquals(1, actual.finishedOldTestament)
        assertEquals(2, actual.finishedNewTestament)
        assertEquals(Bible.BOOK_COUNT, actual.bookReadingStatus.size)
        for ((i, bookReadingStatus) in actual.bookReadingStatus.withIndex()) {
            when (i) {
                0 -> {
                    assertEquals(2, bookReadingStatus.chaptersRead)
                }
                2 -> {
                    assertEquals(1, bookReadingStatus.chaptersRead)
                }
                7 -> {
                    assertEquals(4, bookReadingStatus.chaptersRead)
                }
                63 -> {
                    assertEquals(1, bookReadingStatus.chaptersRead)
                }
                64 -> {
                    assertEquals(1, bookReadingStatus.chaptersRead)
                }
                else -> {
                    assertEquals(0, bookReadingStatus.chaptersRead)
                }
            }
        }
    }
}
