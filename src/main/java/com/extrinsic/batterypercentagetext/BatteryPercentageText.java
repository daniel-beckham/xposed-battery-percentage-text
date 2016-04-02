package com.extrinsic.batterypercentagetext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_BATTERY_LOCK_SCREEN_SETTING;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_SETTING_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_BATTERY_STATUS_BAR_SETTING;

public class BatteryPercentageText implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static boolean mBatteryCharging;
    private static boolean mBroadcastReceiverRegistered;
    private static boolean mLockScreenSettingEnabled;
    private static TextView mPercentLockScreenTextView;
    private static TextView mPercentStatusBarTextView;
    private static boolean mStatusBarSettingEnabled;
    private static XSharedPreferences mSharedPreferences;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mSharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        mSharedPreferences.makeWorldReadable();
        mLockScreenSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_LOCK_SCREEN_SETTING, true);
        mStatusBarSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_STATUS_BAR_SETTING, true);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                final Class<?> phoneKeyguardStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(phoneKeyguardStatusBarViewClass, "onBatteryLevelChanged", int.class, boolean.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mBatteryCharging = (boolean) XposedHelpers.getObjectField(param.thisObject, "mBatteryCharging");
                        if (!mBatteryCharging && mLockScreenSettingEnabled) {
                            XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(phoneKeyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mBatteryCharging = (boolean) XposedHelpers.getObjectField(param.thisObject, "mBatteryCharging");
                        mPercentLockScreenTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        if (!mBatteryCharging && mLockScreenSettingEnabled) {
                            mPercentLockScreenTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                final Class<?> phoneStatusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        ViewGroup statusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");
                        ViewGroup viewGroup = (ViewGroup) statusBarView.findViewById(context.getResources().getIdentifier("signal_cluster", "id", "com.android.systemui"));
                        final int identifier = context.getResources().getIdentifier("percentage", "id", "com.android.systemui");
                        if (identifier != 0) {
                            View view = viewGroup.findViewById(identifier);
                            if (view != null && view instanceof TextView) {
                                mPercentStatusBarTextView = (TextView) view;
                                mPercentStatusBarTextView.setTag("percentage");
                            }
                        }
                        if (mPercentStatusBarTextView == null) {
                            mPercentStatusBarTextView = new TextView(viewGroup.getContext());
                            mPercentStatusBarTextView.setTag("percentage");
                            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                            mPercentStatusBarTextView.setLayoutParams(lParams);
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
                        if (!mBroadcastReceiverRegistered) {
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                            intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                            intentFilter.addAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);
                            viewGroup.getContext().registerReceiver(mBroadcastReceiver, intentFilter);
                            mBroadcastReceiverRegistered = true;
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
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (mPercentStatusBarTextView != null) {
                    int level = (int) (100.0 * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                    String percentage = level + "%";
                    mPercentStatusBarTextView.setText(percentage);
                }
            } else if (action.equals(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED)) {
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
            } else if (action.equals(ACTION_PREF_STATUS_BAR_SETTING_CHANGED)) {
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
            }
        }
    };
}
