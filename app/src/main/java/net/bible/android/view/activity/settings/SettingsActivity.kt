/*
 * Copyright (c) 2020 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package net.bible.android.view.activity.settings

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import net.bible.android.activity.R
import net.bible.android.view.activity.base.ActivityBase
import net.bible.android.view.activity.base.Dialogs
import net.bible.service.common.CommonUtils
import net.bible.service.device.ScreenSettings.autoModeAvailable
import net.bible.service.device.ScreenSettings.systemModeAvailable
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.FeatureType

class SettingsActivity: ActivityBase() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.settings_activity)
		super.buildActivityComponent().inject(this)

		supportFragmentManager
			.beginTransaction()
			.replace(R.id.settings_container, SettingsFragment())
			.commit()
	}
}

class SettingsFragment : PreferenceFragmentCompat() {
	override fun onDisplayPreferenceDialog(preference: Preference?) {
		if(parentFragmentManager.findFragmentByTag("customTag") != null)
			return

        super.onDisplayPreferenceDialog(preference)
	}

    private fun setupDictionary(pref: ListPreference, type: FeatureType): Boolean {
        val dicts = Books.installed().books.filter { it.hasFeature(type) }

        if(dicts.isEmpty()) {
            pref.isVisible = false
            return false
        } else {
            val names = dicts.map { it.name }.toTypedArray()
            val initials = dicts.map { it.initials }.toTypedArray()
            pref.entries = names
            pref.entryValues = initials
            return true
        }
    }

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        //If no light sensor exists switch to old boolean check box
        // see here for method: http://stackoverflow.com/questions/4081533/how-to-remove-android-preferences-from-the-screen
        val nightModePref = preferenceScreen.findPreference<ListPreference>("night_mode_pref3") as ListPreference
        if (systemModeAvailable) {
            if (autoModeAvailable) {
                nightModePref.setEntries(R.array.prefs_night_mode_descriptions_system_auto_manual)
                nightModePref.setEntryValues(R.array.prefs_night_mode_values_system_auto_manual)
                nightModePref.setDefaultValue(R.string.prefs_night_mode_manual)
            } else {
                nightModePref.setEntries(R.array.prefs_night_mode_descriptions_system_manual)
                nightModePref.setEntryValues(R.array.prefs_night_mode_values_system_manual)
                nightModePref.setDefaultValue(R.string.prefs_night_mode_manual)
            }
        } else {
            if (!autoModeAvailable) {
                nightModePref.isVisible = false
            }
        }
        val showErrorBox = preferenceScreen.findPreference<ListPreference>("show_errorbox") as Preference
        showErrorBox.isVisible = CommonUtils.isBeta
        var showDictionaries = false
        val greekStrongs = preferenceScreen.findPreference<ListPreference>("strongs_greek_dictionary") as ListPreference
        showDictionaries = showDictionaries || setupDictionary(greekStrongs, FeatureType.GREEK_DEFINITIONS)
        val hebrewStrongs = preferenceScreen.findPreference<ListPreference>("strongs_hebrew_dictionary") as ListPreference
        showDictionaries = showDictionaries || setupDictionary(hebrewStrongs, FeatureType.HEBREW_DEFINITIONS)
        val greekMorph = preferenceScreen.findPreference<ListPreference>("robinson_greek_morphology") as ListPreference
        showDictionaries = showDictionaries || setupDictionary(greekMorph, FeatureType.GREEK_PARSE)
        val dictCategory = preferenceScreen.findPreference<PreferenceCategory>("dictionaries_category") as PreferenceCategory
        dictCategory.isVisible = showDictionaries

        preferenceScreen.findPreference<ListPreference>("toolbar_button_actions")?.apply {
                if (value.isNullOrBlank())
                    value = "default"
            }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pref = preferenceScreen.findPreference<ListPreference>("request_sdcard_permission_pref") as Preference
            pref.isVisible = false
        }
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}


