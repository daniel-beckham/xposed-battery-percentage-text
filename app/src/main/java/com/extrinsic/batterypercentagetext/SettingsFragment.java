package com.extrinsic.batterypercentagetext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    public static final String ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED = "batterypercentagetext.intent.action.ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED";
    public static final String EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED = "lockScreenSettingEnabled";
    public static final String PREF_BATTERY_LOCK_SCREEN_SETTING = "pref_lock_screen_setting";
    public static final String ACTION_PREF_STATUS_BAR_SETTING_CHANGED = "batterypercentagetext.intent.action.ACTION_PREF_STATUS_BAR_SETTING_CHANGED";
    public static final String EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED = "statusBarSettingEnabled";
    public static final String PREF_BATTERY_STATUS_BAR_SETTING = "pref_status_bar_setting";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(getActivity().getApplicationContext().MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Intent intent = new Intent();

        switch (key) {
            case PREF_BATTERY_LOCK_SCREEN_SETTING:
                intent.setAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                intent.putExtra(EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED, sharedPreferences.getBoolean(PREF_BATTERY_LOCK_SCREEN_SETTING, false));
                break;
            case PREF_BATTERY_STATUS_BAR_SETTING:
                intent.setAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);
                intent.putExtra(EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED, sharedPreferences.getBoolean(PREF_BATTERY_STATUS_BAR_SETTING, false));
                break;
        }

        if (intent.getAction() != null) {
            getActivity().sendBroadcast(intent);
        }
    }
}
