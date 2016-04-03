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
    private boolean batteryCharging;
    private boolean deviceRunningCyanogenMod;
    private Context keyguardStatusBarViewClassContext;
    private boolean lockScreenSettingEnabled;
    private TextView percentLockScreenTextView;
    private TextView percentStatusBarTextView;
    private boolean statusBarSettingEnabled;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences mSharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        mSharedPreferences.makeWorldReadable();
        lockScreenSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_LOCK_SCREEN_SETTING, true);
        statusBarSettingEnabled = mSharedPreferences.getBoolean(PREF_BATTERY_STATUS_BAR_SETTING, true);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                final Class<?> keyguardStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);

                try {
                    if (XposedHelpers.findClass("com.android.systemui.BatteryLevelTextView", lpparam.classLoader) != null) {
                        deviceRunningCyanogenMod = true;
                    }
                } catch (Throwable t) {
                    deviceRunningCyanogenMod = false;
                }

                XposedBridge.hookAllConstructors(keyguardStatusBarViewClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);

                        keyguardStatusBarViewClassContext = (Context) param.args[0];
                        keyguardStatusBarViewClassContext.registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (deviceRunningCyanogenMod) {
                            // CyanogenMod uses a separate class (BatteryLevelTextView) for managing
                            // the battery percentage text. Since this text can be disabled in the
                            // CyanogenMod settings, we will just inject our own here instead.
                            ViewGroup viewGroup = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(keyguardStatusBarViewClassContext.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));
                            if (percentLockScreenTextView == null) {
                                percentLockScreenTextView = new TextView(viewGroup.getContext());
                                percentLockScreenTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                                percentLockScreenTextView.setPadding(20, 0, 0, 0);
                                percentLockScreenTextView.setTextColor(Color.WHITE);
                                percentLockScreenTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

                                if (lockScreenSettingEnabled) {
                                    percentLockScreenTextView.setVisibility(View.VISIBLE);
                                } else {
                                    if (!batteryCharging) {
                                        percentLockScreenTextView.setVisibility(View.GONE);
                                    }
                                }

                                viewGroup.addView(percentLockScreenTextView, viewGroup.getChildCount());
                            }
                        } else {
                            // If the device is not running CyanogenMod, then we can just use the
                            // native battery percentage text that is displayed when the device is
                            // charging.
                            percentLockScreenTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        }

                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!batteryCharging && lockScreenSettingEnabled) {
                            if (percentLockScreenTextView != null) {
                                percentLockScreenTextView.setVisibility(View.VISIBLE);
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
                        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView")).findViewById(context.getResources().getIdentifier("signal_cluster", "id", "com.android.systemui"));

                        if (percentStatusBarTextView == null) {
                            percentStatusBarTextView = new TextView(viewGroup.getContext());
                            percentStatusBarTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                            percentStatusBarTextView.setPadding(20, 0, 0, 0);
                            percentStatusBarTextView.setTextColor(Color.WHITE);
                            percentStatusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                            percentStatusBarTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));

                            if (statusBarSettingEnabled) {
                                percentStatusBarTextView.setVisibility(View.VISIBLE);
                            } else {
                                percentStatusBarTextView.setVisibility(View.GONE);
                            }

                            viewGroup.addView(percentStatusBarTextView, viewGroup.getChildCount());
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
                    final int level = (int) (100.0 * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                    final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                    batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

                    if (percentLockScreenTextView != null) {
                        percentLockScreenTextView.setText(String.format("%s%%", level));
                    }

                    if (percentStatusBarTextView != null) {
                        percentStatusBarTextView.setText(String.format("%s%%", level));
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED)) {
                        lockScreenSettingEnabled = intent.getBooleanExtra(EXTRA_BATTERY_LOCK_SCREEN_SETTING_ENABLED, false);

                        if (lockScreenSettingEnabled) {
                            if (percentLockScreenTextView != null) {
                                percentLockScreenTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (percentLockScreenTextView != null) {
                                if (!batteryCharging) {
                                    percentLockScreenTextView.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED)) {
                        statusBarSettingEnabled = intent.getBooleanExtra(EXTRA_BATTERY_STATUS_BAR_SETTING_ENABLED, false);

                        if (statusBarSettingEnabled) {
                            if (percentStatusBarTextView != null) {
                                percentStatusBarTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (percentStatusBarTextView != null) {
                                percentStatusBarTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
            }
        }
    };
}
