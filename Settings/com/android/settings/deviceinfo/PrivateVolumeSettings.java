package com.android.settings.deviceinfo;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.IPackageDataObserver.Stub;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.ManageApplications;
import com.android.settings.deviceinfo.StorageSettings.MountTask;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.google.android.collect.Lists;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PrivateVolumeSettings extends SettingsPreferenceFragment {
    private static final int[] ITEMS_NO_SHOW_SHARED = new int[]{R.string.storage_detail_apps};
    private static final int[] ITEMS_SHOW_SHARED = new int[]{R.string.storage_detail_apps, R.string.storage_detail_images, R.string.storage_detail_videos, R.string.storage_detail_audio, R.string.storage_detail_other};
    private UserInfo mCurrentUser;
    private Preference mExplore;
    private int mHeaderPoolIndex;
    private List<PreferenceCategory> mHeaderPreferencePool = Lists.newArrayList();
    private int mItemPoolIndex;
    private List<StorageItemPreference> mItemPreferencePool = Lists.newArrayList();
    private StorageMeasurement mMeasure;
    private final MeasurementReceiver mReceiver = new C03261();
    private VolumeInfo mSharedVolume;
    private final StorageEventListener mStorageListener = new C03272();
    private StorageManager mStorageManager;
    private StorageSummaryPreference mSummary;
    private UserManager mUserManager;
    private VolumeInfo mVolume;
    private String mVolumeId;

    class C03261 implements MeasurementReceiver {
        C03261() {
        }

        public void onDetailsChanged(MeasurementDetails details) {
            PrivateVolumeSettings.this.updateDetails(details);
        }
    }

    class C03272 extends StorageEventListener {
        C03272() {
        }

        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (Objects.equals(PrivateVolumeSettings.this.mVolume.getId(), vol.getId())) {
                PrivateVolumeSettings.this.mVolume = vol;
                PrivateVolumeSettings.this.update();
            }
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (Objects.equals(PrivateVolumeSettings.this.mVolume.getFsUuid(), rec.getFsUuid())) {
                PrivateVolumeSettings.this.mVolume = PrivateVolumeSettings.this.mStorageManager.findVolumeById(PrivateVolumeSettings.this.mVolumeId);
                PrivateVolumeSettings.this.update();
            }
        }
    }

    private static class ClearCacheObserver extends Stub {
        private int mRemaining;
        private final PrivateVolumeSettings mTarget;

        class C03281 implements Runnable {
            C03281() {
            }

            public void run() {
                ClearCacheObserver.this.mTarget.update();
            }
        }

        public ClearCacheObserver(PrivateVolumeSettings target, int remaining) {
            this.mTarget = target;
            this.mRemaining = remaining;
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            synchronized (this) {
                int i = this.mRemaining - 1;
                this.mRemaining = i;
                if (i == 0) {
                    this.mTarget.getActivity().runOnUiThread(new C03281());
                }
            }
        }
    }

    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Fragment parent) {
            if (parent.isAdded()) {
                ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmClearCache");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            Builder builder = new Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));
            builder.setPositiveButton(17039370, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    PrivateVolumeSettings target = (PrivateVolumeSettings) ConfirmClearCacheFragment.this.getTargetFragment();
                    PackageManager pm = context.getPackageManager();
                    List<PackageInfo> infos = pm.getInstalledPackages(0);
                    ClearCacheObserver observer = new ClearCacheObserver(target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(17039360, null);
            return builder.create();
        }
    }

    public static class OtherInfoFragment extends DialogFragment {
        public static void show(Fragment parent, String title, VolumeInfo sharedVol) {
            if (parent.isAdded()) {
                OtherInfoFragment dialog = new OtherInfoFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putString("android.intent.extra.TITLE", title);
                args.putParcelable("android.intent.extra.INTENT", sharedVol.buildBrowseIntent());
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "otherInfo");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            String title = getArguments().getString("android.intent.extra.TITLE");
            final Intent intent = (Intent) getArguments().getParcelable("android.intent.extra.INTENT");
            Builder builder = new Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_other), new CharSequence[]{title}));
            builder.setPositiveButton(R.string.storage_menu_explore, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    OtherInfoFragment.this.startActivity(intent);
                }
            });
            builder.setNegativeButton(17039360, null);
            return builder.create();
        }
    }

    public static class RenameFragment extends DialogFragment {
        public static void show(PrivateVolumeSettings parent, VolumeInfo vol) {
            if (parent.isAdded()) {
                RenameFragment dialog = new RenameFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putString("android.os.storage.extra.FS_UUID", vol.getFsUuid());
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "rename");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
            final String fsUuid = getArguments().getString("android.os.storage.extra.FS_UUID");
            VolumeInfo vol = storageManager.findVolumeByUuid(fsUuid);
            VolumeRecord rec = storageManager.findRecordByUuid(fsUuid);
            Builder builder = new Builder(context);
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_edittext, null, false);
            final EditText nickname = (EditText) view.findViewById(R.id.edittext);
            nickname.setText(rec.getNickname());
            builder.setTitle(R.string.storage_rename_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.save, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    storageManager.setVolumeNickname(fsUuid, nickname.getText().toString());
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }

    public static class UserInfoFragment extends DialogFragment {
        public static void show(Fragment parent, CharSequence userLabel, CharSequence userSize) {
            if (parent.isAdded()) {
                UserInfoFragment dialog = new UserInfoFragment();
                dialog.setTargetFragment(parent, 0);
                Bundle args = new Bundle();
                args.putCharSequence("android.intent.extra.TITLE", userLabel);
                args.putCharSequence("android.intent.extra.SUBJECT", userSize);
                dialog.setArguments(args);
                dialog.show(parent.getFragmentManager(), "userInfo");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            CharSequence userLabel = getArguments().getCharSequence("android.intent.extra.TITLE");
            CharSequence userSize = getArguments().getCharSequence("android.intent.extra.SUBJECT");
            Builder builder = new Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_user), new CharSequence[]{userLabel, userSize}));
            builder.setPositiveButton(17039370, null);
            return builder.create();
        }
    }

    private boolean isVolumeValid() {
        if (this.mVolume == null || this.mVolume.getType() != 1) {
            return false;
        }
        return this.mVolume.isMountedReadable();
    }

    protected int getMetricsCategory() {
        return 42;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mVolumeId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        this.mSharedVolume = this.mStorageManager.findEmulatedForPrivate(this.mVolume);
        this.mMeasure = new StorageMeasurement(context, this.mVolume, this.mSharedVolume);
        this.mMeasure.setReceiver(this.mReceiver);
        if (isVolumeValid()) {
            addPreferencesFromResource(R.xml.device_info_storage_volume);
            getPreferenceScreen().setOrderingAsAdded(true);
            this.mSummary = new StorageSummaryPreference(context);
            this.mCurrentUser = this.mUserManager.getUserInfo(UserHandle.myUserId());
            this.mExplore = buildAction(R.string.storage_menu_explore);
            setHasOptionsMenu(true);
            return;
        }
        getActivity().finish();
    }

    public void update() {
        if (isVolumeValid()) {
            int userIndex;
            UserInfo userInfo;
            getActivity().setTitle(this.mStorageManager.getBestVolumeDescription(this.mVolume));
            getFragmentManager().invalidateOptionsMenu();
            Context context = getActivity();
            PreferenceGroup screen = getPreferenceScreen();
            screen.removeAll();
            addPreference(screen, this.mSummary);
            List<UserInfo> allUsers = this.mUserManager.getUsers();
            int userCount = allUsers.size();
            boolean showHeaders = userCount > 1;
            boolean isMountedReadable = this.mSharedVolume != null ? this.mSharedVolume.isMountedReadable() : false;
            this.mItemPoolIndex = 0;
            this.mHeaderPoolIndex = 0;
            int addedUserCount = 0;
            for (userIndex = 0; userIndex < userCount; userIndex++) {
                userInfo = (UserInfo) allUsers.get(userIndex);
                if (isProfileOf(this.mCurrentUser, userInfo)) {
                    addDetailItems(showHeaders ? addCategory(screen, userInfo.name) : screen, isMountedReadable, userInfo.id);
                    addedUserCount++;
                }
            }
            if (userCount - addedUserCount > 0) {
                PreferenceGroup otherUsers = addCategory(screen, getText(R.string.storage_other_users));
                for (userIndex = 0; userIndex < userCount; userIndex++) {
                    userInfo = (UserInfo) allUsers.get(userIndex);
                    if (!isProfileOf(this.mCurrentUser, userInfo)) {
                        addItem(otherUsers, 0, userInfo.name, userInfo.id);
                    }
                }
            }
            addItem(screen, R.string.storage_detail_cached, null, -10000);
            if (isMountedReadable) {
                addPreference(screen, this.mExplore);
            }
            File file = this.mVolume.getPath();
            long totalBytes = file.getTotalSpace();
            long usedBytes = totalBytes - file.getFreeSpace();
            BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
            this.mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), new CharSequence[]{result.value, result.units}));
            this.mSummary.setSummary(getString(R.string.storage_volume_used, new Object[]{"16 GB"}));
            this.mSummary.setPercent((int) ((100 * usedBytes) / totalBytes));
            this.mMeasure.forceMeasure();
            return;
        }
        getActivity().finish();
    }

    private void addPreference(PreferenceGroup group, Preference pref) {
        pref.setOrder(Integer.MAX_VALUE);
        group.addPreference(pref);
    }

    private PreferenceCategory addCategory(PreferenceGroup group, CharSequence title) {
        PreferenceCategory category;
        if (this.mHeaderPoolIndex < this.mHeaderPreferencePool.size()) {
            category = (PreferenceCategory) this.mHeaderPreferencePool.get(this.mHeaderPoolIndex);
        } else {
            category = new PreferenceCategory(getActivity(), null, 16842892);
            this.mHeaderPreferencePool.add(category);
        }
        category.setTitle(title);
        category.removeAll();
        addPreference(group, category);
        this.mHeaderPoolIndex++;
        return category;
    }

    private void addDetailItems(PreferenceGroup category, boolean showShared, int userId) {
        int[] itemsToAdd = showShared ? ITEMS_SHOW_SHARED : ITEMS_NO_SHOW_SHARED;
        for (int addItem : itemsToAdd) {
            addItem(category, addItem, null, userId);
        }
    }

    private void addItem(PreferenceGroup group, int titleRes, CharSequence title, int userId) {
        StorageItemPreference item;
        if (this.mItemPoolIndex < this.mItemPreferencePool.size()) {
            item = (StorageItemPreference) this.mItemPreferencePool.get(this.mItemPoolIndex);
        } else {
            item = buildItem();
            this.mItemPreferencePool.add(item);
        }
        if (title != null) {
            item.setTitle(title);
        } else {
            item.setTitle(titleRes);
        }
        item.setSummary(R.string.memory_calculating_size);
        item.userHandle = userId;
        addPreference(group, item);
        this.mItemPoolIndex++;
    }

    private StorageItemPreference buildItem() {
        return new StorageItemPreference(getActivity());
    }

    private Preference buildAction(int titleRes) {
        Preference pref = new Preference(getActivity());
        pref.setTitle(titleRes);
        return pref;
    }

    public void onResume() {
        super.onResume();
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        if (isVolumeValid()) {
            this.mStorageManager.registerListener(this.mStorageListener);
            update();
            return;
        }
        getActivity().finish();
    }

    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mMeasure != null) {
            this.mMeasure.onDestroy();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.storage_volume, menu);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        boolean z = false;
        if (isVolumeValid()) {
            MenuItem rename = menu.findItem(R.id.storage_rename);
            MenuItem mount = menu.findItem(R.id.storage_mount);
            MenuItem unmount = menu.findItem(R.id.storage_unmount);
            MenuItem format = menu.findItem(R.id.storage_format);
            MenuItem migrate = menu.findItem(R.id.storage_migrate);
            if ("private".equals(this.mVolume.getId())) {
                rename.setVisible(false);
                mount.setVisible(false);
                unmount.setVisible(false);
                format.setVisible(false);
            } else {
                boolean z2;
                if (this.mVolume.getType() == 1) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                rename.setVisible(z2);
                if (this.mVolume.getState() == 0) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                mount.setVisible(z2);
                unmount.setVisible(this.mVolume.isMountedReadable());
                format.setVisible(true);
            }
            format.setTitle(R.string.storage_menu_format_public);
            VolumeInfo privateVol = getActivity().getPackageManager().getPrimaryStorageCurrentVolume();
            if (!(privateVol == null || privateVol.getType() != 1 || Objects.equals(this.mVolume, privateVol))) {
                z = StorageManagerEx.isSetPrimaryStorageUuidFinish();
            }
            migrate.setVisible(z);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getActivity();
        Bundle args = new Bundle();
        switch (item.getItemId()) {
            case R.id.storage_rename:
                RenameFragment.show(this, this.mVolume);
                return true;
            case R.id.storage_mount:
                new MountTask(context, this.mVolume).execute(new Void[0]);
                return true;
            case R.id.storage_unmount:
                args.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startFragment(this, PrivateVolumeUnmount.class.getCanonicalName(), R.string.storage_menu_unmount, 0, args);
                return true;
            case R.id.storage_format:
                args.putString("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startFragment(this, PrivateVolumeFormat.class.getCanonicalName(), R.string.storage_menu_format, 0, args);
                return true;
            case R.id.storage_migrate:
                Intent intent = new Intent(context, StorageWizardMigrateConfirm.class);
                intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        int userId = pref instanceof StorageItemPreference ? ((StorageItemPreference) pref).userHandle : -1;
        Intent intent = null;
        switch (pref.getTitleRes()) {
            case 0:
                UserInfoFragment.show(this, pref.getTitle(), pref.getSummary());
                return true;
            case R.string.storage_menu_explore:
                intent = this.mSharedVolume.buildBrowseIntent();
                break;
            case R.string.storage_detail_apps:
                Bundle args = new Bundle();
                args.putString("classname", StorageUseActivity.class.getName());
                args.putString("volumeUuid", this.mVolume.getFsUuid());
                args.putString("volumeName", this.mVolume.getDescription());
                intent = Utils.onBuildStartFragmentIntent(getActivity(), ManageApplications.class.getName(), args, null, R.string.apps_storage, null, false);
                break;
            case R.string.storage_detail_images:
                intent = new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "images_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_videos:
                intent = new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "videos_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_audio:
                intent = new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT");
                intent.setData(DocumentsContract.buildRootUri("com.android.providers.media.documents", "audio_root"));
                intent.addCategory("android.intent.category.DEFAULT");
                break;
            case R.string.storage_detail_cached:
                ConfirmClearCacheFragment.show(this);
                return true;
            case R.string.storage_detail_other:
                OtherInfoFragment.show(this, this.mStorageManager.getBestVolumeDescription(this.mVolume), this.mSharedVolume);
                return true;
        }
        if (intent == null) {
            return super.onPreferenceTreeClick(preferenceScreen, pref);
        }
        if (userId == -1) {
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w("StorageSettings", "No activity found for " + intent);
            }
        } else {
            getActivity().startActivityAsUser(intent, new UserHandle(userId));
        }
        return true;
    }

    private void updateDetails(MeasurementDetails details) {
        for (int i = 0; i < this.mItemPoolIndex; i++) {
            StorageItemPreference item = (StorageItemPreference) this.mItemPreferencePool.get(i);
            int userId = item.userHandle;
            switch (item.getTitleRes()) {
                case 0:
                    updatePreference(item, details.usersSize.get(userId));
                    break;
                case R.string.storage_detail_apps:
                    updatePreference(item, details.appsSize.get(userId));
                    break;
                case R.string.storage_detail_images:
                    updatePreference(item, totalValues(details, userId, Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES));
                    break;
                case R.string.storage_detail_videos:
                    updatePreference(item, totalValues(details, userId, Environment.DIRECTORY_MOVIES));
                    break;
                case R.string.storage_detail_audio:
                    updatePreference(item, totalValues(details, userId, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS));
                    break;
                case R.string.storage_detail_cached:
                    updatePreference(item, details.cacheSize);
                    break;
                case R.string.storage_detail_other:
                    updatePreference(item, details.miscSize.get(userId));
                    break;
                default:
                    break;
            }
        }
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        pref.setSummary(Formatter.formatFileSize(getActivity(), size));
    }

    private boolean isProfileOf(UserInfo user, UserInfo profile) {
        if (user.id == profile.id) {
            return true;
        }
        if (user.profileGroupId != -1) {
            return user.profileGroupId == profile.profileGroupId;
        } else {
            return false;
        }
    }

    private static long totalValues(MeasurementDetails details, int userId, String... keys) {
        long total = 0;
        HashMap<String, Long> map = (HashMap) details.mediaSize.get(userId);
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += ((Long) map.get(key)).longValue();
                }
            }
        } else {
            Log.w("StorageSettings", "MeasurementDetails mediaSize array does not have key for user " + userId);
        }
        return total;
    }
}
