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

package me.xizzhu.android.joshua.bookmarks.list

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Constants
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.ui.fadeIn
import me.xizzhu.android.joshua.ui.recyclerview.*
import me.xizzhu.android.joshua.utils.activities.BaseSettingsView

interface BookmarksView : BaseSettingsView {
    fun onBookmarksLoadingStarted()

    fun onBookmarksLoadingCompleted()

    fun onBookmarksLoaded(bookmarks: List<BaseItem>)

    fun onBookmarksLoadFailed(@Constants.SortOrder sortOrder: Int)

    fun onVerseSelectionFailed(verseToSelect: VerseIndex)
}

class BookmarksListView : BaseRecyclerView, BookmarksView {
    private lateinit var presenter: BookmarksPresenter

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setPresenter(presenter: BookmarksPresenter) {
        this.presenter = presenter
    }

    override fun onBookmarksLoadingStarted() {
        visibility = GONE
    }

    override fun onBookmarksLoadingCompleted() {
        fadeIn()
    }

    override fun onBookmarksLoaded(bookmarks: List<BaseItem>) {
        setItems(bookmarks)
    }

    override fun onBookmarksLoadFailed(@Constants.SortOrder sortOrder: Int) {
        DialogHelper.showDialog(context, true, R.string.dialog_load_bookmarks_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.loadBookmarks(sortOrder)
                })
    }

    override fun onVerseSelectionFailed(verseToSelect: VerseIndex) {
        DialogHelper.showDialog(context, true, R.string.dialog_verse_selection_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.selectVerse(verseToSelect)
                })
    }
}
