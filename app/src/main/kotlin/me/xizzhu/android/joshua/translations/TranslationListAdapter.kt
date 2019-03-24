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

package me.xizzhu.android.joshua.translations

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.core.TranslationInfo
import me.xizzhu.android.joshua.ui.getPrimaryTextColor
import me.xizzhu.android.joshua.ui.getSecondaryTextColor
import java.util.ArrayList

private class AvailableTranslationTitleViewHolder(inflater: LayoutInflater, parent: ViewGroup,
                                                  resources: Resources, settings: Settings)
    : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_available_translation_title, parent, false)) {
    private val title: TextView = itemView.findViewById(R.id.title)

    init {
        title.setTextColor(settings.getSecondaryTextColor(resources))
    }
}

private class TranslationInfoViewHolder(private val listener: TranslationListAdapter.Listener,
                                        inflater: LayoutInflater, parent: ViewGroup,
                                        resources: Resources, settings: Settings)
    : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_translation, parent, false)),
        View.OnClickListener, View.OnLongClickListener {
    private val textView = itemView as TextView
    private var translationInfo: TranslationInfo? = null

    init {
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)

        textView.setTextColor(settings.getPrimaryTextColor(resources))
    }

    fun bind(translationInfo: TranslationInfo, currentTranslation: Boolean) {
        this.translationInfo = translationInfo

        textView.text = translationInfo.name
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                if (currentTranslation) R.drawable.ic_check else 0, 0)
    }

    override fun onClick(v: View) {
        val t = translationInfo ?: return
        listener.onTranslationClicked(t)
    }

    override fun onLongClick(v: View): Boolean {
        val t = translationInfo ?: return false
        listener.onTranslationLongClicked(t)
        return true
    }
}

class TranslationListAdapter(context: Context, private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val ITEM_AVAILABLE_TRANSLATIONS_TITLE = 0
        private const val ITEM_TRANSLATION_INFO = 1
    }

    interface Listener {
        fun onTranslationClicked(translationInfo: TranslationInfo)

        fun onTranslationLongClicked(translationInfo: TranslationInfo)
    }

    private val resources = context.resources
    private val inflater = LayoutInflater.from(context)

    private val downloadedTranslations = ArrayList<TranslationInfo>()
    private val availableTranslations = ArrayList<TranslationInfo>()
    private var currentTranslation: String = ""
    private var settings: Settings? = null

    fun setTranslations(downloadedTranslations: List<TranslationInfo>,
                        availableTranslations: List<TranslationInfo>, currentTranslation: String) {
        this.downloadedTranslations.clear()
        this.downloadedTranslations.addAll(downloadedTranslations)
        this.availableTranslations.clear()
        this.availableTranslations.addAll(availableTranslations)
        this.currentTranslation = currentTranslation
        notifyDataSetChanged()
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                ITEM_AVAILABLE_TRANSLATIONS_TITLE -> AvailableTranslationTitleViewHolder(inflater, parent, resources, settings!!)
                ITEM_TRANSLATION_INFO -> TranslationInfoViewHolder(listener, inflater, parent, resources, settings!!)
                else -> throw IllegalArgumentException("Unsupported view type: $viewType")
            }

    override fun getItemCount(): Int =
            when {
                settings == null -> 0
                availableTranslations.size == 0 -> downloadedTranslations.size
                else -> downloadedTranslations.size + 1 + availableTranslations.size
            }

    override fun getItemViewType(position: Int): Int =
            if (availableTranslations.size > 0 && position == downloadedTranslations.size) {
                ITEM_AVAILABLE_TRANSLATIONS_TITLE
            } else {
                ITEM_TRANSLATION_INFO
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < downloadedTranslations.size) {
            val translation = downloadedTranslations[position]
            (holder as TranslationInfoViewHolder).bind(translation, translation.shortName == currentTranslation)
            return
        }

        var index = position - downloadedTranslations.size
        if (availableTranslations.size > 0) {
            --index
        }
        if (index >= 0) {
            (holder as TranslationInfoViewHolder).bind(availableTranslations[index], false)
        }
    }
}
