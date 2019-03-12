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

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.ui.fadeIn
import me.xizzhu.android.joshua.utils.MVPView
import java.lang.StringBuilder

interface SearchResultView : MVPView {
    fun onSearchStarted()

    fun onSearchCompleted()

    fun onSearchResultUpdated(verses: List<Verse>)

    fun onVerseSelectionFailed(verseToSelect: VerseIndex)
}

class SearchResultListView : RecyclerView, SearchResultView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var presenter: SearchResultPresenter

    private val listener = object : OnSearchResultClickedListener {
        override fun onSearchResultClicked(verse: Verse) {
            presenter.selectVerse(verse.verseIndex)
        }
    }
    private val adapter: SearchResultListAdapter = SearchResultListAdapter(context, listener)

    init {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        setAdapter(adapter)
    }

    fun setPresenter(presenter: SearchResultPresenter) {
        this.presenter = presenter
    }

    override fun onSearchStarted() {
        visibility = GONE
    }

    override fun onSearchCompleted() {
        fadeIn()
    }

    override fun onSearchResultUpdated(verses: List<Verse>) {
        adapter.setSearchResult(verses)
    }

    override fun onVerseSelectionFailed(verseToSelect: VerseIndex) {
        DialogHelper.showDialog(context, true, R.string.dialog_verse_selection_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.selectVerse(verseToSelect)
                })
    }
}

private interface OnSearchResultClickedListener {
    fun onSearchResultClicked(verse: Verse)
}

private class SearchResultListAdapter(context: Context, private val listener: OnSearchResultClickedListener)
    : RecyclerView.Adapter<SearchResultViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val searchResult: ArrayList<Verse> = ArrayList()

    fun setSearchResult(verses: List<Verse>) {
        searchResult.clear()
        searchResult.addAll(verses)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = searchResult.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder =
            SearchResultViewHolder(inflater, parent, listener)

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResult[position])
    }
}

private class SearchResultViewHolder(inflater: LayoutInflater, parent: ViewGroup, private val listener: OnSearchResultClickedListener)
    : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_search_result, parent, false)),
        View.OnClickListener {
    private val stringBuilder = StringBuilder()
    private val text = itemView as TextView

    private var currentVerse: Verse? = null

    init {
        itemView.setOnClickListener(this)
    }

    fun bind(verse: Verse) {
        currentVerse = verse

        stringBuilder.setLength(0)
        stringBuilder.append(verse.text.bookName).append(' ')
                .append(verse.verseIndex.chapterIndex + 1).append(':').append(verse.verseIndex.verseIndex + 1)
                .append('\n').append(verse.text.text)
        text.text = stringBuilder.toString()
    }

    override fun onClick(v: View) {
        if (currentVerse != null) {
            listener.onSearchResultClicked(currentVerse!!)
        }
    }
}
