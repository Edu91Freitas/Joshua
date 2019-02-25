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

package me.xizzhu.android.joshua.core.repository

import androidx.annotation.WorkerThread
import me.xizzhu.android.joshua.core.TranslationInfo
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex

interface LocalStorage {
    @WorkerThread
    fun readCurrentVerseIndex(): VerseIndex

    @WorkerThread
    fun saveCurrentVerseIndex(verseIndex: VerseIndex)

    @WorkerThread
    fun readCurrentTranslation(): String

    @WorkerThread
    fun saveCurrentTranslation(translationShortName: String)

    @WorkerThread
    fun readTranslations(): List<TranslationInfo>

    @WorkerThread
    fun replaceTranslations(translations: List<TranslationInfo>)

    @WorkerThread
    fun readBookNames(translationShortName: String): List<String>

    @WorkerThread
    fun readVerses(translationShortName: String, bookIndex: Int, chapterIndex: Int): List<Verse>

    @WorkerThread
    fun search(translationShortName: String, query: String): List<Verse>

    @WorkerThread
    fun saveTranslation(translation: Translation)
}
