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

import android.os.Bundle
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.annotated.bookmarks.list.BookmarksPresenter
import me.xizzhu.android.joshua.annotated.AnnotatedVerseListView
import me.xizzhu.android.joshua.annotated.AnnotatedVersesToolbar
import me.xizzhu.android.joshua.annotated.AnnotatedVersesToolbarPresenter
import me.xizzhu.android.joshua.ui.bindView
import me.xizzhu.android.joshua.utils.activities.BaseLoadingSpinnerActivity
import me.xizzhu.android.joshua.utils.activities.BaseSettingsInteractor
import javax.inject.Inject

class BookmarksActivity : BaseLoadingSpinnerActivity() {
    @Inject
    lateinit var bookmarksInteractor: BookmarksInteractor

    @Inject
    lateinit var toolbarPresenter: AnnotatedVersesToolbarPresenter

    @Inject
    lateinit var bookmarksPresenter: BookmarksPresenter

    private val toolbar: AnnotatedVersesToolbar by bindView(R.id.toolbar)
    private val bookmarksListView: AnnotatedVerseListView by bindView(R.id.verse_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_annotated)
        bookmarksListView.setPresenter(bookmarksPresenter)
        toolbar.setPresenter(toolbarPresenter)
        toolbar.setTitle(R.string.title_bookmarks)
    }

    override fun onStart() {
        super.onStart()

        toolbarPresenter.attachView(toolbar)
        bookmarksPresenter.attachView(bookmarksListView)
    }

    override fun onStop() {
        toolbarPresenter.detachView()
        bookmarksPresenter.detachView()

        super.onStop()
    }

    override fun getBaseSettingsInteractor(): BaseSettingsInteractor = bookmarksInteractor
}
