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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.xizzhu.android.joshua.core.StrongNumber
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.core.repository.local.LocalStrongNumberStorage

class StrongNumberRepository(private val localStrongNumberStorage: LocalStrongNumberStorage) {
    suspend fun read(verseIndex: VerseIndex): List<StrongNumber> = localStrongNumberStorage.read(verseIndex)

    fun download(): Flow<Int> = flow {
        (0..101).forEach {
            emit(it)
            delay(100L)
        }
    }
}
