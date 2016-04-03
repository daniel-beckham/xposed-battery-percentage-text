package com.extrinsic.batterypercentagetext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    public static final String ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED = "batterypercentagetext.intent.action.ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED";
    public static final String EXTRA_LOCK_SCREEN_SETTING_ENABLED = "lockScreenSettingEnabled";
    public static final String PREF_LOCK_SCREEN = "pref_lock_screen";

    public static final String ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED = "batterypercentagetext.intent.action.ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED";
    public static final String EXTRA_NOTIFICATION_SHADE_HEADER_ENABLED = "notificationShadeHeaderSettingEnabled";
    public static final String PREF_NOTIFICATION_SHADE_HEADER = "pref_notification_shade_header";

    public static final String ACTION_PREF_STATUS_BAR_SETTING_CHANGED = "batterypercentagetext.intent.action.ACTION_PREF_STATUS_BAR_SETTING_CHANGED";
    public static final String EXTRA_STATUS_BAR_SETTING_ENABLED = "statusBarSettingEnabled";
    public static final String PREF_STATUS_BAR = "pref_status_bar";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

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
            case PREF_LOCK_SCREEN:
                intent.setAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                intent.putExtra(EXTRA_LOCK_SCREEN_SETTING_ENABLED, sharedPreferences.getBoolean(PREF_LOCK_SCREEN, false));
                break;
            case PREF_NOTIFICATION_SHADE_HEADER:
                intent.setAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED);
                intent.putExtra(EXTRA_NOTIFICATION_SHADE_HEADER_ENABLED, sharedPreferences.getBoolean(PREF_NOTIFICATION_SHADE_HEADER, false));
                break;
            case PREF_STATUS_BAR:
                intent.setAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);
                intent.putExtra(EXTRA_STATUS_BAR_SETTING_ENABLED, sharedPreferences.getBoolean(PREF_STATUS_BAR, false));
                break;
        }

        if (intent.getAction() != null) {
            getActivity().sendBroadcast(intent);
        }
    }
}
