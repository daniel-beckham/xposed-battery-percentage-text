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
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_FONT_STYLE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_FONT_SIZE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_FONT_STYLE_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.ACTION_PREF_STATUS_BAR_POSITION_CHANGED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_LOCK_SCREEN_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_ENABLED;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.EXTRA_PREF_STATUS_BAR_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_SIZE_LARGE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_SIZE_NORMAL;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_SIZE_SMALL;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_STYLE_BOLD;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_STYLE_BOLD_ITALIC;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_STYLE_ITALIC;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_FONT_STYLE_NORMAL;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_LOCK_SCREEN_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_NOTIFICATION_SHADE_HEADER_POSITION;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_POSITION_LEFT;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_POSITION_RIGHT;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR_FONT_SIZE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR_FONT_STYLE;
import static com.extrinsic.batterypercentagetext.SettingsFragment.PREF_STATUS_BAR_POSITION;

public class BatteryPercentageText implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private boolean batteryCharging;
    private boolean deviceRunningCyanogenMod;

    private boolean lockScreenEnabled;
    private int lockScreenFontSize;
    private int lockScreenFontStyle;
    private int lockScreenPosition;
    private boolean notificationShadeHeaderEnabled;
    private int notificationShadeHeaderFontSize;
    private int notificationShadeHeaderFontStyle;
    private int notificationShadeHeaderPosition;
    private boolean statusBarEnabled;
    private int statusBarFontSize;
    private int statusBarFontStyle;
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

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences sharedPreferences = new XSharedPreferences("com.extrinsic.batterypercentagetext");
        sharedPreferences.makeWorldReadable();

        lockScreenEnabled = sharedPreferences.getBoolean(PREF_LOCK_SCREEN, true);
        notificationShadeHeaderEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SHADE_HEADER, true);
        statusBarEnabled = sharedPreferences.getBoolean(PREF_STATUS_BAR, true);

        try {
            lockScreenFontSize = Integer.parseInt(sharedPreferences.getString(PREF_LOCK_SCREEN_FONT_SIZE, ""));
            lockScreenFontStyle = Integer.parseInt(sharedPreferences.getString(PREF_LOCK_SCREEN_FONT_STYLE, ""));
            lockScreenPosition = Integer.parseInt(sharedPreferences.getString(PREF_LOCK_SCREEN_POSITION, ""));
            notificationShadeHeaderFontSize = Integer.parseInt(sharedPreferences.getString(PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE, ""));
            notificationShadeHeaderFontStyle = Integer.parseInt(sharedPreferences.getString(PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE, ""));
            notificationShadeHeaderPosition = Integer.parseInt(sharedPreferences.getString(PREF_NOTIFICATION_SHADE_HEADER_POSITION, ""));
            statusBarFontSize = Integer.parseInt(sharedPreferences.getString(PREF_STATUS_BAR_FONT_SIZE, ""));
            statusBarFontStyle = Integer.parseInt(sharedPreferences.getString(PREF_STATUS_BAR_FONT_STYLE, ""));
            statusBarPosition = Integer.parseInt(sharedPreferences.getString(PREF_STATUS_BAR_POSITION, ""));
        } catch (NumberFormatException e) {
            lockScreenFontSize = PREF_FONT_SIZE_NORMAL;
            lockScreenFontStyle = PREF_FONT_STYLE_BOLD;
            lockScreenPosition = PREF_POSITION_LEFT;
            notificationShadeHeaderFontSize = PREF_FONT_SIZE_NORMAL;
            notificationShadeHeaderFontStyle = PREF_FONT_STYLE_BOLD;
            notificationShadeHeaderPosition = PREF_POSITION_LEFT;
            statusBarFontSize = PREF_FONT_SIZE_NORMAL;
            statusBarFontStyle = PREF_FONT_STYLE_BOLD;
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
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_FONT_STYLE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_SIZE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_FONT_SIZE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_FONT_STYLE_CHANGED);
                        intentFilter.addAction(ACTION_PREF_STATUS_BAR_POSITION_CHANGED);

                        keyguardStatusBarViewClassContext = (Context) param.args[0];
                        keyguardStatusBarViewClassContext.registerReceiver(broadcastReceiver, intentFilter);
                    }
                });

                XposedHelpers.findAndHookMethod(keyguardStatusBarViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        lockScreenNativeTextView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
                        lockScreenViewGroup = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(keyguardStatusBarViewClassContext.getResources().getIdentifier("system_icons", "id", "com.android.systemui"));
                        lockScreenTextView = new TextView(lockScreenViewGroup.getContext());
                        modifyTextView(lockScreenTextView, lockScreenViewGroup, lockScreenFontSize, lockScreenFontStyle, lockScreenPosition, lockScreenEnabled);
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
                        notificationShadeHeaderTextView = new TextView(notificationShadeHeaderViewGroup.getContext());
                        modifyTextView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup, notificationShadeHeaderFontSize, notificationShadeHeaderFontStyle, notificationShadeHeaderPosition, notificationShadeHeaderEnabled);
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
                        statusBarTextView = new TextView(statusBarViewGroup.getContext());
                        modifyTextView(statusBarTextView, statusBarViewGroup, statusBarFontSize, statusBarFontStyle, statusBarPosition, statusBarEnabled);
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
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
                        modifyTextView(lockScreenTextView, lockScreenViewGroup, lockScreenFontSize, lockScreenFontStyle, lockScreenPosition, lockScreenEnabled);
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_FONT_STYLE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_LOCK_SCREEN_FONT_STYLE)) {
                        lockScreenFontStyle = intent.getIntExtra(EXTRA_PREF_LOCK_SCREEN_FONT_STYLE, PREF_FONT_STYLE_BOLD);
                        modifyTextView(lockScreenTextView, lockScreenViewGroup, lockScreenFontSize, lockScreenFontStyle, lockScreenPosition, lockScreenEnabled);
                    }
                    break;
                case ACTION_PREF_LOCK_SCREEN_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_LOCK_SCREEN_POSITION)) {
                        lockScreenPosition = intent.getIntExtra(EXTRA_PREF_LOCK_SCREEN_POSITION, PREF_POSITION_LEFT);
                        modifyTextView(lockScreenTextView, lockScreenViewGroup, lockScreenFontSize, lockScreenFontStyle, lockScreenPosition, lockScreenEnabled);
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
                        modifyTextView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup, notificationShadeHeaderFontSize, notificationShadeHeaderFontStyle, notificationShadeHeaderPosition, notificationShadeHeaderEnabled);
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE)) {
                        notificationShadeHeaderFontStyle = intent.getIntExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_FONT_STYLE, PREF_FONT_STYLE_BOLD);
                        modifyTextView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup, notificationShadeHeaderFontSize, notificationShadeHeaderFontStyle, notificationShadeHeaderPosition, notificationShadeHeaderEnabled);
                    }
                    break;
                case ACTION_PREF_NOTIFICATION_SHADE_HEADER_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION)) {
                        notificationShadeHeaderPosition = intent.getIntExtra(EXTRA_PREF_NOTIFICATION_SHADE_HEADER_POSITION, PREF_POSITION_LEFT);
                        modifyTextView(notificationShadeHeaderTextView, notificationShadeHeaderViewGroup, notificationShadeHeaderFontSize, notificationShadeHeaderFontStyle, notificationShadeHeaderPosition, notificationShadeHeaderEnabled);
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
                        modifyTextView(statusBarTextView, statusBarViewGroup, statusBarFontSize, statusBarFontStyle, statusBarPosition, statusBarEnabled);
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_FONT_STYLE_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_STATUS_BAR_FONT_STYLE)) {
                        statusBarFontStyle = intent.getIntExtra(EXTRA_PREF_STATUS_BAR_FONT_STYLE, PREF_FONT_STYLE_BOLD);
                        modifyTextView(statusBarTextView, statusBarViewGroup, statusBarFontSize, statusBarFontStyle, statusBarPosition, statusBarEnabled);
                    }
                    break;
                case ACTION_PREF_STATUS_BAR_POSITION_CHANGED:
                    if (intent.hasExtra(EXTRA_PREF_STATUS_BAR_POSITION)) {
                        statusBarPosition = intent.getIntExtra(EXTRA_PREF_STATUS_BAR_POSITION, PREF_POSITION_LEFT);
                        modifyTextView(statusBarTextView, statusBarViewGroup, statusBarFontSize, statusBarFontStyle, statusBarPosition, statusBarEnabled);
                    }
                    break;
            }
        }
    };

    private <S, T> void modifyTextView(S textView, T viewGroup, int size, int style, int position, boolean enabled) {
        if (textView != null && viewGroup != null) {
            if (((TextView) textView).getParent() != null) {
                ((ViewGroup) ((TextView) textView).getParent()).removeView((TextView) textView);
            }

            ((TextView) textView).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            ((TextView) textView).setTextColor(Color.WHITE);

            switch (size) {
                case PREF_FONT_SIZE_SMALL:
                    ((TextView) textView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    break;
                case PREF_FONT_SIZE_NORMAL:
                    ((TextView) textView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    break;
                case PREF_FONT_SIZE_LARGE:
                    ((TextView) textView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    break;
            }

            switch (style) {
                case PREF_FONT_STYLE_NORMAL:
                    ((TextView) textView).setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                    break;
                case PREF_FONT_STYLE_ITALIC:
                    ((TextView) textView).setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                    break;
                case PREF_FONT_STYLE_BOLD:
                    ((TextView) textView).setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
                    break;
                case PREF_FONT_STYLE_BOLD_ITALIC:
                    ((TextView) textView).setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD_ITALIC));
                    break;
            }

            if (position == PREF_POSITION_LEFT) {
                if (!deviceRunningCyanogenMod) {
                    ((TextView) textView).setPadding(0, 0, 20, 0);
                } else {
                    ((TextView) textView).setPadding(20, 0, 0, 0);
                }

                final int childCount = ((ViewGroup) viewGroup).getChildCount();

                for (int i = 0; i < childCount; i++) {
                    if (((ViewGroup) viewGroup).getChildAt(i).getClass().getName().equals("com.android.systemui.BatteryMeterView")) {
                        ((ViewGroup) viewGroup).addView((TextView) textView, childCount - (childCount - i));
                        break;
                    }
                }
            } else if (position == PREF_POSITION_RIGHT) {
                ((TextView) textView).setPadding(20, 0, 0, 0);
                ((ViewGroup) viewGroup).addView((TextView) textView, ((ViewGroup) viewGroup).getChildCount());
            }

            if (enabled) {
                ((TextView) textView).setVisibility(View.VISIBLE);
            } else {
                ((TextView) textView).setVisibility(View.GONE);
            }
        }
    }
}
