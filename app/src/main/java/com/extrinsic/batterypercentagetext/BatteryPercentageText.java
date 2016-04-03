package com.extrinsic.batterypercentagetext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_SETTING_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_BATTERY_LOCK_SCREEN_SETTING;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_BATTERY_STATUS_BAR_SETTING;

public class BatteryPercentageText implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static boolean mBatteryCharging;
    private static boolean mLockScreenSettingEnabled;
    private static TextView mPercentLockScreenTextView;
    private static TextView mPercentStatusBarTextView;
    private static boolean mStatusBarSettingEnabled;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences mSharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        mSharedPreferences.makeWorldReadable();
        mLockScreenSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_LOCK_SCREEN_SETTING, true);
        mStatusBarSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_STATUS_BAR_SETTING, true);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                final Class<?> phoneKeyguardStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);

                XposedBridge.hookAllConstructors(phoneKeyguardStatusBarViewClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);
                        ((Context) param.args[0]).registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                });

                XposedHelpers.findAndHookMethod(phoneKeyguardStatusBarViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(phoneKeyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mPercentLockScreenTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");

                        if (!mBatteryCharging && mLockScreenSettingEnabled) {
                            if (mPercentLockScreenTextView != null) {
                                mPercentLockScreenTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            try {
                final Class<?> phoneStatusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        ViewGroup statusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");
                        ViewGroup viewGroup = (ViewGroup) statusBarView.findViewById(context.getResources().getIdentifier("signal_cluster", "id", "com.android.systemui"));

                        if (mPercentStatusBarTextView == null) {
                            mPercentStatusBarTextView = new TextView(viewGroup.getContext());
                            mPercentStatusBarTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                            mPercentStatusBarTextView.setPadding(20, 0, 0, 0);
                            mPercentStatusBarTextView.setTextColor(Color.WHITE);
                            mPercentStatusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                            mPercentStatusBarTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));

                            if (mStatusBarSettingEnabled) {
                                mPercentStatusBarTextView.setVisibility(View.VISIBLE);
                            } else {
                                mPercentStatusBarTextView.setVisibility(View.GONE);
                            }

                            viewGroup.addView(mPercentStatusBarTextView, viewGroup.getChildCount());
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_CHANGED:
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                    mBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

                    if (mPercentStatusBarTextView != null) {
                        int level = (int) (100.0 * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                        mPercentStatusBarTextView.setText(String.format("%s%%", level));
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED)) {
                        mLockScreenSettingEnabled = intent.getBooleanExtra(EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED, false);

                        if (mLockScreenSettingEnabled) {
                            if (mPercentLockScreenTextView != null) {
                                mPercentLockScreenTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (mPercentLockScreenTextView != null) {
                                if (!mBatteryCharging) {
                                    mPercentLockScreenTextView.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED)) {
                        mStatusBarSettingEnabled = intent.getBooleanExtra(EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED, false);

                        if (mStatusBarSettingEnabled) {
                            if (mPercentStatusBarTextView != null) {
                                mPercentStatusBarTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (mPercentStatusBarTextView != null) {
                                mPercentStatusBarTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
            }
        }
    };
}
