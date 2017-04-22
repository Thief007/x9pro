package com.android.settings;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.usb.IUsbManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceFrameLayout.LayoutParams;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan.TextBuilder;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import com.android.internal.R;
import com.android.internal.util.UserIcons;
import com.android.settings.UserAdapter.UserDetails;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.drawable.CircleFramedDrawable;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils {
    public static final int[] BADNESS_COLORS = new int[]{0, -3917784, -1750760, -754944, -344276, -9986505, -16089278};
    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray();
    private static Signature[] sSystemSignature;

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context, PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {
        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }
        Intent intent = preference.getIntent();
        if (intent != null) {
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = (ResolveInfo) list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    preference.setIntent(new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    if ((flags & 1) != 0) {
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }
                    return true;
                }
            }
        }
        parentPreferenceGroup.removePreference(preference);
        return false;
    }

    public static boolean updateTileToSpecificActivityFromMetaDataOrRemove(Context context, DashboardTile tile) {
        Intent intent = tile.intent;
        if (intent != null) {
            List<ResolveInfo> list;
            PackageManager pm = context.getPackageManager();
            if (tile.userHandle.size() != 0) {
                list = pm.queryIntentActivitiesAsUser(intent, 128, ((UserHandle) tile.userHandle.get(0)).getIdentifier());
            } else {
                list = pm.queryIntentActivities(intent, 128);
            }
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = (ResolveInfo) list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    int icon = 0;
                    CharSequence charSequence = null;
                    CharSequence summary = null;
                    try {
                        Resources res = pm.getResourcesForApplication(resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;
                        if (!(res == null || metaData == null)) {
                            if (metaData.containsKey("com.android.settings.icon")) {
                                icon = metaData.getInt("com.android.settings.icon");
                            }
                            if (metaData.containsKey("com.android.settings.title")) {
                                charSequence = res.getString(metaData.getInt("com.android.settings.title"));
                            }
                            if (metaData.containsKey("com.android.settings.summary")) {
                                summary = res.getString(metaData.getInt("com.android.settings.summary"));
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                    if (TextUtils.isEmpty(charSequence)) {
                        charSequence = resolveInfo.loadLabel(pm).toString();
                    }
                    if (icon == 0) {
                        icon = resolveInfo.activityInfo.icon;
                    }
                    tile.iconRes = icon;
                    tile.iconPkg = resolveInfo.activityInfo.packageName;
                    tile.title = charSequence;
                    tile.summary = summary;
                    tile.intent = new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        return telephony != null ? telephony.isVoiceCapable() : false;
    }

    public static boolean isWifiOnly(Context context) {
        if (((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0)) {
            return false;
        }
        return true;
    }

    public static String getWifiIpAddresses(Context context) {
        return formatIpAddresses(((ConnectivityManager) context.getSystemService("connectivity")).getLinkProperties(1));
    }

    public static String getDefaultIpAddresses(ConnectivityManager cm) {
        return formatIpAddresses(cm.getActiveLinkProperties());
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) {
            return null;
        }
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        if (!iter.hasNext()) {
            return null;
        }
        String addresses = "";
        while (iter.hasNext()) {
            addresses = addresses + ((InetAddress) iter.next()).getHostAddress();
            if (iter.hasNext()) {
                addresses = addresses + "\n";
            }
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        if (localeStr == null) {
            return Locale.getDefault();
        }
        String[] brokenDownLocale = localeStr.split("_", 3);
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        }
        if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        }
        return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
    }

    public static String formatPercentage(long amount, long total) {
        return formatPercentage(((double) amount) / ((double) total));
    }

    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0d);
    }

    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra("present", true);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    public static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        return (level * 100) / batteryChangedIntent.getIntExtra("scale", 100);
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent) {
        Intent intent = batteryChangedIntent;
        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", 1);
        if (status == 2) {
            int resId;
            if (plugType == 1) {
                resId = R.string.battery_info_status_charging_ac;
            } else if (plugType == 2) {
                resId = R.string.battery_info_status_charging_usb;
            } else if (plugType == 4) {
                resId = R.string.battery_info_status_charging_wireless;
            } else {
                resId = R.string.battery_info_status_charging;
            }
            return res.getString(resId);
        } else if (status == 3) {
            return res.getString(R.string.battery_info_status_discharging);
        } else {
            if (status == 4) {
                return res.getString(R.string.battery_info_status_not_charging);
            }
            if (status == 5) {
                return res.getString(R.string.battery_info_status_full);
            }
            return res.getString(R.string.battery_info_status_unknown);
        }
    }

    public static void forcePrepareCustomPreferencesList(ViewGroup parent, View child, ListView list, boolean ignoreSidePadding) {
        list.setScrollBarStyle(33554432);
        list.setClipToPadding(false);
        prepareCustomPreferencesList(parent, child, list, ignoreSidePadding);
    }

    public static void prepareCustomPreferencesList(ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        if (list.getScrollBarStyle() == 33554432) {
            Resources res = list.getResources();
            int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
            int paddingBottom = res.getDimensionPixelSize(17104938);
            if (parent instanceof PreferenceFrameLayout) {
                ((LayoutParams) child.getLayoutParams()).removeBorders = true;
                int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
                list.setPaddingRelative(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
                return;
            }
            list.setPaddingRelative(paddingSide, 0, paddingSide, paddingBottom);
        }
    }

    public static void forceCustomPadding(View view, boolean additive) {
        int paddingStart;
        Resources res = view.getResources();
        int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
        if (additive) {
            paddingStart = view.getPaddingStart();
        } else {
            paddingStart = 0;
        }
        int paddingStart2 = paddingSide + paddingStart;
        if (additive) {
            paddingStart = view.getPaddingEnd();
        } else {
            paddingStart = 0;
        }
        view.setPaddingRelative(paddingStart2, 0, paddingSide + paddingStart, res.getDimensionPixelSize(17104938));
    }

    public static int getTetheringLabel(ConnectivityManager cm) {
        String[] usbRegexs = cm.getTetherableUsbRegexs();
        String[] wifiRegexs = cm.getTetherableWifiRegexs();
        String[] bluetoothRegexs = cm.getTetherableBluetoothRegexs();
        boolean usbAvailable = usbRegexs.length != 0;
        boolean wifiAvailable = wifiRegexs.length != 0;
        boolean bluetoothAvailable = bluetoothRegexs.length != 0;
        if (wifiAvailable && usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable && usbAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable) {
            return R.string.tether_settings_title_wifi;
        }
        if (usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_usb_bluetooth;
        }
        if (usbAvailable) {
            return R.string.tether_settings_title_usb;
        }
        return R.string.tether_settings_title_bluetooth;
    }

    public static boolean copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = Profile.CONTENT_URI;
        UserManager um = (UserManager) context.getSystemService("user");
        int userId = user != null ? user.id : UserHandle.myUserId();
        InputStream avatarDataStream = Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
        if (avatarDataStream == null) {
            Log.d("Settings", "Profile photo changed, but the phote is null.");
            um.setUserIcon(userId, null);
            return false;
        }
        um.setUserIcon(userId, BitmapFactory.decodeStream(avatarDataStream));
        try {
            avatarDataStream.close();
        } catch (IOException e) {
        }
        return true;
    }

    public static Dialog buildGlobalChangeWarningDialog(Context context, int titleResId, final Runnable positiveAction) {
        Builder builder = new Builder(context);
        builder.setTitle(titleResId);
        builder.setMessage(R.string.global_change_warning);
        builder.setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                positiveAction.run();
            }
        });
        builder.setNegativeButton(17039360, null);
        return builder.create();
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService("user")).getUsers().size() > 1;
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode, null, titleResId, title, false);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId, CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode, titleResPackageName, titleResId, title, false);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, null, titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResPackageName, titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragmentAsUser(Context context, String fragmentName, Bundle args, int titleResId, CharSequence title, boolean isShortcut, UserHandle userHandle) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, null, titleResId, title, isShortcut);
        intent.addFlags(268435456);
        intent.addFlags(32768);
        context.startActivityAsUser(intent, userHandle);
    }

    public static Intent onBuildStartFragmentIntent(Context context, String fragmentName, Bundle args, String titleResPackageName, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(context, SubSettings.class);
        intent.putExtra(":settings:show_fragment", fragmentName);
        intent.putExtra(":settings:show_fragment_args", args);
        intent.putExtra(":settings:show_fragment_title_res_package_name", titleResPackageName);
        intent.putExtra(":settings:show_fragment_title_resid", titleResId);
        intent.putExtra(":settings:show_fragment_title", title);
        intent.putExtra(":settings:show_fragment_as_shortcut", isShortcut);
        return intent;
    }

    public static UserHandle getManagedProfile(UserManager userManager) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            UserHandle profile = (UserHandle) userProfiles.get(i);
            if (profile.getIdentifier() != userManager.getUserHandle() && userManager.getUserInfo(profile.getIdentifier()).isManagedProfile()) {
                return profile;
            }
        }
        return null;
    }

    public static boolean isManagedProfile(UserManager userManager) {
        return userManager.getUserInfo(userManager.getUserHandle()).isManagedProfile();
    }

    public static UserAdapter createUserSpinnerAdapter(UserManager userManager, Context context) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        if (userProfiles.size() < 2) {
            return null;
        }
        UserHandle myUserHandle = new UserHandle(UserHandle.myUserId());
        userProfiles.remove(myUserHandle);
        userProfiles.add(0, myUserHandle);
        return createUserAdapter(userManager, context, userProfiles);
    }

    public static UserAdapter createUserAdapter(UserManager userManager, Context context, List<UserHandle> userProfiles) {
        ArrayList<UserDetails> userDetails = new ArrayList(userProfiles.size());
        int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            userDetails.add(new UserDetails((UserHandle) userProfiles.get(i), userManager, context));
        }
        return new UserAdapter(context, userDetails);
    }

    public static UserHandle getSecureTargetUser(IBinder activityToken, UserManager um, Bundle arguments, Bundle intentExtras) {
        UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            boolean launchedFromSettingsApp = "com.android.settings".equals(am.getLaunchedFromPackage(activityToken));
            UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser != null && !launchedFromUser.equals(currentUser) && isProfileOf(um, launchedFromUser)) {
                return launchedFromUser;
            }
            UserHandle userHandle = intentExtras != null ? (UserHandle) intentExtras.getParcelable("android.intent.extra.USER") : null;
            if (userHandle != null && !userHandle.equals(currentUser) && launchedFromSettingsApp && isProfileOf(um, userHandle)) {
                return userHandle;
            }
            UserHandle userHandle2 = arguments != null ? (UserHandle) arguments.getParcelable("android.intent.extra.USER") : null;
            if (userHandle2 == null || userHandle2.equals(currentUser) || !launchedFromSettingsApp || !isProfileOf(um, userHandle2)) {
                return currentUser;
            }
            return userHandle2;
        } catch (RemoteException e) {
            Log.v("Settings", "Could not talk to activity manager.", e);
        }
    }

    private static boolean isProfileOf(UserManager um, UserHandle otherUser) {
        if (um == null || otherUser == null) {
            return false;
        }
        boolean contains;
        if (UserHandle.myUserId() != otherUser.getIdentifier()) {
            contains = um.getUserProfiles().contains(otherUser);
        } else {
            contains = true;
        }
        return contains;
    }

    static boolean isOemUnlockEnabled(Context context) {
        return ((PersistentDataBlockManager) context.getSystemService("persistent_data_block")).getOemUnlockEnabled();
    }

    static void setOemUnlockEnabled(Context context, boolean enabled) {
        ((PersistentDataBlockManager) context.getSystemService("persistent_data_block")).setOemUnlockEnabled(enabled);
    }

    public static Drawable getUserIcon(Context context, UserManager um, UserInfo user) {
        if (user.isManagedProfile()) {
            return CircleFramedDrawable.getInstance(context, BitmapFactory.decodeResource(context.getResources(), 17302323));
        }
        if (user.iconPath != null) {
            Bitmap icon = um.getUserIcon(user.id);
            if (icon != null) {
                return CircleFramedDrawable.getInstance(context, icon);
            }
        }
        return CircleFramedDrawable.getInstance(context, UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(user.id, false)));
    }

    public static String getUserLabel(Context context, UserInfo info) {
        String str = info != null ? info.name : null;
        if (info.isManagedProfile()) {
            return context.getString(R.string.managed_user_title);
        }
        if (info.isGuest()) {
            str = context.getString(R.string.user_guest);
        }
        if (str == null && info != null) {
            str = Integer.toString(info.id);
        } else if (info == null) {
            str = context.getString(R.string.unknown);
        }
        return context.getResources().getString(R.string.running_process_item_user_label, new Object[]{str});
    }

    public static boolean showSimCardTile(Context context) {
        if (((TelephonyManager) context.getSystemService("phone")).getSimCount() > 1) {
            return true;
        }
        return false;
    }

    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(pm)};
        }
        if (sSystemSignature[0] != null) {
            return sSystemSignature[0].equals(getFirstSignature(pkg));
        }
        return false;
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg == null || pkg.signatures == null || pkg.signatures.length <= 0) {
            return null;
        }
        return pkg.signatures[0];
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            return getFirstSignature(pm.getPackageInfo("android", 64));
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static String formatElapsedTime(Context context, double millis, boolean withSeconds) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000.0d);
        if (!withSeconds) {
            seconds += 30;
        }
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (seconds >= 86400) {
            days = seconds / 86400;
            seconds -= 86400 * days;
        }
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds -= hours * 3600;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        if (withSeconds) {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days, new Object[]{Integer.valueOf(days), Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)}));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours, new Object[]{Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)}));
            } else if (minutes > 0) {
                sb.append(context.getString(R.string.battery_history_minutes, new Object[]{Integer.valueOf(minutes), Integer.valueOf(seconds)}));
            } else {
                sb.append(context.getString(R.string.battery_history_seconds, new Object[]{Integer.valueOf(seconds)}));
            }
        } else if (days > 0) {
            sb.append(context.getString(R.string.battery_history_days_no_seconds, new Object[]{Integer.valueOf(days), Integer.valueOf(hours), Integer.valueOf(minutes)}));
        } else if (hours > 0) {
            sb.append(context.getString(R.string.battery_history_hours_no_seconds, new Object[]{Integer.valueOf(hours), Integer.valueOf(minutes)}));
        } else {
            sb.append(context.getString(R.string.battery_history_minutes_no_seconds, new Object[]{Integer.valueOf(minutes)}));
        }
        return sb.toString();
    }

    public static UserInfo getExistingUser(UserManager userManager, UserHandle checkUser) {
        List<UserInfo> users = userManager.getUsers(true);
        int checkUserId = checkUser.getIdentifier();
        for (UserInfo user : users) {
            if (user.id == checkUserId) {
                return user;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
        TypedArray a = inflater.getContext().obtainStyledAttributes(null, R.styleable.Preference, 16842892, 0);
        int resId = a.getResourceId(3, 0);
        a.recycle();
        return inflater.inflate(resId, parent, false);
    }

    public static boolean isLowStorage(Context context) {
        return StorageManager.from(context).getStorageBytesUntilLow(context.getFilesDir()) < 0;
    }

    public static Bitmap getDefaultUserIconAsBitmap(int userId) {
        Bitmap bitmap = (Bitmap) sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap != null) {
            return bitmap;
        }
        bitmap = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(userId, false));
        sDarkDefaultUserBitmapCache.put(userId, bitmap);
        return bitmap;
    }

    public static boolean hasUsbDefaults(IUsbManager usbManager, String packageName) {
        if (usbManager != null) {
            try {
                return usbManager.hasDefaults(packageName, UserHandle.myUserId());
            } catch (RemoteException e) {
                Log.e("Settings", "mUsbManager.hasDefaults", e);
            }
        }
        return false;
    }

    public static boolean hasPreferredActivities(PackageManager pm, String packageName) {
        List<ComponentName> prefActList = new ArrayList();
        pm.getPreferredActivities(new ArrayList(), prefActList, packageName);
        Log.d("Settings", "Have " + prefActList.size() + " number of activities in preferred list");
        if (prefActList.size() > 0) {
            return true;
        }
        return false;
    }

    public static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        List<IntentFilterVerificationInfo> iviList = pm.getIntentFilterVerifications(packageName);
        List<IntentFilter> filters = pm.getAllIntentFilters(packageName);
        ArraySet<String> result = new ArraySet();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory("android.intent.category.BROWSABLE") && (filter.hasDataScheme("http") || filter.hasDataScheme("https"))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }

    public static CharSequence getLaunchByDeafaultSummary(AppEntry appEntry, IUsbManager usbManager, PackageManager pm, Context context) {
        boolean hasPreferred;
        int i;
        String packageName = appEntry.info.packageName;
        if (hasPreferredActivities(pm, packageName)) {
            hasPreferred = true;
        } else {
            hasPreferred = hasUsbDefaults(usbManager, packageName);
        }
        int status = pm.getIntentVerificationStatus(packageName, UserHandle.myUserId());
        boolean hasDomainURLsPreference = status != 2 ? status == 3 : true;
        if (hasPreferred || hasDomainURLsPreference) {
            i = R.string.launch_defaults_some;
        } else {
            i = R.string.launch_defaults_none;
        }
        return context.getString(i);
    }

    public static void handleLoadingContainer(View loading, View doneLoading, boolean done, boolean animate) {
        setViewShown(loading, !done, animate);
        setViewShown(doneLoading, done, animate);
    }

    private static void setViewShown(final View view, boolean shown, boolean animate) {
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), shown ? 17432576 : 17432577);
            if (shown) {
                view.setVisibility(0);
            } else {
                animation.setAnimationListener(new AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(4);
                    }
                });
            }
            view.startAnimation(animation);
            return;
        }
        view.clearAnimation();
        view.setVisibility(shown ? 0 : 4);
    }

    public static ApplicationInfo getAdminApplicationInfo(Context context, int profileId) {
        ComponentName mdmPackage = ((DevicePolicyManager) context.getSystemService("device_policy")).getProfileOwnerAsUser(profileId);
        if (mdmPackage == null) {
            return null;
        }
        String mdmPackageName = mdmPackage.getPackageName();
        try {
            return AppGlobals.getPackageManager().getApplicationInfo(mdmPackageName, 0, profileId);
        } catch (RemoteException e) {
            Log.e("Settings", "Error while retrieving application info for package " + mdmPackageName + ", userId " + profileId, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        try {
            return Stub.asInterface(ServiceManager.getService("network_management")).isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static SpannableString createAccessibleSequence(CharSequence displayText, String accessibileText) {
        SpannableString str = new SpannableString(displayText);
        str.setSpan(new TextBuilder(accessibileText).build(), 0, displayText.length(), 18);
        return str;
    }

    public static int getEffectiveUserId(Context context) {
        UserManager um = UserManager.get(context);
        if (um != null) {
            return um.getCredentialOwnerProfile(UserHandle.myUserId());
        }
        Log.e("Settings", "Unable to acquire UserManager");
        return UserHandle.myUserId();
    }
}
