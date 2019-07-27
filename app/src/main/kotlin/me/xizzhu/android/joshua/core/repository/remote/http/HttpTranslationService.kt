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

package me.xizzhu.android.joshua.core.repository.remote.http

import android.util.JsonReader
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import me.xizzhu.android.joshua.core.repository.remote.RemoteTranslation
import me.xizzhu.android.joshua.core.repository.remote.RemoteTranslationInfo
import me.xizzhu.android.joshua.core.repository.remote.RemoteTranslationService
import me.xizzhu.android.logger.Log
import java.io.*
import java.net.URL
import java.util.zip.ZipInputStream

open class HttpTranslationService : RemoteTranslationService {
    companion object {
        private const val TIMEOUT_IN_MILLISECONDS = 30000 // 30 seconds
        private const val BASE_URL = "https://xizzhu.me/bible/download"

        private val TAG: String = HttpTranslationService::class.java.simpleName
    }

    override suspend fun fetchTranslations(): List<RemoteTranslationInfo> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Start fetching translation list")
        val translations = JsonReader(BufferedReader(InputStreamReader(getInputStream("list.json"), "UTF-8")))
                .use { reader -> reader.readListJson() }
        Log.i(TAG, "Translation list downloaded")
        return@withContext translations
    }

    @VisibleForTesting
    open fun getInputStream(relativeUrl: String): InputStream =
            URL("$BASE_URL/$relativeUrl")
                    .openConnection()
                    .apply {
                        connectTimeout = TIMEOUT_IN_MILLISECONDS
                        readTimeout = TIMEOUT_IN_MILLISECONDS
                    }.getInputStream()

    override suspend fun fetchTranslation(channel: SendChannel<Int>, translationInfo: RemoteTranslationInfo): RemoteTranslation = withContext(Dispatchers.IO) {
        Log.i(TAG, "Start fetching translation - ${translationInfo.shortName}")

        lateinit var bookNamesShortNamesPair: Pair<List<String>, List<String>>
        val verses = HashMap<Pair<Int, Int>, List<String>>()
        ZipInputStream(BufferedInputStream(getInputStream("${translationInfo.shortName}.zip")))
                .use { zipInputStream ->
                    val buffer = ByteArray(4096)
                    val os = ByteArrayOutputStream()
                    var entryName = ""
                    var downloaded = 0
                    var progress = -1
                    while (zipInputStream.nextEntry?.also { entryName = it.name } != null) {
                        os.reset()
                        while (zipInputStream.read(buffer).also {
                                    if (it > 0) os.write(buffer, 0, it)
                                } != -1);
                        val jsonReader = JsonReader(StringReader(os.toString("UTF-8")))

                        if (entryName == "books.json") {
                            bookNamesShortNamesPair = jsonReader.readBooksJson()
                        } else {
                            val (bookIndex, chapterIndex) = entryName.substring(0, entryName.length - 5).split("-")
                            verses[Pair(bookIndex.toInt(), chapterIndex.toInt())] = jsonReader.readChapterJson()
                        }

                        // only emits if the progress is actually changed
                        val currentProgress = ++downloaded / 12
                        if (currentProgress > progress) {
                            progress = currentProgress
                            channel.send(progress)
                        }
                    }

                    Log.i(TAG, "Translation downloaded")
                }

        return@withContext RemoteTranslation(translationInfo, bookNamesShortNamesPair.first, bookNamesShortNamesPair.second, verses)
    }
}
