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

package me.xizzhu.android.joshua.annotated.bookmarks

import me.xizzhu.android.joshua.annotated.*
import me.xizzhu.android.joshua.annotated.bookmarks.list.BookmarksPresenter
import me.xizzhu.android.joshua.utils.activities.BaseSettingsInteractor
import javax.inject.Inject

class BookmarksActivity : BaseAnnotatedVersesActivity() {
    @Inject
    lateinit var bookmarksInteractor: BookmarksInteractor

    @Inject
    lateinit var bookmarksPresenter: BookmarksPresenter

    override fun getBaseSettingsInteractor(): BaseSettingsInteractor = bookmarksInteractor

    override fun getAnnotatedVersesPresenter(): AnnotatedVersePresenter = bookmarksPresenter
}
