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

data class ReadingProgressForDisplay(val bookReadingStatus: List<BookReadingStatus>) {
    data class BookReadingStatus(val bookName: String, val chaptersRead: Int, val chaptersCount: Int)
}

fun ReadingProgress.toReadingProgressForDisplay(bookNames: List<String>): ReadingProgressForDisplay {
    val chaptersRead = Array(Bible.BOOK_COUNT) { 0 }
    for (chapter in chapterReadingStatus) {
        chaptersRead[chapter.bookIndex]++
    }
    val bookReadingStatus = ArrayList<ReadingProgressForDisplay.BookReadingStatus>(Bible.BOOK_COUNT)
    for ((i, readCount) in chaptersRead.withIndex()) {
        bookReadingStatus.add(ReadingProgressForDisplay.BookReadingStatus(
                bookNames[i], readCount, Bible.getChapterCount(i)))
    }
    return ReadingProgressForDisplay(bookReadingStatus)
}
