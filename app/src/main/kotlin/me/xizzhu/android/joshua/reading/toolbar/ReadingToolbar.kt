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

package me.xizzhu.android.joshua.reading.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import me.xizzhu.android.joshua.R

class ReadingToolbar : Toolbar {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setTitle(R.string.app_name)
        inflateMenu(R.menu.menu_bible_reading)
    }

    fun initialize(onParallelTranslationRequested: (String) -> Unit,
                   onParallelTranslationRemoved: (String) -> Unit,
                   onSpinnerItemSelected: (String) -> Unit) {
        with(spinner()) {
            this.adapter = TranslationSpinnerAdapter(context = context,
                    onParallelTranslationRequested = onParallelTranslationRequested,
                    onParallelTranslationRemoved = onParallelTranslationRemoved)
            this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    onSpinnerItemSelected(spinnerAdapter().getItem(position))
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // do nothing
                }
            }
        }
    }

    private fun spinner(): Spinner = menu.findItem(R.id.action_translations).actionView as Spinner

    fun setSpinnerSelection(position: Int) {
        spinner().setSelection(position)
    }

    fun setCurrentTranslation(currentTranslation: String) {
        spinnerAdapter().setCurrentTranslation(currentTranslation)
    }

    private fun spinnerAdapter(): TranslationSpinnerAdapter = spinner().adapter as TranslationSpinnerAdapter

    fun setTranslationShortNames(translationShortNames: List<String>) {
        spinnerAdapter().setTranslationShortNames(translationShortNames)
    }

    fun setParallelTranslations(parallelTranslations: List<String>) {
        spinnerAdapter().setParallelTranslations(parallelTranslations)
    }
}
