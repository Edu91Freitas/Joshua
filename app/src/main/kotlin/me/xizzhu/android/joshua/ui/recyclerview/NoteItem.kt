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

package me.xizzhu.android.joshua.ui.recyclerview

import android.content.res.Resources
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.Verse
import me.xizzhu.android.joshua.core.VerseIndex
import me.xizzhu.android.joshua.ui.getBodyTextSize
import me.xizzhu.android.joshua.ui.getCaptionTextSize
import me.xizzhu.android.joshua.ui.getPrimaryTextColor

data class NoteItem(val verseIndex: VerseIndex, val text: Verse.Text, val note: String, val timestamp: Long) : BaseItem {
    companion object {
        private val BOOK_NAME_STYLE_SPAN = StyleSpan(Typeface.BOLD)
        private val SPANNABLE_STRING_BUILDER = SpannableStringBuilder()
    }

    val textForDisplay: CharSequence by lazy {
        SPANNABLE_STRING_BUILDER.clear()
        SPANNABLE_STRING_BUILDER.clearSpans()

        // format:
        // <book name> <chapter index>:<verse index> <verse text>
        SPANNABLE_STRING_BUILDER.append(text.bookName).append(' ')
                .append((verseIndex.chapterIndex + 1).toString()).append(':').append((verseIndex.verseIndex + 1).toString())
        SPANNABLE_STRING_BUILDER.setSpan(BOOK_NAME_STYLE_SPAN, 0, SPANNABLE_STRING_BUILDER.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

        SPANNABLE_STRING_BUILDER.append(' ').append(text.text)

        return@lazy SPANNABLE_STRING_BUILDER.subSequence(0, SPANNABLE_STRING_BUILDER.length)
    }

    override fun getItemViewType(): Int = BaseItem.NOTE_ITEM
}

class NoteItemViewHolder(inflater: LayoutInflater, parent: ViewGroup, private val resources: Resources)
    : BaseViewHolder<NoteItem>(inflater.inflate(R.layout.item_note, parent, false)) {
    private val verse: TextView = itemView.findViewById(R.id.verse)
    private val text: TextView = itemView.findViewById(R.id.text)

    override fun bind(settings: Settings, item: NoteItem, payloads: List<Any>) {
        val textColor = settings.getPrimaryTextColor(resources)
        with(verse) {
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, settings.getCaptionTextSize(this@NoteItemViewHolder.resources))
            text = item.textForDisplay
        }
        with(text) {
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, settings.getBodyTextSize(this@NoteItemViewHolder.resources))
            text = item.note
        }
    }
}
