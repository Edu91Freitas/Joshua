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

package me.xizzhu.android.joshua.annotated.bookmarks.list

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.annotated.AnnotatedVersesInteractor
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesPresenter
import me.xizzhu.android.joshua.annotated.bookmarks.BookmarksActivity
import me.xizzhu.android.joshua.core.Bookmark
import me.xizzhu.android.joshua.core.Constants
import me.xizzhu.android.joshua.ui.recyclerview.BaseItem

class BookmarksListPresenter(bookmarksActivity: BookmarksActivity,
                             navigator: Navigator,
                             bookmarksListInteractor: AnnotatedVersesInteractor<Bookmark>,
                             dispatcher: CoroutineDispatcher = Dispatchers.Main)
    : BaseAnnotatedVersesPresenter<Bookmark, AnnotatedVersesInteractor<Bookmark>>(
        bookmarksActivity, navigator, R.string.text_no_bookmark, bookmarksListInteractor, dispatcher) {
    override fun Bookmark.toBaseItem(bookName: String, bookShortName: String, verseText: String, @Constants.SortOrder sortOrder: Int): BaseItem =
            BookmarkItem(verseIndex, bookName, bookShortName, verseText, sortOrder, ::openVerse)
}
