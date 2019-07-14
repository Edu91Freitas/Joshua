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

package me.xizzhu.android.joshua.search.result

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.launch
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.search.SearchInteractor
import me.xizzhu.android.joshua.utils.activities.BaseSettingsPresenter
import me.xizzhu.android.logger.Log

class SearchResultPresenter(private val searchInteractor: SearchInteractor)
    : BaseSettingsPresenter<SearchResultView>(searchInteractor) {
    override fun onViewAttached() {
        super.onViewAttached()

        coroutineScope.launch(Dispatchers.Main) {
            searchInteractor.observeSearchQuery()
                    .filter { it.isNotEmpty() }
                    .consumeEach { query -> search(query) }
        }
    }

    fun search(query: String) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                searchInteractor.notifySearchStarted()
                view?.onSearchStarted()

                view?.onSearchResultUpdated(searchInteractor.search(query).toSearchResult(
                        query, searchInteractor.readBookNames(searchInteractor.readCurrentTranslation()),
                        this@SearchResultPresenter::selectVerse))
                view?.onSearchCompleted()
            } catch (e: Exception) {
                Log.e(tag, "Failed to search Bible verses", e)
                view?.onSearchFailed(query)
            } finally {
                searchInteractor.notifySearchFinished()
            }
        }
    }

    fun selectVerse(verseToSelect: VerseIndex) {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                searchInteractor.selectVerse(verseToSelect)
                searchInteractor.openReading()
            } catch (e: Exception) {
                Log.e(tag, "Failed to select verse and open reading activity", e)
                view?.onVerseSelectionFailed(verseToSelect)
            }
        }
    }
}
