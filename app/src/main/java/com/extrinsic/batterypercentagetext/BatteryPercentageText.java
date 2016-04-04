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

import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_FONT_SIZE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_FONT_SIZE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_SIZE_NORMAL;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_SIZE_SMALL;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_POSITION_LEFT;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_POSITION_RIGHT;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR_POSITION;

public class BatteryPercentageText implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private boolean batteryCharging;
    private boolean deviceRunningCyanogenMod;

    private boolean lockScreenEnabled;
    private int lockScreenFontSize;
    private int lockScreenPosition;
    private boolean notificationShadeHeaderEnabled;
    private int notificationShadeHeaderFontSize;
    private int notificationShadeHeaderPosition;
    private boolean statusBarEnabled;
    private int statusBarFontSize;
    private int statusBarPosition;

    private Context keyguardStatusBarViewClassContext;

    private TextView lockScreenNativeTextView;
    private TextView lockScreenTextView;
    private ViewGroup lockScreenViewGroup;
    private TextView notificationShadeHeaderNativeTextView;
    private TextView notificationShadeHeaderTextView;
    private ViewGroup notificationShadeHeaderViewGroup;
    private TextView statusBarTextView;
    private ViewGroup statusBarViewGroup;

    final private int TEXT_VIEW_TYPE_LOCK_SCREEN = 0;
    final private int TEXT_VIEW_TYPE_NOTIFICATION_SHADE_HEADER = 1;
    final private int TEXT_VIEW_TYPE_STATUS_BAR = 2;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences sharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        sharedPreferences.makeWorldReadable();

        lockScreenEnabled = sharedPreferences.getBoolean(PREF_LOCK_SCREEN, true);
        notificationShadeHeaderEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SHADE_HEADER, true);
        statusBarEnabled = sharedPreferences.getBoolean(PREF_STATUS_BAR, true);

        try {
            lockScreenFontSize = Integer.parseInt(sharedPreferences.getString(PREF_LOCK_SCREEN_FONT_SIZE, ""));
            lockScreenPosition = Integer.parseInt(sharedPreferences.getString(PREF_LOCK_SCREEN_POSITION, ""));
            notificationShadeHeaderFontSize = Integer.parseInt(sharedPreferences.getString(PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE, ""));
            notificationShadeHeaderPosition = Integer.parseInt(sharedPreferences.getString(PREF_NOTIFICATION_SHADE_HEADER_POSITION, ""));
            statusBarFontSize = Integer.parseInt(sharedPreferences.getString(PREF_STATUS_BAR_FONT_SIZE, ""));
            statusBarPosition = Integer.parseInt(sharedPreferences.getString(PREF_STATUS_BAR_POSITION, ""));
        } catch (NumberFormatException e) {
            lockScreenFontSize = PREF_FONT_SIZE_NORMAL;
            lockScreenPosition = PREF_POSITION_LEFT;
            notificationShadeHeaderFontSize = PREF_FONT_SIZE_NORMAL;
            notificationShadeHeaderPosition = PREF_POSITION_LEFT;
            statusBarFontSize = PREF_FONT_SIZE_NORMAL;
            statusBarPosition = PREF_POSITION_LEFT;
        }
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
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_FONT_SIZE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_FONT_SIZE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_POSITION_CHANGED);

                        keyguardStatusBarViewClassContext = (Context) param.args[0];
                        keyguardStatusBarViewClassContext.registerReceiver(mBroadcastReceiver, intentFilter);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        lockScreenNativeTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        lockScreenViewGroup = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(keyguardStatusBarViewClassContext.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));

                        createTextView(TEXT_VIEW_TYPE_LOCK_SCREEN, lockScreenFontSize, lockScreenPosition);
                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (lockScreenEnabled) {
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

                        createTextView(TEXT_VIEW_TYPE_NOTIFICATION_SHADE_HEADER, notificationShadeHeaderFontSize, notificationShadeHeaderPosition);
                        XposedHelpers.callMethod(param.thisObject, "updateVisibilities");
                    }
                });

                XposedHelpers.findAndHookMethod(statusBarHeaderViewClass, "updateVisibilities", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (notificationShadeHeaderEnabled) {
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

                        createTextView(TEXT_VIEW_TYPE_STATUS_BAR, statusBarFontSize, statusBarPosition);

                        if (statusBarEnabled) {
                            if (statusBarTextView != null) {
                                statusBarTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            // Alpha channel change implementation
            try {
                // This class has a method that changes alpha channels for elements in the status
                // bar. We'll hook it so that our injected text will receive those changes as well.
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
                // This class should exist on Lollipop and above. If, for some reason, it doesn't,
                // then we'll just ignore the exception anyway since it's not really crucial to the
                // operation of the module.
            }

            // Color change implementation
            try {
                // This class was added in Marshmallow and contains a method that changes the
                // colors of status bar elements. We'll hook this as well so that our injected text
                // can change colors when when necessary.
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
                // If this class doesn't exist, then the device is probably running Lollipop. We'll
                // just ignore the exception.
            }

            // CyanogenMod fix implementation
            try {
                // This is a non-standard class found in CyanogenMod. If it exists, then we'll try
                // to hook one of its methods to hide the existing battery percentage text.
                final Class<?> batteryLevelTextViewClass = XposedHelpers.findClass("com.android.systemui.BatteryLevelTextView", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(batteryLevelTextViewClass, "updateVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mRequestedVisibility", View.GONE);
                    }
                });

                deviceRunningCyanogenMod = true;
            } catch (Throwable t) {
                // If this class doesn't exist, then the device isn't running CyanogenMod (or a ROM
                // containing CyanogenMod code). The exception can be safely ignored in that case.
                deviceRunningCyanogenMod = false;
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
                case ACTION_PREF_LOCK_SCREEN_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_LOCK_SCREEN_ENABLED)) {
                        lockScreenEnabled = intent.getBooleanExtra(EXTRA_PREF_LOCK_SCREEN_ENABLED, false);

                        if (lockScreenEnabled) {
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
                case ACTION_PREF_LOCK_SCREEN_FONT_SIZE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_LOCK_SCREEN_FONT_SIZE)) {
                        lockScreenFontSize = intent.getIntExtra(EXTRA_PREF_LOCK_SCREEN_FONT_SIZE, PREF_FONT_SIZE_NORMAL);

                        if (lockScreenEnabled) {
                            createTextView(TEXT_VIEW_TYPE_LOCK_SCREEN, lockScreenFontSize, lockScreenPosition);
                        }
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_LOCK_SCREEN_POSITION)) {
                        lockScreenPosition = intent.getIntExtra(EXTRA_PREF_LOCK_SCREEN_POSITION, PREF_POSITION_LEFT);

                        if (lockScreenEnabled) {
                            createTextView(TEXT_VIEW_TYPE_LOCK_SCREEN, lockScreenFontSize, lockScreenPosition);
                        }
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_ENABLED)) {
                        notificationShadeHeaderEnabled = intent.getBooleanExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_ENABLED, false);

                        if (notificationShadeHeaderEnabled) {
                            if (notificationShadeHeaderNativeTextView != null) {
                                notificationShadeHeaderNativeTextView.setVisibility(View.GONE);
                            }

                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (notificationShadeHeaderNativeTextView != null) {
                                notificationShadeHeaderNativeTextView.setVisibility(View.VISIBLE);
                            }

                            if (notificationShadeHeaderTextView != null) {
                                notificationShadeHeaderTextView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE)) {
                        notificationShadeHeaderFontSize = intent.getIntExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE, PREF_FONT_SIZE_NORMAL);

                        if (notificationShadeHeaderEnabled) {
                            createTextView(TEXT_VIEW_TYPE_NOTIFICATION_SHADE_HEADER, notificationShadeHeaderFontSize, notificationShadeHeaderPosition);
                        }
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION)) {
                        notificationShadeHeaderPosition = intent.getIntExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION, PREF_POSITION_LEFT);

                        if (notificationShadeHeaderEnabled) {
                            createTextView(TEXT_VIEW_TYPE_NOTIFICATION_SHADE_HEADER, notificationShadeHeaderFontSize, notificationShadeHeaderPosition);
                        }
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_STATUS_BAR_ENABLED)) {
                        statusBarEnabled = intent.getBooleanExtra(EXTRA_PREF_STATUS_BAR_ENABLED, false);

                        if (statusBarEnabled) {
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
                case ACTION_PREF_STATUS_BAR_FONT_SIZE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_STATUS_BAR_FONT_SIZE)) {
                        statusBarFontSize = intent.getIntExtra(EXTRA_PREF_STATUS_BAR_FONT_SIZE, PREF_FONT_SIZE_NORMAL);

                        if (statusBarEnabled) {
                            createTextView(TEXT_VIEW_TYPE_STATUS_BAR, statusBarFontSize, statusBarPosition);
                        }
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_STATUS_BAR_POSITION)) {
                        statusBarPosition = intent.getIntExtra(EXTRA_PREF_STATUS_BAR_POSITION, PREF_POSITION_LEFT);

                        if (statusBarEnabled) {
                            createTextView(TEXT_VIEW_TYPE_STATUS_BAR, statusBarFontSize, statusBarPosition);
                        }
                    }
                    break;
            }
        }
    };

    private void createTextView(int type, int fontSize, int position) {
        switch (type) {
            case TEXT_VIEW_TYPE_LOCK_SCREEN:
                if (lockScreenViewGroup != null) {
                    if (lockScreenTextView != null) {
                        if (lockScreenTextView.getParent() != null) {
                            ((ViewGroup) lockScreenTextView.getParent()).removeView(lockScreenTextView);
                        }
                    } else {
                        lockScreenTextView = new TextView(lockScreenViewGroup.getContext());
                    }

                    lockScreenTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    lockScreenTextView.setTextColor(Color.WHITE);

                    if (fontSize == PREF_FONT_SIZE_SMALL) {
                        lockScreenTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (fontSize == PREF_FONT_SIZE_NORMAL) {
                        lockScreenTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        lockScreenTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (position == PREF_POSITION_LEFT) {
                        if (!deviceRunningCyanogenMod) {
                            lockScreenTextView.setPadding(0, 0, 20, 0);
                        } else {
                            lockScreenTextView.setPadding(20, 0, 0, 0);
                        }

                        final int childCount = lockScreenViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (lockScreenViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                lockScreenViewGroup.addView(lockScreenTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (position == PREF_POSITION_RIGHT) {
                        lockScreenTextView.setPadding(20, 0, 0, 0);
                        lockScreenViewGroup.addView(lockScreenTextView, lockScreenViewGroup.getChildCount());
                    }

                    if (lockScreenEnabled) {
                        lockScreenTextView.setVisibility(View.VISIBLE);
                    } else {
                        lockScreenTextView.setVisibility(View.GONE);
                    }
                }
                break;
            case TEXT_VIEW_TYPE_NOTIFICATION_SHADE_HEADER:
                if (notificationShadeHeaderViewGroup != null) {
                    if (notificationShadeHeaderTextView != null) {
                        if (notificationShadeHeaderTextView.getParent() != null) {
                            ((ViewGroup) notificationShadeHeaderTextView.getParent()).removeView(notificationShadeHeaderTextView);
                        }
                    } else {
                        notificationShadeHeaderTextView = new TextView(notificationShadeHeaderViewGroup.getContext());
                    }

                    notificationShadeHeaderTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    notificationShadeHeaderTextView.setTextColor(Color.WHITE);

                    if (fontSize == PREF_FONT_SIZE_SMALL) {
                        notificationShadeHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (fontSize == PREF_FONT_SIZE_NORMAL) {
                        notificationShadeHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        notificationShadeHeaderTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (position == PREF_POSITION_LEFT) {
                        if (!deviceRunningCyanogenMod) {
                            notificationShadeHeaderTextView.setPadding(0, 0, 20, 0);
                        } else {
                            notificationShadeHeaderTextView.setPadding(20, 0, 0, 0);
                        }

                        final int childCount = notificationShadeHeaderViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (notificationShadeHeaderViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                notificationShadeHeaderViewGroup.addView(notificationShadeHeaderTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (position == PREF_POSITION_RIGHT) {
                        notificationShadeHeaderTextView.setPadding(20, 0, 0, 0);
                        notificationShadeHeaderViewGroup.addView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup.getChildCount());
                    }

                    if (notificationShadeHeaderEnabled) {
                        notificationShadeHeaderTextView.setVisibility(View.VISIBLE);
                    } else {
                        notificationShadeHeaderTextView.setVisibility(View.GONE);
                    }
                }
                break;
            case TEXT_VIEW_TYPE_STATUS_BAR:
                if (statusBarViewGroup != null) {
                    if (statusBarTextView != null) {
                        if (statusBarTextView.getParent() != null) {
                            ((ViewGroup) statusBarTextView.getParent()).removeView(statusBarTextView);
                        }
                    } else {
                        statusBarTextView = new TextView(statusBarViewGroup.getContext());
                    }

                    statusBarTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    statusBarTextView.setTextColor(Color.WHITE);

                    if (fontSize == PREF_FONT_SIZE_SMALL) {
                        statusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    } else if (fontSize == PREF_FONT_SIZE_NORMAL) {
                        statusBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        statusBarTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    }

                    if (position == PREF_POSITION_LEFT) {
                        if (!deviceRunningCyanogenMod) {
                            statusBarTextView.setPadding(0, 0, 20, 0);
                        } else {
                            statusBarTextView.setPadding(20, 0, 0, 0);
                        }

                        final int childCount = statusBarViewGroup.getChildCount();

                        for (int i = 0; i < childCount; i++) {
                            if (statusBarViewGroup.getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                                statusBarViewGroup.addView(statusBarTextView, childCount - (childCount - i));
                                break;
                            }
                        }
                    } else if (position == PREF_POSITION_RIGHT) {
                        statusBarTextView.setPadding(20, 0, 0, 0);
                        statusBarViewGroup.addView(statusBarTextView, statusBarViewGroup.getChildCount());
                    }

                    if (statusBarEnabled) {
                        statusBarTextView.setVisibility(View.VISIBLE);
                    } else {
                        statusBarTextView.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }
}
