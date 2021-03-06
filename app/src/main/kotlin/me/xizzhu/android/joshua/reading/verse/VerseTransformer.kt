/*
 * Copyright (C) 2020 Xizhi Zhu
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

package me.xizzhu.android.joshua.reading.verse

import me.xizzhu.android.joshua.core.*

fun List<Verse>.toSimpleVerseItems(bookName: String, highlights: List<Highlight>,
                                   onVerseClicked: (Verse) -> Unit, onVerseLongClicked: (Verse) -> Unit): List<SimpleVerseItem> {
    val items = ArrayList<SimpleVerseItem>(size)

    val verseIterator = iterator()
    var verse: Verse? = null
    val highlightIterator = highlights.iterator()
    var highlight: Highlight? = null
    while (verse != null || verseIterator.hasNext()) {
        verse = verse ?: verseIterator.next()

        val (nextVerse, parallel, followingEmptyVerseCount) = verseIterator.nextNonEmpty(verse)

        val verseIndex = verse.verseIndex.verseIndex
        if (highlight == null || highlight.verseIndex.verseIndex < verseIndex) {
            while (highlightIterator.hasNext()) {
                highlight = highlightIterator.next()
                if (highlight.verseIndex.verseIndex >= verseIndex) {
                    break
                }
            }
        }
        val highlightColor = highlight
                ?.let { if (it.verseIndex.verseIndex == verseIndex) it.color else Highlight.COLOR_NONE }
                ?: Highlight.COLOR_NONE

        items.add(SimpleVerseItem(verse.transform(parallel), bookName, size,
                followingEmptyVerseCount, highlightColor, onVerseClicked, onVerseLongClicked))

        verse = nextVerse
    }

    return items
}

// skips the empty verses, and concatenates the parallels
private fun Iterator<Verse>.nextNonEmpty(current: Verse): Triple<Verse?, Array<StringBuilder>, Int> {
    val parallel = Array(current.parallel.size) { StringBuilder() }.append(current.parallel)

    var nextVerse: Verse? = null
    while (hasNext()) {
        nextVerse = next()
        if (nextVerse.text.text.isEmpty()) {
            parallel.append(nextVerse.parallel)
            nextVerse = null
        } else {
            break
        }
    }

    val followingEmptyVerseCount = nextVerse
            ?.let { it.verseIndex.verseIndex - 1 - current.verseIndex.verseIndex }
            ?: 0

    return Triple(nextVerse, parallel, followingEmptyVerseCount)
}

private fun Array<StringBuilder>.append(texts: List<Verse.Text>): Array<StringBuilder> {
    texts.forEachIndexed { index, text ->
        with(get(index)) {
            if (isNotEmpty()) append(' ')
            append(text.text)
        }
    }
    return this
}

private fun Verse.transform(concatenatedParallel: Array<StringBuilder>): Verse {
    if (parallel.isEmpty() || concatenatedParallel.isEmpty()) return this

    val parallelTexts = ArrayList<Verse.Text>(concatenatedParallel.size)
    parallel.forEachIndexed { index, text ->
        parallelTexts.add(Verse.Text(text.translationShortName, concatenatedParallel[index].toString()))
    }
    return copy(parallel = parallelTexts)
}

fun List<Verse>.toVerseItems(bookName: String, bookmarks: List<Bookmark>,
                             highlights: List<Highlight>, notes: List<Note>,
                             onVerseClicked: (Verse) -> Unit, onVerseLongClicked: (Verse) -> Unit,
                             onBookmarkClicked: (VerseIndex, Boolean) -> Unit,
                             onHighlightClicked: (VerseIndex, Int) -> Unit,
                             onNoteClicked: (VerseIndex) -> Unit): List<VerseItem> {
    val items = ArrayList<VerseItem>(size)

    val verseIterator = iterator()
    var verse: Verse? = null
    val bookmarkIterator = bookmarks.iterator()
    var bookmark: Bookmark? = null
    val highlightIterator = highlights.iterator()
    var highlight: Highlight? = null
    val noteIterator = notes.iterator()
    var note: Note? = null
    while (verse != null || verseIterator.hasNext()) {
        verse = verse ?: verseIterator.next()

        val (nextVerse, parallel, followingEmptyVerseCount) = verseIterator.nextNonEmpty(verse)

        val verseIndex = verse.verseIndex.verseIndex
        if (note == null || note.verseIndex.verseIndex < verseIndex) {
            while (noteIterator.hasNext()) {
                note = noteIterator.next()
                if (note.verseIndex.verseIndex >= verseIndex) {
                    break
                }
            }
        }
        val hasNote = note?.let { it.verseIndex.verseIndex == verseIndex } ?: false

        if (highlight == null || highlight.verseIndex.verseIndex < verseIndex) {
            while (highlightIterator.hasNext()) {
                highlight = highlightIterator.next()
                if (highlight.verseIndex.verseIndex >= verseIndex) {
                    break
                }
            }
        }
        val highlightColor = highlight
                ?.let { if (it.verseIndex.verseIndex == verseIndex) it.color else Highlight.COLOR_NONE }
                ?: Highlight.COLOR_NONE

        if (bookmark == null || bookmark.verseIndex.verseIndex < verseIndex) {
            while (bookmarkIterator.hasNext()) {
                bookmark = bookmarkIterator.next()
                if (bookmark.verseIndex.verseIndex >= verseIndex) {
                    break
                }
            }
        }
        val hasBookmark = bookmark?.let { it.verseIndex.verseIndex == verseIndex }
                ?: false

        items.add(VerseItem(verse.transform(parallel), bookName, followingEmptyVerseCount,
                hasNote, highlightColor, hasBookmark, onVerseClicked, onVerseLongClicked,
                onNoteClicked, onHighlightClicked, onBookmarkClicked))

        verse = nextVerse
    }

    return items
}

fun Collection<Verse>.toStringForSharing(bookName: String): String {
    val stringBuilder = StringBuilder()
    sortedBy { verse ->
        val verseIndex = verse.verseIndex
        verseIndex.bookIndex * 100000 + verseIndex.chapterIndex * 1000 + verseIndex.verseIndex
    }.forEach { verse -> stringBuilder.append(verse, bookName) }
    return stringBuilder.toString()
}

private fun StringBuilder.append(verse: Verse, bookName: String): StringBuilder {
    if (isNotEmpty()) append('\n')

    if (verse.parallel.isEmpty()) {
        // format: <book name> <chapter index>:<verse index> <text>
        append(bookName).append(' ')
                .append(verse.verseIndex.chapterIndex + 1).append(':').append(verse.verseIndex.verseIndex + 1).append(' ')
                .append(verse.text.text)
    } else {
        // format:
        // <book name> <chapter verseIndex>:<verse verseIndex>
        // <primary translation>: <verse text>
        // <parallel translation 1>: <verse text>
        // <parallel translation 2>: <verse text>
        append(bookName).append(' ')
                .append(verse.verseIndex.chapterIndex + 1).append(':').append(verse.verseIndex.verseIndex + 1).append('\n')
                .append(verse.text.translationShortName).append(": ").append(verse.text.text).append('\n')
        verse.parallel.forEach { text -> append(text.translationShortName).append(": ").append(text.text).append('\n') }
        setLength(length - 1) // remove the appended space
    }
    return this
}

fun Verse.toStringForSharing(bookName: String): String = StringBuilder().append(this, bookName).toString()
