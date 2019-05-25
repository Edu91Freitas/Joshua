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

package me.xizzhu.android.joshua.bookmarks

import dagger.Module
import dagger.Provides
import me.xizzhu.android.joshua.ActivityScope
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.bookmarks.list.BookmarksPresenter
import me.xizzhu.android.joshua.bookmarks.toolbar.ToolbarPresenter
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.BookmarkManager
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.ui.LoadingSpinnerPresenter

@Module
class BookmarksModule {
    @Provides
    @ActivityScope
    fun provideBookmarksInteractor(bookmarksActivity: BookmarksActivity,
                                   bibleReadingManager: BibleReadingManager,
                                   bookmarkManager: BookmarkManager,
                                   navigator: Navigator,
                                   settingsManager: SettingsManager): BookmarksInteractor =
            BookmarksInteractor(bookmarksActivity, bibleReadingManager, bookmarkManager, navigator, settingsManager)

    @Provides
    fun provideLoadingSpinnerPresenter(bookmarksInteractor: BookmarksInteractor): LoadingSpinnerPresenter =
            LoadingSpinnerPresenter(bookmarksInteractor.observeBookmarksLoadingState())

    @Provides
    fun provideToolbarPresenter(bookmarksInteractor: BookmarksInteractor): ToolbarPresenter =
            ToolbarPresenter(bookmarksInteractor)

    @Provides
    fun provideBookmarksPresenter(bookmarksActivity: BookmarksActivity,
                                  bookmarksInteractor: BookmarksInteractor): BookmarksPresenter =
            BookmarksPresenter(bookmarksInteractor, bookmarksActivity.resources)
}
