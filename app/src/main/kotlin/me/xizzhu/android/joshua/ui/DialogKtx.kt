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

package me.xizzhu.android.joshua.ui

import android.app.Activity
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

fun Activity.dialog(@StringRes title: Int, items: Array<String>, selected: Int,
                    onClicked: DialogInterface.OnClickListener) {
    if (isDestroyed) return

    AlertDialog.Builder(this)
            .setCancelable(true)
            .setSingleChoiceItems(items, selected, onClicked)
            .setTitle(title)
            .show()
}

fun Activity.dialog(cancelable: Boolean, @StringRes message: Int,
                    onPositive: DialogInterface.OnClickListener, onNegative: DialogInterface.OnClickListener? = null) {
    if (isDestroyed) return

    AlertDialog.Builder(this)
            .setCancelable(cancelable)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, onPositive)
            .setNegativeButton(android.R.string.no, onNegative)
            .show()
}
