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
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_SETTING_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_LOCK_SCREEN_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_NOTIFICATION_SHADE_HEADER_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_STATUS_BAR_SETTING_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR;

public class BatteryPercentageText implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private boolean batteryCharging;

    private boolean lockScreenSettingEnabled;
    private boolean notificationShadeHeaderSettingEnabled;
    private boolean statusBarSettingEnabled;

    private Context keyguardStatusBarViewClassContext;
    private TextView lockScreenNativeTextView;
    private TextView lockScreenTextView;
    private ViewGroup lockScreenViewGroup;
    private TextView notificationShadeHeaderNativeTextView;
    private TextView notificationShadeHeaderTextView;
    private ViewGroup notificationShadeHeaderViewGroup;
    private TextView statusBarTextView;
    private ViewGroup statusBarViewGroup;

    private enum TextViewFontSize { SMALL, NORMAL };
    private enum TextViewPosition { LEFT, RIGHT };
    private enum TextViewType { LOCK_SCREEN, NOTIFICATION_SHADE_HEADER, STATUS_BAR };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences sharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        sharedPreferences.makeWorldReadable();

        lockScreenSettingEnabled = sharedPreferences.getBoolean(PREF_LOCK_SCREEN, true);
        notificationShadeHeaderSettingEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SHADE_HEADER, true);
        statusBarSettingEnabled = sharedPreferences.getBoolean(PREF_STATUS_BAR, true);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            // Lock screen implementation
            try {
                final Class<?> keyguardStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);

                XposedBridge.hookAllConstructors(keyguardStatusBarViewClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_SETTING_CHANGED);

                        keyguardStatusBarViewClassContext = (Context) param.args[0];
                        keyguardStatusBarViewClassContext.registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        lockScreenNativeTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        lockScreenViewGroup = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(keyguardStatusBarViewClassContext.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));

                        createTextView(TextViewType.LOCK_SCREEN, TextViewPosition.RIGHT, TextViewFontSize.NORMAL);
                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (lockScreenSettingEnabled) {
                            if (lockScreenNativeTextView != null) {
                                lockScreenNativeTextView.setVisibility(View.GONE);
                            }

                            if (lockScreenTextView != null) {
                                lockScreenTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            // Notification shade header implementation
            try {
                final Class<?> statusBarHeaderViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarHeaderView", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(statusBarHeaderViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        notificationShadeHeaderNativeTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        notificationShadeHeaderViewGroup = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(context.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));

                        createTextView(TextViewType.NOTIFICATION_SHADE_HEADER, TextViewPosition.RIGHT, TextViewFontSize.NORMAL);
                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(statusBarHeaderViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (notificationShadeHeaderSettingEnabled) {
                            if (notificationShadeHeaderNativeTextView != null) {
                                notificationShadeHeaderNativeTextView.setVisibility(View.GONE);
                            }

                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            // Status bar implementation
            try {
                final Class<?> phoneStatusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        statusBarViewGroup = (ViewGroup) ((ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView")).findViewById(context.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));

                        createTextView(TextViewType.STATUS_BAR, TextViewPosition.RIGHT, TextViewFontSize.NORMAL);

                        if (statusBarSettingEnabled) {
                            if (statusBarTextView != null) {
                                statusBarTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            try {
                // Sometimes the status bar icons will change alpha channels depending on the app
                // that is currently open. We'll hook a method in this class so that our injected
                // text will change as well.
                final Class<?> statusBarIconControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarTransitions", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(statusBarIconControllerClass, "applyMode", int.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (statusBarTextView != null) {
                            float alpha = (float) XposedHelpers.callMethod(param.thisObject, "getBatteryClockAlpha", param.args[0]);
                            statusBarTextView.setAlpha(alpha);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            try {
                // The status bar icons can also sometimes change colors. A method in this class
                // will be hooked so that our injected text will receive those changes.
                final Class<?> statusBarIconControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(statusBarIconControllerClass, "applyIconTint", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (statusBarTextView != null) {
                            int iconTint = (int) XposedHelpers.getObjectField(param.thisObject, "mIconTint");
                            statusBarTextView.setTextColor(iconTint);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            try {
                // This is a non-standard class found in CyanogenMod. If it exists, then we'll try
                // to use it to hide the existing battery percentage text.
                final Class<?> batteryLevelTextViewClass = XposedHelpers.findClass("com.android.systemui.BatteryLevelTextView", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(batteryLevelTextViewClass, "updateVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mRequestedVisibility", View.GONE);
                    }
                });
            } catch (Throwable t) {
                // It wasn't found, so we'll just ignore this exception.
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

                    if (lockScreenTextView != null) {
                        lockScreenTextView.setText(String.format("%s%%", level));
                    }

                    if (notificationShadeHeaderTextView != null) {
                        notificationShadeHeaderTextView.setText(String.format("%s%%", level));
                    }

                    if (statusBarTextView != null) {
                        statusBarTextView.setText(String.format("%s%%", level));
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_LOCK_SCREEN_SETTING_ENABLED)) {
                        lockScreenSettingEnabled = intent.getBooleanExtra(EXTRA_LOCK_SCREEN_SETTING_ENABLED, false);

                        if (lockScreenSettingEnabled) {
                            if (lockScreenNativeTextView != null) {
                                lockScreenNativeTextView.setVisibility(View.GONE);
                            }

                            if (lockScreenTextView != null) {
                                lockScreenTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (lockScreenNativeTextView != null) {
                                if (batteryCharging) {
                                    lockScreenNativeTextView.setVisibility(View.VISIBLE);
                                }
                            }

                            if (lockScreenTextView != null) {
                                lockScreenTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED:
                    if (intent.hasExtra(EXTRA_NOTIFICATION_SHADE_HEADER_ENABLED)) {
                        notificationShadeHeaderSettingEnabled = intent.getBooleanExtra(EXTRA_NOTIFICATION_SHADE_HEADER_ENABLED, false);

                        if (notificationShadeHeaderSettingEnabled) {
                            if (notificationShadeHeaderNativeTextView != null) {
                                notificationShadeHeaderNativeTextView.setVisibility(View.GONE);
                            }

                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.VISIBLE);
                            }

                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_SETTING_CHANGED:
                    if (intent.hasExtra(EXTRA_STATUS_BAR_SETTING_ENABLED)) {
                        statusBarSettingEnabled = intent.getBooleanExtra(EXTRA_STATUS_BAR_SETTING_ENABLED, false);

                        if (statusBarSettingEnabled) {
                            if (statusBarTextView != null) {
                                statusBarTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (statusBarTextView != null) {
                                statusBarTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
            }
        }
    };

    private void createTextView(TextViewType textViewType, TextViewPosition textViewPosition, TextViewFontSize textViewFontSize) {
        switch (textViewType) {
            case LOCK_SCREEN:
                if (lockScreenViewGroup != null) {
                    if (lockScreenTextView != null) {
                        if (lockScreenTextView.getParent() != null) {
                            ((ViewGroup) lockScreenTextView.getParent()).removeView(lockScreenTextView);
                        }
                    }

                    lockScreenTextView = new TextView(lockScreenViewGroup.getContext());
                    lockScreenTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    lockScreenTextView.setPadding(20, 0, 0, 0);
                    lockScreenTextView.setTextColor(Color.WHITE);

                    if (textViewFontSize == TextViewFontSize.SMALL) {
                        lockScreenTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (textViewFontSize == TextViewFontSize.NORMAL) {
                        lockScreenTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        lockScreenTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (textViewPosition == TextViewPosition.LEFT) {
                        final int childCount = lockScreenViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (lockScreenViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                lockScreenViewGroup.addView(lockScreenTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (textViewPosition == TextViewPosition.RIGHT) {
                        lockScreenViewGroup.addView(lockScreenTextView, lockScreenViewGroup.getChildCount());
                    }
                }
                break;
            case NOTIFICATION_SHADE_HEADER:
                if (notificationShadeHeaderViewGroup != null) {
                    if (notificationShadeHeaderTextView != null) {
                        if (notificationShadeHeaderTextView.getParent() != null) {
                            ((ViewGroup) notificationShadeHeaderTextView.getParent()).removeView(notificationShadeHeaderTextView);
                        }
                    }

                    notificationShadeHeaderTextView = new TextView(notificationShadeHeaderViewGroup.getContext());
                    notificationShadeHeaderTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    notificationShadeHeaderTextView.setPadding(20, 0, 0, 0);
                    notificationShadeHeaderTextView.setTextColor(Color.WHITE);

                    if (textViewFontSize == TextViewFontSize.SMALL) {
                        notificationShadeHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (textViewFontSize == TextViewFontSize.NORMAL) {
                        notificationShadeHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        notificationShadeHeaderTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (textViewPosition == TextViewPosition.LEFT) {
                        final int childCount = notificationShadeHeaderViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (notificationShadeHeaderViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                notificationShadeHeaderViewGroup.addView(notificationShadeHeaderTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (textViewPosition == TextViewPosition.RIGHT) {
                        notificationShadeHeaderViewGroup.addView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup.getChildCount());
                    }
                }
                break;
            case STATUS_BAR:
                if (statusBarViewGroup != null) {
                    if (statusBarTextView != null) {
                        if (statusBarTextView.getParent() != null) {
                            ((ViewGroup) statusBarTextView.getParent()).removeView(statusBarTextView);
                        }
                    }

                    statusBarTextView = new TextView(statusBarViewGroup.getContext());
                    statusBarTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    statusBarTextView.setPadding(20, 0, 0, 0);
                    statusBarTextView.setTextColor(Color.WHITE);

                    if (textViewFontSize == TextViewFontSize.SMALL) {
                        statusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (textViewFontSize == TextViewFontSize.NORMAL) {
                        statusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        statusBarTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (textViewPosition == TextViewPosition.LEFT) {
                        final int childCount = statusBarViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (statusBarViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                statusBarViewGroup.addView(statusBarTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (textViewPosition == TextViewPosition.RIGHT) {
                        statusBarViewGroup.addView(statusBarTextView, statusBarViewGroup.getChildCount());
                    }
                }
                break;
        }
    }
}
