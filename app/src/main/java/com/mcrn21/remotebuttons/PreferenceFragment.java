package com.mcrn21.remotebuttons;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class PreferenceFragment  extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Common.SETTINGS_FILE);
        addPreferencesFromResource(R.xml.preferences);
    }
}
