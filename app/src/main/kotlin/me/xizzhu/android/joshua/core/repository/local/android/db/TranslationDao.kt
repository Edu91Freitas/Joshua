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
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import java.lang.StringBuilder

class TranslationDao(private val sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val COLUMN_BOOK_INDEX = "bookIndex"
        private const val COLUMN_CHAPTER_INDEX = "chapterIndex"
        private const val COLUMN_VERSE_INDEX = "verseIndex"
        private const val COLUMN_TEXT = "text"
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun createTable(translationShortName: String) {
        db.execSQL("CREATE TABLE $translationShortName (" +
                "$COLUMN_BOOK_INDEX INTEGER NOT NULL, $COLUMN_CHAPTER_INDEX INTEGER NOT NULL, " +
                "$COLUMN_VERSE_INDEX INTEGER NOT NULL, $COLUMN_TEXT TEXT NOT NULL, " +
                "PRIMARY KEY($COLUMN_BOOK_INDEX, $COLUMN_CHAPTER_INDEX, $COLUMN_VERSE_INDEX));")
    }

    @WorkerThread
    fun removeTable(translationShortName: String) {
        db.execSQL("DROP TABLE IF EXISTS $translationShortName")
    }

    @WorkerThread
    fun read(translationShortName: String, bookIndex: Int, chapterIndex: Int, bookName: String): List<Verse> {
        var cursor: Cursor? = null
        db.beginTransaction()
        try {
            if (!db.hasTable(translationShortName)) {
                return emptyList()
            }

            cursor = db.query(translationShortName, arrayOf(COLUMN_TEXT),
                    "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ?", arrayOf(bookIndex.toString(), chapterIndex.toString()),
                    null, null, "$COLUMN_VERSE_INDEX ASC")
            val verses = ArrayList<Verse>(cursor.count)
            var verseIndex = 0
            while (cursor.moveToNext()) {
                verses.add(Verse(VerseIndex(bookIndex, chapterIndex, verseIndex++),
                        Verse.Text(translationShortName, bookName, cursor.getString(0))))
            }

            db.setTransactionSuccessful()
            return verses
        } finally {
            db.endTransaction()
            cursor?.close()
        }
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean {
        val cursor: Cursor = rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '$name'", null)
        return cursor.use {
            cursor.count > 0
        }
    }

    @WorkerThread
    fun search(translationShortName: String, bookNames: List<String>, query: String): List<Verse> {
        var cursor: Cursor? = null
        try {
            val keywords = query.trim().replace("\\s+", " ").split(" ")
            if (keywords.isEmpty()) {
                return emptyList()
            }

            val singleSelection = "$COLUMN_TEXT LIKE ?"
            val selection = StringBuilder()
            val selectionArgs = Array(keywords.size) { "" }
            for (i in 0 until keywords.size) {
                if (selection.isNotEmpty()) {
                    selection.append(" AND ")
                }
                selection.append(singleSelection)

                selectionArgs[i] = "%%${keywords[i]}%%"
            }

            db.beginTransaction()
            if (!db.hasTable(translationShortName)) {
                return emptyList()
            }

            cursor = db.query(translationShortName,
                    arrayOf(COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX, COLUMN_TEXT),
                    selection.toString(), selectionArgs, null, null,
                    "$COLUMN_BOOK_INDEX ASC, $COLUMN_CHAPTER_INDEX ASC, $COLUMN_VERSE_INDEX ASC")
            val verses = ArrayList<Verse>(cursor.count)
            val bookColumnIndex = cursor.getColumnIndex(COLUMN_BOOK_INDEX)
            val chapterColumnIndex = cursor.getColumnIndex(COLUMN_CHAPTER_INDEX)
            val verseColumnIndex = cursor.getColumnIndex(COLUMN_VERSE_INDEX)
            val textColumnIndex = cursor.getColumnIndex(COLUMN_TEXT)
            while (cursor.moveToNext()) {
                val verseIndex = VerseIndex(cursor.getInt(bookColumnIndex),
                        cursor.getInt(chapterColumnIndex), cursor.getInt(verseColumnIndex))
                verses.add(Verse(verseIndex, Verse.Text(translationShortName,
                        bookNames[verseIndex.bookIndex], cursor.getString(textColumnIndex))))
            }

            db.setTransactionSuccessful()
            return verses
        } finally {
            if (db.inTransaction()) {
                db.endTransaction()
            }
            cursor?.close()
        }
    }

    @WorkerThread
    fun save(translationShortName: String, verses: Map<Pair<Int, Int>, List<String>>) {
        val values = ContentValues(4)
        for (entry in verses) {
            values.put(COLUMN_BOOK_INDEX, entry.key.first)
            values.put(COLUMN_CHAPTER_INDEX, entry.key.second)
            for ((verseIndex, verse) in entry.value.withIndex()) {
                values.put(COLUMN_VERSE_INDEX, verseIndex)
                values.put(COLUMN_TEXT, verse)
                db.insertWithOnConflict(translationShortName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }
}
