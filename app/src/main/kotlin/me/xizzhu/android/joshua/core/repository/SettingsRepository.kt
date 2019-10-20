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

import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.repository.local.LocalSettingsStorage

class SettingsRepository(private val localSettingsStorage: LocalSettingsStorage) {
    private var currentSettings: Settings? = null

    suspend fun readSettings(): Settings =
            currentSettings ?: localSettingsStorage.readSettings().apply { currentSettings = this }

    suspend fun saveSettings(settings: Settings): Settings =
            localSettingsStorage.saveSettings(settings).also { currentSettings = it }
}
