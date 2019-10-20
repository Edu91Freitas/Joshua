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

package me.xizzhu.android.joshua.annotated.highlights

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.xizzhu.android.joshua.annotated.BaseAnnotatedVersesViewModel
import me.xizzhu.android.joshua.annotated.highlights.list.HighlightsListInteractor
import me.xizzhu.android.joshua.annotated.toolbar.AnnotatedVersesToolbarInteractor
import me.xizzhu.android.joshua.core.Highlight
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.infra.ui.LoadingSpinnerInteractor

class HighlightsViewModel(settingsManager: SettingsManager,
                          annotatedVersesToolbarInteractor: AnnotatedVersesToolbarInteractor,
                          loadingSpinnerInteractor: LoadingSpinnerInteractor,
                          highlightsListInteractor: HighlightsListInteractor,
                          dispatcher: CoroutineDispatcher = Dispatchers.Default)
    : BaseAnnotatedVersesViewModel<Highlight>(settingsManager, annotatedVersesToolbarInteractor, loadingSpinnerInteractor, highlightsListInteractor, dispatcher)
