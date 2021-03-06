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

package me.xizzhu.android.joshua.strongnumber

import dagger.Module
import dagger.Provides
import me.xizzhu.android.joshua.ActivityScope
import me.xizzhu.android.joshua.Navigator
import me.xizzhu.android.joshua.core.BibleReadingManager
import me.xizzhu.android.joshua.core.SettingsManager
import me.xizzhu.android.joshua.core.StrongNumberManager
import me.xizzhu.android.joshua.infra.ui.LoadingSpinnerInteractor
import me.xizzhu.android.joshua.infra.ui.LoadingSpinnerPresenter

@Module
object StrongNumberListModule {
    @ActivityScope
    @Provides
    fun provideLoadingSpinnerInteractor(): LoadingSpinnerInteractor = LoadingSpinnerInteractor()

    @ActivityScope
    @Provides
    fun provideLoadingSpinnerPresenter(loadingSpinnerInteractor: LoadingSpinnerInteractor): LoadingSpinnerPresenter =
            LoadingSpinnerPresenter(loadingSpinnerInteractor)

    @ActivityScope
    @Provides
    fun provideStrongNumberListInteractor(strongNumberManager: StrongNumberManager,
                                          bibleReadingManager: BibleReadingManager,
                                          settingsManager: SettingsManager): StrongNumberListInteractor =
            StrongNumberListInteractor(strongNumberManager, bibleReadingManager, settingsManager)

    @ActivityScope
    @Provides
    fun provideStrongNumberListPresenter(strongNumberListActivity: StrongNumberListActivity,
                                         navigator: Navigator,
                                         strongNumberListInteractor: StrongNumberListInteractor): StrongNumberListPresenter =
            StrongNumberListPresenter(strongNumberListActivity, navigator, strongNumberListInteractor)

    @ActivityScope
    @Provides
    fun provideStrongNumberViewModel(settingsManager: SettingsManager,
                                     loadingSpinnerInteractor: LoadingSpinnerInteractor,
                                     strongNumberListInteractor: StrongNumberListInteractor): StrongNumberListViewModel =
            StrongNumberListViewModel(settingsManager, loadingSpinnerInteractor, strongNumberListInteractor)
}
