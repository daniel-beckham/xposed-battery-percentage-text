package com.extrinsic.batterypercentagetext;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getFragmentManager().findFragmentByTag("settings") == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment(), "settings")
                    .commit();
        }
    }
}
