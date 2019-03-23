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

package me.xizzhu.android.joshua.settings

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat
import me.xizzhu.android.joshua.R
import me.xizzhu.android.joshua.core.Settings
import me.xizzhu.android.joshua.settings.widgets.SettingButton
import me.xizzhu.android.joshua.ui.DialogHelper
import me.xizzhu.android.joshua.utils.BaseActivity
import me.xizzhu.android.joshua.utils.MVPView
import javax.inject.Inject

interface SettingsView : MVPView {
    fun onVersionLoaded(version: String)

    fun onSettingsLoaded(settings: Settings)

    fun onSettingsLoadFailed()

    fun onSettingsUpdateFailed(settingsToUpdate: Settings)
}

class SettingsActivity : BaseActivity(), SettingsView {
    @Inject
    lateinit var presenter: SettingsPresenter

    private lateinit var keepScreenOn: SwitchCompat
    private lateinit var version: SettingButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        version = findViewById(R.id.version)
        keepScreenOn = findViewById(R.id.keep_screen_on)
        keepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            presenter.setKeepScreenOn(isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onVersionLoaded(version: String) {
        this.version.setDescription(version)
    }

    override fun onSettingsLoaded(settings: Settings) {
        keepScreenOn.isChecked = settings.keepScreenOn
        window.decorView.keepScreenOn = settings.keepScreenOn
    }

    override fun onSettingsLoadFailed() {
        DialogHelper.showDialog(this, true, R.string.dialog_load_settings_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.loadSettings()
                })
    }

    override fun onSettingsUpdateFailed(settingsToUpdate: Settings) {
        DialogHelper.showDialog(this, true, R.string.dialog_update_settings_error,
                DialogInterface.OnClickListener { _, _ ->
                    presenter.saveSettings(settingsToUpdate)
                })
    }
}
