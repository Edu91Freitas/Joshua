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

package me.xizzhu.android.joshua.core.repository.local.android

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import me.xizzhu.android.joshua.core.*
import java.lang.StringBuilder

class AndroidDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "DATABASE_JOSHUA"
        const val DATABASE_VERSION = 1
    }

    val bookNamesDao = BookNamesDao(this)
    val metadataDao = MetadataDao(this)
    val readingProgressDao = ReadingProgressDao(this)
    val translationDao = TranslationDao(this)
    val translationInfoDao = TranslationInfoDao(this)

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            BookNamesDao.createTable(db)
            MetadataDao.createTable(db)
            ReadingProgressDao.createTable(db)
            TranslationInfoDao.createTable(db)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // do nothing
    }
}

class BookNamesDao(private val sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val TABLE_BOOK_NAMES = "bookNames"
        private const val INDEX_BOOK_NAMES = "bookNamesIndex"
        private const val COLUMN_TRANSLATION_SHORT_NAME = "translationShortName"
        private const val COLUMN_BOOK_INDEX = "bookIndex"
        private const val COLUMN_BOOK_NAME = "bookName"

        @WorkerThread
        fun createTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_BOOK_NAMES (" +
                    "$COLUMN_TRANSLATION_SHORT_NAME TEXT NOT NULL, " +
                    "$COLUMN_BOOK_INDEX INTEGER NOT NULL, " +
                    "$COLUMN_BOOK_NAME TEXT NOT NULL, " +
                    "PRIMARY KEY ($COLUMN_TRANSLATION_SHORT_NAME, $COLUMN_BOOK_INDEX));")
            db.execSQL("CREATE INDEX $INDEX_BOOK_NAMES ON $TABLE_BOOK_NAMES ($COLUMN_TRANSLATION_SHORT_NAME);")
        }
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun read(translationShortName: String): List<String> {
        var cursor: Cursor? = null
        try {
            cursor = db.query(TABLE_BOOK_NAMES, arrayOf(COLUMN_BOOK_NAME),
                    "$COLUMN_TRANSLATION_SHORT_NAME = ?", arrayOf(translationShortName), null, null,
                    "$COLUMN_BOOK_INDEX ASC")
            val bookNames = ArrayList<String>(Bible.BOOK_COUNT)
            while (cursor.moveToNext()) {
                bookNames.add(cursor.getString(0))
            }
            return bookNames
        } finally {
            cursor?.close()
        }
    }

    @WorkerThread
    fun save(translationShortName: String, bookNames: List<String>) {
        val values = ContentValues(3)
        values.put(COLUMN_TRANSLATION_SHORT_NAME, translationShortName)
        for ((bookIndex, bookName) in bookNames.withIndex()) {
            values.put(COLUMN_BOOK_INDEX, bookIndex)
            values.put(COLUMN_BOOK_NAME, bookName)
            db.insertWithOnConflict(TABLE_BOOK_NAMES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    @WorkerThread
    fun remove(translationShortName: String) {
        db.delete(TABLE_BOOK_NAMES, "$COLUMN_TRANSLATION_SHORT_NAME = ?", arrayOf(translationShortName))
    }
}

class MetadataDao(private val sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val TABLE_METADATA = "metadata"
        private const val COLUMN_KEY = "key"
        private const val COLUMN_VALUE = "value"

        const val KEY_CURRENT_TRANSLATION = "currentTranslation"
        const val KEY_CURRENT_BOOK_INDEX = "currentBookIndex"
        const val KEY_CURRENT_CHAPTER_INDEX = "currentChapterIndex"
        const val KEY_CURRENT_VERSE_INDEX = "currentVerseIndex"
        const val KEY_CONTINUOUS_READING_DAYS = "continuousReadingDays"
        const val COLUMN_LAST_READING_TIMESTAMP = "lastReadingTimestamp"

        @WorkerThread
        fun createTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_METADATA (" +
                    "$COLUMN_KEY TEXT PRIMARY KEY, $COLUMN_VALUE TEXT NOT NULL);")
        }
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun read(key: String, defaultValue: String): String {
        var cursor: Cursor? = null
        try {
            cursor = db.query(TABLE_METADATA, arrayOf(COLUMN_VALUE),
                    "$COLUMN_KEY = ?", arrayOf(key), null, null, null)
            return if (cursor.count > 0 && cursor.moveToNext()) {
                cursor.getString(0)
            } else {
                defaultValue
            }
        } finally {
            cursor?.close()
        }
    }

    @WorkerThread
    fun read(keys: List<Pair<String, String>>): List<String> {
        val results = ArrayList<String>(keys.size)
        db.beginTransaction()
        try {
            for (key in keys) {
                results.add(read(key.first, key.second))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return results
    }

    @WorkerThread
    fun save(key: String, value: String) {
        val values = ContentValues(2)
        values.put(COLUMN_KEY, key)
        values.put(COLUMN_VALUE, value)
        db.insertWithOnConflict(TABLE_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @WorkerThread
    fun save(entries: List<Pair<String, String>>) {
        db.beginTransaction()
        try {
            val values = ContentValues(2)
            for (entry in entries) {
                values.put(COLUMN_KEY, entry.first)
                values.put(COLUMN_VALUE, entry.second)
                db.insertWithOnConflict(TABLE_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

class ReadingProgressDao(private val sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val TABLE_READING_PROGRESS = "readingProgress"
        private const val COLUMN_BOOK_INDEX = "bookIndex"
        private const val COLUMN_CHAPTER_INDEX = "chapterIndex"
        private const val COLUMN_READ_COUNT = "readCount"
        private const val COLUMN_LAST_READING_TIMESTAMP = "lastReadingTimestamp"

        @WorkerThread
        fun createTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_READING_PROGRESS (" +
                    "$COLUMN_BOOK_INDEX INTEGER NOT NULL, $COLUMN_CHAPTER_INDEX INTEGER NOT NULL, " +
                    "$COLUMN_LAST_READING_TIMESTAMP INTEGER NOT NULL, $COLUMN_READ_COUNT INTEGER NOT NULL, " +
                    "PRIMARY KEY ($COLUMN_BOOK_INDEX, $COLUMN_CHAPTER_INDEX));")
        }
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun save(chapterReadingStatus: ReadingProgress.ChapterReadingStatus) {
        val values = ContentValues(4)
        values.put(COLUMN_BOOK_INDEX, chapterReadingStatus.bookIndex)
        values.put(COLUMN_CHAPTER_INDEX, chapterReadingStatus.chapterIndex)
        values.put(COLUMN_READ_COUNT, chapterReadingStatus.readCount)
        values.put(COLUMN_LAST_READING_TIMESTAMP, chapterReadingStatus.lastReadingTimestamp)
        db.insertWithOnConflict(TABLE_READING_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @WorkerThread
    fun read(bookIndex: Int, chapterIndex: Int): ReadingProgress.ChapterReadingStatus {
        var cursor: Cursor? = null
        try {
            cursor = db.query(TABLE_READING_PROGRESS, arrayOf(COLUMN_READ_COUNT, COLUMN_LAST_READING_TIMESTAMP),
                    "$COLUMN_BOOK_INDEX = ? AND $COLUMN_CHAPTER_INDEX = ?",
                    arrayOf(bookIndex.toString(), chapterIndex.toString()), null, null, null)
            val readCount: Int
            val lastReadingTimestamp: Long
            if (cursor.count > 0 && cursor.moveToNext()) {
                readCount = cursor.getInt(cursor.getColumnIndex(COLUMN_READ_COUNT))
                lastReadingTimestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_READING_TIMESTAMP))
            } else {
                readCount = 0
                lastReadingTimestamp = 0L
            }
            return ReadingProgress.ChapterReadingStatus(bookIndex, chapterIndex, readCount, lastReadingTimestamp)
        } finally {
            cursor?.close()
        }
    }
}

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

class TranslationInfoDao(private val sqliteHelper: SQLiteOpenHelper) {
    companion object {
        private const val TABLE_TRANSLATION_INFO = "translationInfo"
        private const val COLUMN_SHORT_NAME = "shortName"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_LANGUAGE = "language"
        private const val COLUMN_SIZE = "size"
        private const val COLUMN_DOWNLOADED = "downloaded"

        @WorkerThread
        fun createTable(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_TRANSLATION_INFO (" +
                    "$COLUMN_SHORT_NAME TEXT PRIMARY KEY, $COLUMN_NAME TEXT NOT NULL, " +
                    " $COLUMN_LANGUAGE TEXT NOT NULL, $COLUMN_SIZE INTEGER NOT NULL, " +
                    " $COLUMN_DOWNLOADED INTEGER NOT NULL);")
        }
    }

    private val db by lazy { sqliteHelper.writableDatabase }

    @WorkerThread
    fun read(): List<TranslationInfo> {
        var cursor: Cursor? = null
        try {
            cursor = db.query(TABLE_TRANSLATION_INFO, null, null, null, null, null, null, null)
            val count = cursor.count
            return if (count == 0) {
                emptyList()
            } else {
                val shortName = cursor.getColumnIndex(COLUMN_SHORT_NAME)
                val name = cursor.getColumnIndex(COLUMN_NAME)
                val language = cursor.getColumnIndex(COLUMN_LANGUAGE)
                val size = cursor.getColumnIndex(COLUMN_SIZE)
                val downloaded = cursor.getColumnIndex(COLUMN_DOWNLOADED)
                val translations = ArrayList<TranslationInfo>(count)
                while (cursor.moveToNext()) {
                    translations.add(TranslationInfo(cursor.getString(shortName),
                            cursor.getString(name), cursor.getString(language),
                            cursor.getLong(size), cursor.getInt(downloaded) == 1))
                }
                translations
            }
        } finally {
            cursor?.close()
        }
    }

    @WorkerThread
    fun replace(translations: List<TranslationInfo>) {
        db.beginTransaction()
        try {
            db.delete(TABLE_TRANSLATION_INFO, null, null)

            val values = ContentValues(5)
            for (t in translations) {
                t.saveTo(values)
                db.insertWithOnConflict(TABLE_TRANSLATION_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun TranslationInfo.saveTo(`out`: ContentValues) {
        `out`.put(COLUMN_SHORT_NAME, shortName)
        `out`.put(COLUMN_NAME, name)
        `out`.put(COLUMN_LANGUAGE, language)
        `out`.put(COLUMN_SIZE, size)
        `out`.put(COLUMN_DOWNLOADED, if (downloaded) 1 else 0)
    }

    @WorkerThread
    fun save(translation: TranslationInfo) {
        val values = ContentValues(5)
        translation.saveTo(values)
        db.insertWithOnConflict(TABLE_TRANSLATION_INFO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
