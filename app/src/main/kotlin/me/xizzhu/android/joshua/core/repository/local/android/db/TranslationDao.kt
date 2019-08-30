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

package me.xizzhu.android.joshua.core.repository.local.android.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.logger.Log
import java.lang.StringBuilder
import kotlin.math.max

class TranslationDao(sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val COLUMN_BOOK_INDEX = "bookIndex"
        private const val COLUMN_CHAPTER_INDEX = "chapterIndex"
        private const val COLUMN_VERSE_INDEX = "verseIndex"
        private const val COLUMN_TEXT = "text"

        private val TAG: String = TranslationDao::class.java.simpleName
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun read(translationShortName: String, bookIndex: Int, chapterIndex: Int): List<Verse> {
        db.withTransaction {
            if (!hasTable(translationShortName)) return emptyList()

            query(translationShortName, arrayOf(COLUMN_TEXT),
                    "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ?", arrayOf(bookIndex.toString(), chapterIndex.toString()),
                    null, null, "$COLUMN_VERSE_INDEX ASC").use {
                val verses = ArrayList<Verse>(it.count)
                var verseIndex = 0
                while (it.moveToNext()) {
                    verses.add(Verse(VerseIndex(bookIndex, chapterIndex, verseIndex++),
                            Verse.Text(translationShortName, it.getString(0)), emptyList()))
                }
                return verses
            }
        }
    }

    @WorkerThread
    private fun SQLiteDatabase.hasTable(name: String, logIfNoTable: Boolean = true): Boolean {
        rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '$name'", null).use {
            val hasTable = it.count > 0
            if (logIfNoTable && !hasTable) {
                Log.e(TAG, "", IllegalStateException("Missing translation $name"))
            }
            return hasTable
        }
    }

    @WorkerThread
    fun read(translationShortName: String, parallelTranslations: List<String>,
             bookIndex: Int, chapterIndex: Int): List<Verse> {
        db.withTransaction {
            if (!hasTable(translationShortName)) return emptyList()

            val primaryTexts = readVerseTexts(translationShortName, bookIndex, chapterIndex)
            var versesCount = primaryTexts.size
            val parallelTexts = ArrayList<ArrayList<Verse.Text>>(parallelTranslations.size).apply {
                val countVerses: (List<Verse.Text>) -> Unit = { versesCount = max(versesCount, it.size) }
                parallelTranslations.forEach { add(readVerseTexts(it, bookIndex, chapterIndex).also(countVerses)) }
            }

            val verses = ArrayList<Verse>(versesCount)
            for (verseIndex in 0 until versesCount) {
                val primary = if (primaryTexts.size > verseIndex) primaryTexts[verseIndex] else Verse.Text(translationShortName, "")

                val parallel = ArrayList<Verse.Text>(parallelTranslations.size)
                for ((i, translation) in parallelTranslations.withIndex()) {
                    parallel.add(parallelTexts[i].let {
                        return@let if (it.size > verseIndex) it[verseIndex] else Verse.Text(translation, "")
                    })
                }

                verses.add(Verse(VerseIndex(bookIndex, chapterIndex, verseIndex), primary, parallel))
            }

            return verses
        }
    }

    @WorkerThread
    private fun SQLiteDatabase.readVerseTexts(translation: String,
                                              bookIndex: Int, chapterIndex: Int): ArrayList<Verse.Text> {
        if (!hasTable(translation)) return ArrayList()

        query(translation, arrayOf(COLUMN_TEXT),
                "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ?",
                arrayOf(bookIndex.toString(), chapterIndex.toString()),
                null, null, "$COLUMN_VERSE_INDEX ASC").use {
            val texts = ArrayList<Verse.Text>(it.count)
            while (it.moveToNext()) {
                texts.add(Verse.Text(translation, it.getString(0)))
            }
            return texts
        }
    }

    @WorkerThread
    fun read(translationShortName: String, parallelTranslations: List<String>, verseIndex: VerseIndex): Verse {
        db.withTransaction {
            if (!hasTable(translationShortName)) return Verse.INVALID

            val primaryText = readVerseText(translationShortName, verseIndex).let { text ->
                return@let if (text.isValid()) text else Verse.Text(translationShortName, "")
            }
            val parallelTexts = mutableListOf<Verse.Text>().apply {
                parallelTranslations.forEach { translation ->
                    readVerseText(translation, verseIndex).let { text ->
                        add(if (text.isValid()) text else Verse.Text(translation, ""))
                    }
                }
            }
            return Verse(verseIndex, primaryText, parallelTexts)
        }
    }

    @WorkerThread
    private fun SQLiteDatabase.readVerseText(translation: String, verseIndex: VerseIndex): Verse.Text {
        if (!hasTable(translation)) return Verse.Text.INVALID

        query(translation, arrayOf(COLUMN_TEXT),
                "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ? AND $COLUMN_VERSE_INDEX = ?",
                arrayOf(verseIndex.bookIndex.toString(), verseIndex.chapterIndex.toString(), verseIndex.verseIndex.toString()),
                null, null, null).use {
            return if (it.moveToNext()) Verse.Text(translation, it.getString(0)) else Verse.Text.INVALID
        }
    }

    @WorkerThread
    fun read(translationShortName: String, verseIndex: VerseIndex): Verse {
        if (!verseIndex.isValid()) return Verse.INVALID

        db.withTransaction {
            if (!hasTable(translationShortName)) return Verse.INVALID

            query(translationShortName, arrayOf(COLUMN_TEXT),
                    "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ? AND $COLUMN_VERSE_INDEX = ?",
                    arrayOf(verseIndex.bookIndex.toString(), verseIndex.chapterIndex.toString(), verseIndex.verseIndex.toString()),
                    null, null, null).use {
                return if (it.moveToNext()) {
                    Verse(verseIndex, Verse.Text(translationShortName, it.getString(0)), emptyList())
                } else {
                    Verse.INVALID
                }
            }
        }
    }

    @WorkerThread
    fun search(translationShortName: String, query: String): List<Verse> {
        val keywords = query.trim().let {
            if (it.isEmpty()) return emptyList()
            return@let it.replace("\\s+", " ").split(" ")
        }

        val singleSelection = "$COLUMN_TEXT LIKE ?"
        val selection = StringBuilder()
        val selectionArgs = Array(keywords.size) { "" }
        for (i in keywords.indices) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            selection.append(singleSelection)

            selectionArgs[i] = "%%${keywords[i]}%%"
        }

        db.withTransaction {
            if (!hasTable(translationShortName)) return emptyList()

            query(translationShortName,
                    arrayOf(COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX, COLUMN_TEXT),
                    selection.toString(), selectionArgs, null, null,
                    "$COLUMN_BOOK_INDEX ASC, $COLUMN_CHAPTER_INDEX ASC, $COLUMN_VERSE_INDEX ASC").use {
                val verses = ArrayList<Verse>(it.count)
                if (it.count > 0) {
                    val bookColumnIndex = it.getColumnIndex(COLUMN_BOOK_INDEX)
                    val chapterColumnIndex = it.getColumnIndex(COLUMN_CHAPTER_INDEX)
                    val verseColumnIndex = it.getColumnIndex(COLUMN_VERSE_INDEX)
                    val textColumnIndex = it.getColumnIndex(COLUMN_TEXT)
                    while (it.moveToNext()) {
                        val verseIndex = VerseIndex(it.getInt(bookColumnIndex),
                                it.getInt(chapterColumnIndex), it.getInt(verseColumnIndex))
                        verses.add(Verse(verseIndex, Verse.Text(translationShortName, it.getString(textColumnIndex)), emptyList()))
                    }
                }
                return verses
            }
        }
    }

    @WorkerThread
    fun save(translationShortName: String, verses: Map<Pair<Int, Int>, List<String>>) {
        db.withTransaction {
            if (hasTable(translationShortName, false)) {
                Log.e(TAG, "", IllegalStateException("Translation $translationShortName already installed"))
                remove(translationShortName)
            }
            execSQL("CREATE TABLE IF NOT EXISTS $translationShortName (" +
                    "$COLUMN_BOOK_INDEX INTEGER NOT NULL, $COLUMN_CHAPTER_INDEX INTEGER NOT NULL, " +
                    "$COLUMN_VERSE_INDEX INTEGER NOT NULL, $COLUMN_TEXT TEXT NOT NULL, " +
                    "PRIMARY KEY($COLUMN_BOOK_INDEX, $COLUMN_CHAPTER_INDEX, $COLUMN_VERSE_INDEX));")

            val values = ContentValues(4)
            for (entry in verses) {
                with(values) {
                    put(COLUMN_BOOK_INDEX, entry.key.first)
                    put(COLUMN_CHAPTER_INDEX, entry.key.second)
                    for ((verseIndex, verse) in entry.value.withIndex()) {
                        put(COLUMN_VERSE_INDEX, verseIndex)
                        put(COLUMN_TEXT, verse)
                        insertWithOnConflict(translationShortName, null, this, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
            }
        }
    }

    @WorkerThread
    fun remove(translationShortName: String) {
        db.execSQL("DROP TABLE IF EXISTS $translationShortName")
    }
}
