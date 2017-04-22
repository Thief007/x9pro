package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceFrameLayout.LayoutParams;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filter.FilterResults;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.AppHeader;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings.AllApplicationsActivity;
import com.android.settings.Settings.DomainsURLsAppListActivity;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.Settings.NotificationAppListActivity;
import com.android.settings.Settings.OverlaySettingsActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.Settings.UsageAccessSettingsActivity;
import com.android.settings.Settings.WriteSettingsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.ApplicationsState.Session;
import com.android.settingslib.applications.ApplicationsState.VolumeFilter;
import com.android.setupwizardlib.R$styleable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ManageApplications extends InstrumentedFragment implements OnItemClickListener, OnItemSelectedListener {
    static final boolean DEBUG = Log.isLoggable("ManageApplications", 3);
    public static final AppFilter[] FILTERS = new AppFilter[]{new CompoundFilter(AppStatePowerBridge.FILTER_POWER_WHITELISTED, ApplicationsState.FILTER_ALL_ENABLED), new CompoundFilter(ApplicationsState.FILTER_PERSONAL_WITHOUT_DISABLED_UNTIL_USED, ApplicationsState.FILTER_ALL_ENABLED), ApplicationsState.FILTER_EVERYTHING, ApplicationsState.FILTER_ALL_ENABLED, ApplicationsState.FILTER_DISABLED, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_PRIORITY, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_NO_PEEK, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_SENSITIVE, ApplicationsState.FILTER_PERSONAL, ApplicationsState.FILTER_WORK, ApplicationsState.FILTER_WITH_DOMAIN_URLS, AppStateUsageBridge.FILTER_APP_USAGE, AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW, AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS};
    public static final int[] FILTER_LABELS = new int[]{R.string.high_power_filter_on, R.string.filter_all_apps, R.string.filter_all_apps, R.string.filter_enabled_apps, R.string.filter_apps_disabled, R.string.filter_notif_blocked_apps, R.string.filter_notif_priority_apps, R.string.filter_notif_no_peeking, R.string.filter_notif_sensitive_apps, R.string.filter_personal_apps, R.string.filter_work_apps, R.string.filter_with_domain_urls_apps, R.string.filter_all_apps, R.string.filter_overlay_apps, R.string.filter_write_settings_apps};
    public ApplicationsAdapter mApplications;
    private ApplicationsState mApplicationsState;
    private String mCurrentPkgName;
    private int mCurrentUid;
    public int mFilter;
    private FilterSpinnerAdapter mFilterAdapter;
    private Spinner mFilterSpinner;
    private boolean mFinishAfterDialog;
    private LayoutInflater mInflater;
    CharSequence mInvalidSizeStr;
    private View mListContainer;
    public int mListType;
    private ListView mListView;
    private View mLoadingContainer;
    private NotificationBackend mNotifBackend;
    private Menu mOptionsMenu;
    private ResetAppsHelper mResetAppsHelper;
    private View mRootView;
    private boolean mShowSystem;
    private int mSortOrder = R.id.sort_order_alpha;
    private View mSpinnerHeader;
    private String mVolumeName;
    private String mVolumeUuid;

    static class ApplicationsAdapter extends BaseAdapter implements Filterable, Callbacks, Callback, RecyclerListener {
        private final ArrayList<View> mActive = new ArrayList();
        private ArrayList<AppEntry> mBaseEntries;
        private final Context mContext;
        CharSequence mCurFilterPrefix;
        private ArrayList<AppEntry> mEntries;
        private final AppStateBaseBridge mExtraInfoBridge;
        private Filter mFilter = new C02611();
        private int mFilterMode;
        private boolean mHasReceivedBridgeCallback;
        private boolean mHasReceivedLoadEntries;
        private int mLastSortMode = -1;
        private final ManageApplications mManageApplications;
        private AppFilter mOverrideFilter;
        private PackageManager mPm;
        private boolean mResumed;
        private final Session mSession;
        private final ApplicationsState mState;
        private int mWhichSize = 0;

        class C02611 extends Filter {
            C02611() {
            }

            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<AppEntry> entries = ApplicationsAdapter.this.applyPrefixFilter(constraint, ApplicationsAdapter.this.mBaseEntries);
                FilterResults fr = new FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            protected void publishResults(CharSequence constraint, FilterResults results) {
                ApplicationsAdapter.this.mCurFilterPrefix = constraint;
                ApplicationsAdapter.this.mEntries = (ArrayList) results.values;
                ApplicationsAdapter.this.notifyDataSetChanged();
            }
        }

        public ApplicationsAdapter(ApplicationsState state, ManageApplications manageApplications, int filterMode) {
            this.mState = state;
            this.mSession = state.newSession(this);
            this.mManageApplications = manageApplications;
            this.mContext = manageApplications.getActivity();
            this.mPm = this.mContext.getPackageManager();
            this.mFilterMode = filterMode;
            if (this.mManageApplications.mListType == 1) {
                this.mExtraInfoBridge = new AppStateNotificationBridge(this.mContext.getPackageManager(), this.mState, this, manageApplications.mNotifBackend);
            } else if (this.mManageApplications.mListType == 4) {
                this.mExtraInfoBridge = new AppStateUsageBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 5) {
                this.mExtraInfoBridge = new AppStatePowerBridge(this.mState, this);
            } else if (this.mManageApplications.mListType == 6) {
                this.mExtraInfoBridge = new AppStateOverlayBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 7) {
                this.mExtraInfoBridge = new AppStateWriteSettingsBridge(this.mContext, this.mState, this);
            } else {
                this.mExtraInfoBridge = null;
            }
        }

        public void setOverrideFilter(AppFilter overrideFilter) {
            this.mOverrideFilter = overrideFilter;
            rebuild(true);
        }

        public void setFilter(int filter) {
            this.mFilterMode = filter;
            rebuild(true);
        }

        public void resume(int sort) {
            if (ManageApplications.DEBUG) {
                Log.i("ManageApplications", "Resume!  mResumed=" + this.mResumed);
            }
            if (this.mResumed) {
                rebuild(sort);
                return;
            }
            this.mResumed = true;
            this.mSession.resume();
            this.mLastSortMode = sort;
            if (this.mExtraInfoBridge != null) {
                this.mExtraInfoBridge.resume();
            }
            rebuild(true);
        }

        public void pause() {
            if (this.mResumed) {
                this.mResumed = false;
                this.mSession.pause();
                if (this.mExtraInfoBridge != null) {
                    this.mExtraInfoBridge.pause();
                }
            }
        }

        public void release() {
            this.mSession.release();
            if (this.mExtraInfoBridge != null) {
                this.mExtraInfoBridge.release();
            }
        }

        public void rebuild(int sort) {
            if (sort != this.mLastSortMode) {
                this.mLastSortMode = sort;
                rebuild(true);
            }
        }

        public void rebuild(boolean eraseold) {
            if (this.mHasReceivedLoadEntries || !(this.mExtraInfoBridge == null || this.mHasReceivedBridgeCallback)) {
                Comparator<AppEntry> comparatorObj;
                if (ManageApplications.DEBUG) {
                    Log.i("ManageApplications", "Rebuilding app list...");
                }
                if (Environment.isExternalStorageEmulated()) {
                    this.mWhichSize = 0;
                } else {
                    this.mWhichSize = 1;
                }
                AppFilter filterObj = ManageApplications.FILTERS[this.mFilterMode];
                if (this.mOverrideFilter != null) {
                    filterObj = this.mOverrideFilter;
                }
                if (!this.mManageApplications.mShowSystem) {
                    filterObj = new CompoundFilter(filterObj, ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
                }
                switch (this.mLastSortMode) {
                    case R.id.sort_order_size:
                        switch (this.mWhichSize) {
                            case 1:
                                comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                                break;
                            case 2:
                                comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                                break;
                            default:
                                comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                                break;
                        }
                    default:
                        comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                        break;
                }
                ArrayList<AppEntry> entries = this.mSession.rebuild(filterObj, comparatorObj);
                if (entries != null || eraseold) {
                    this.mBaseEntries = entries;
                    if (this.mBaseEntries != null) {
                        this.mEntries = applyPrefixFilter(this.mCurFilterPrefix, this.mBaseEntries);
                    } else {
                        this.mEntries = null;
                    }
                    notifyDataSetChanged();
                    if (!(this.mSession.getAllApps().size() == 0 || this.mManageApplications.mListContainer.getVisibility() == 0)) {
                        Utils.handleLoadingContainer(this.mManageApplications.mLoadingContainer, this.mManageApplications.mListContainer, true, true);
                    }
                    if (this.mManageApplications.mListType != 4) {
                        this.mManageApplications.setHasDisabled(this.mState.haveDisabledApps());
                    }
                }
            }
        }

        private void updateLoading() {
            boolean z;
            View -get4 = this.mManageApplications.mLoadingContainer;
            View -get3 = this.mManageApplications.mListContainer;
            if (!this.mHasReceivedLoadEntries || this.mSession.getAllApps().size() == 0) {
                z = false;
            } else {
                z = true;
            }
            Utils.handleLoadingContainer(-get4, -get3, z, false);
        }

        ArrayList<AppEntry> applyPrefixFilter(CharSequence prefix, ArrayList<AppEntry> origEntries) {
            int i;
            List<String> hideApps = Arrays.asList(new String[]{"com.opera.branding", "com.cooee.widget.searchwidget", "com.sherlock.news", "com.android.Pet.mediaproxy"});
            ArrayList arrayList = new ArrayList();
            ArrayList<AppEntry> showEntries = origEntries;
            for (i = 0; i < origEntries.size(); i++) {
                AppEntry entry = (AppEntry) origEntries.get(i);
                if (hideApps.contains(entry.info.packageName)) {
                    showEntries.remove(entry);
                }
            }
            origEntries = showEntries;
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            }
            String prefixStr = ApplicationsState.normalize(prefix.toString());
            String spacePrefixStr = " " + prefixStr;
            ArrayList<AppEntry> newEntries = new ArrayList();
            for (i = 0; i < origEntries.size(); i++) {
                entry = (AppEntry) origEntries.get(i);
                String nlabel = entry.getNormalizedLabel();
                if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                    newEntries.add(entry);
                }
            }
            return newEntries;
        }

        public void onExtraInfoUpdated() {
            this.mHasReceivedBridgeCallback = true;
            rebuild(false);
        }

        public void onRunningStateChanged(boolean running) {
            this.mManageApplications.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            if (this.mManageApplications.mLoadingContainer.getVisibility() == 0) {
                this.mManageApplications.mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(this.mContext, 17432577));
                this.mManageApplications.mListContainer.startAnimation(AnimationUtils.loadAnimation(this.mContext, 17432576));
            }
            this.mManageApplications.mListContainer.setVisibility(0);
            this.mManageApplications.mLoadingContainer.setVisibility(8);
            this.mBaseEntries = apps;
            this.mEntries = applyPrefixFilter(this.mCurFilterPrefix, this.mBaseEntries);
            notifyDataSetChanged();
        }

        public void onPackageListChanged() {
            rebuild(false);
        }

        public void onPackageIconChanged() {
        }

        public void onLoadEntriesCompleted() {
            this.mHasReceivedLoadEntries = true;
        }

        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < this.mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder) ((View) this.mActive.get(i)).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        updateSummary(holder);
                    }
                    if (holder.entry.info.packageName.equals(this.mManageApplications.mCurrentPkgName) && this.mLastSortMode == R.id.sort_order_size) {
                        rebuild(false);
                    }
                    return;
                }
            }
        }

        public void onLauncherInfoChanged() {
            if (!this.mManageApplications.mShowSystem) {
                rebuild(false);
            }
        }

        public void onAllSizesComputed() {
            if (this.mLastSortMode == R.id.sort_order_size) {
                rebuild(false);
            }
        }

        public int getCount() {
            return this.mEntries != null ? this.mEntries.size() : 0;
        }

        public Object getItem(int position) {
            return this.mEntries.get(position);
        }

        public AppEntry getAppEntry(int position) {
            return (AppEntry) this.mEntries.get(position);
        }

        public long getItemId(int position) {
            return ((AppEntry) this.mEntries.get(position)).id;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            boolean z = true;
            if (this.mManageApplications.mListType != 5) {
                return true;
            }
            if (PowerWhitelistBackend.getInstance().isSysWhitelisted(((AppEntry) this.mEntries.get(position)).info.packageName)) {
                z = false;
            }
            return z;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(this.mManageApplications.mInflater, convertView);
            convertView = holder.rootView;
            AppEntry entry = (AppEntry) this.mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                this.mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                updateSummary(holder);
                if ((entry.info.flags & 8388608) == 0) {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.not_installed);
                } else if (entry.info.enabled) {
                    holder.disabled.setVisibility(8);
                } else {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.disabled);
                }
            }
            this.mActive.remove(convertView);
            this.mActive.add(convertView);
            convertView.setEnabled(isEnabled(position));
            return convertView;
        }

        private void updateSummary(AppViewHolder holder) {
            switch (this.mManageApplications.mListType) {
                case 1:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText(InstalledAppDetails.getNotificationSummary((AppRow) holder.entry.extraInfo, this.mContext));
                        return;
                    } else {
                        holder.summary.setText(null);
                        return;
                    }
                case 2:
                    holder.summary.setText(getDomainsSummary(holder.entry.info.packageName));
                    return;
                case 4:
                    if (holder.entry.extraInfo != null) {
                        int i;
                        TextView textView = holder.summary;
                        if (new UsageState((PermissionState) holder.entry.extraInfo).isPermissible()) {
                            i = R.string.switch_on_text;
                        } else {
                            i = R.string.switch_off_text;
                        }
                        textView.setText(i);
                        return;
                    }
                    holder.summary.setText(null);
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    holder.summary.setText(HighPowerDetail.getSummary(this.mContext, holder.entry));
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                    holder.summary.setText(DrawOverlayDetails.getSummary(this.mContext, holder.entry));
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                    holder.summary.setText(WriteSettingsDetails.getSummary(this.mContext, holder.entry));
                    return;
                default:
                    holder.updateSizeText(this.mManageApplications.mInvalidSizeStr, this.mWhichSize);
                    return;
            }
        }

        public Filter getFilter() {
            return this.mFilter;
        }

        public void onMovedToScrapHeap(View view) {
            this.mActive.remove(view);
        }

        private CharSequence getDomainsSummary(String packageName) {
            if (this.mPm.getIntentVerificationStatus(packageName, UserHandle.myUserId()) == 3) {
                return this.mContext.getString(R.string.domain_urls_summary_none);
            }
            ArraySet<String> result = Utils.getHandledDomains(this.mPm, packageName);
            if (result.size() == 0) {
                return this.mContext.getString(R.string.domain_urls_summary_none);
            }
            if (result.size() == 1) {
                return this.mContext.getString(R.string.domain_urls_summary_one, new Object[]{result.valueAt(0)});
            }
            return this.mContext.getString(R.string.domain_urls_summary_some, new Object[]{result.valueAt(0)});
        }
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {
        private final ArrayList<Integer> mFilterOptions = new ArrayList();
        private final ManageApplications mManageApplications;

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.getActivity(), R.layout.filter_spinner_item);
            setDropDownViewResource(17367049);
            this.mManageApplications = manageApplications;
        }

        public int getFilter(int position) {
            return ((Integer) this.mFilterOptions.get(position)).intValue();
        }

        public void setFilterEnabled(int filter, boolean enabled) {
            if (enabled) {
                enableFilter(filter);
            } else {
                disableFilter(filter);
            }
        }

        public void enableFilter(int filter) {
            if (!this.mFilterOptions.contains(Integer.valueOf(filter))) {
                if (ManageApplications.DEBUG) {
                    Log.d("ManageApplications", "Enabling filter " + filter);
                }
                this.mFilterOptions.add(Integer.valueOf(filter));
                Collections.sort(this.mFilterOptions);
                this.mManageApplications.mSpinnerHeader.setVisibility(this.mFilterOptions.size() > 1 ? 0 : 8);
                notifyDataSetChanged();
                if (this.mFilterOptions.size() == 1) {
                    if (ManageApplications.DEBUG) {
                        Log.d("ManageApplications", "Auto selecting filter " + filter);
                    }
                    this.mManageApplications.mFilterSpinner.setSelection(0);
                    this.mManageApplications.onItemSelected(null, null, 0, 0);
                }
            }
        }

        public void disableFilter(int filter) {
            if (this.mFilterOptions.remove(Integer.valueOf(filter))) {
                if (ManageApplications.DEBUG) {
                    Log.d("ManageApplications", "Disabling filter " + filter);
                }
                Collections.sort(this.mFilterOptions);
                this.mManageApplications.mSpinnerHeader.setVisibility(this.mFilterOptions.size() > 1 ? 0 : 8);
                notifyDataSetChanged();
                if (this.mManageApplications.mFilter == filter && this.mFilterOptions.size() > 0) {
                    if (ManageApplications.DEBUG) {
                        Log.d("ManageApplications", "Auto selecting filter " + this.mFilterOptions.get(0));
                    }
                    this.mManageApplications.mFilterSpinner.setSelection(0);
                    this.mManageApplications.onItemSelected(null, null, 0, 0);
                }
            }
        }

        public int getCount() {
            return this.mFilterOptions.size();
        }

        public CharSequence getItem(int position) {
            return getFilterString(((Integer) this.mFilterOptions.get(position)).intValue());
        }

        private CharSequence getFilterString(int filter) {
            return this.mManageApplications.getString(ManageApplications.FILTER_LABELS[filter]);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        String string = args != null ? args.getString("classname") : null;
        if (string == null) {
            string = intent.getComponent().getClassName();
        }
        if (string.equals(AllApplicationsActivity.class.getName())) {
            this.mShowSystem = true;
        } else if (string.equals(NotificationAppListActivity.class.getName())) {
            this.mListType = 1;
            this.mNotifBackend = new NotificationBackend();
        } else if (string.equals(DomainsURLsAppListActivity.class.getName())) {
            this.mListType = 2;
        } else if (string.equals(StorageUseActivity.class.getName())) {
            if (args == null || !args.containsKey("volumeUuid")) {
                this.mListType = 0;
            } else {
                this.mVolumeUuid = args.getString("volumeUuid");
                this.mVolumeName = args.getString("volumeName");
                this.mListType = 3;
            }
            this.mSortOrder = R.id.sort_order_size;
        } else if (string.equals(UsageAccessSettingsActivity.class.getName())) {
            this.mListType = 4;
            getActivity().getActionBar().setTitle(R.string.usage_access_title);
        } else if (string.equals(HighPowerApplicationsActivity.class.getName())) {
            this.mListType = 5;
            this.mShowSystem = true;
        } else if (string.equals(OverlaySettingsActivity.class.getName())) {
            this.mListType = 6;
            getActivity().getActionBar().setTitle(R.string.system_alert_window_access_title);
        } else if (string.equals(WriteSettingsActivity.class.getName())) {
            this.mListType = 7;
            getActivity().getActionBar().setTitle(R.string.write_settings_title);
        } else {
            this.mListType = 0;
        }
        this.mFilter = getDefaultFilter();
        if (savedInstanceState != null) {
            this.mSortOrder = savedInstanceState.getInt("sortOrder", this.mSortOrder);
            this.mShowSystem = savedInstanceState.getBoolean("showSystem", this.mShowSystem);
        }
        this.mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        this.mResetAppsHelper = new ResetAppsHelper(getActivity());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mInflater = inflater;
        this.mRootView = inflater.inflate(R.layout.manage_applications_apps, null);
        this.mLoadingContainer = this.mRootView.findViewById(R.id.loading_container);
        this.mLoadingContainer.setVisibility(0);
        this.mListContainer = this.mRootView.findViewById(R.id.list_container);
        if (this.mListContainer != null) {
            View emptyView = this.mListContainer.findViewById(16908292);
            ListView lv = (ListView) this.mListContainer.findViewById(16908298);
            if (emptyView != null) {
                lv.setEmptyView(emptyView);
            }
            lv.setOnItemClickListener(this);
            lv.setSaveEnabled(true);
            lv.setItemsCanFocus(true);
            lv.setTextFilterEnabled(true);
            this.mListView = lv;
            this.mApplications = new ApplicationsAdapter(this.mApplicationsState, this, this.mFilter);
            if (savedInstanceState != null) {
                this.mApplications.mHasReceivedLoadEntries = savedInstanceState.getBoolean("hasEntries", false);
            }
            this.mListView.setAdapter(this.mApplications);
            this.mListView.setRecyclerListener(this.mApplications);
            Utils.prepareCustomPreferencesList(container, this.mRootView, this.mListView, false);
        }
        if (container instanceof PreferenceFrameLayout) {
            ((LayoutParams) this.mRootView.getLayoutParams()).removeBorders = true;
        }
        createHeader();
        this.mResetAppsHelper.onRestoreInstanceState(savedInstanceState);
        return this.mRootView;
    }

    private void createHeader() {
        FrameLayout pinnedHeader = (FrameLayout) this.mRootView.findViewById(R.id.pinned_header);
        this.mSpinnerHeader = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.apps_filter_spinner, pinnedHeader, false);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new FilterSpinnerAdapter(this);
        this.mFilterSpinner.setAdapter(this.mFilterAdapter);
        this.mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(this.mSpinnerHeader, 0);
        this.mFilterAdapter.enableFilter(getDefaultFilter());
        if ((this.mListType == 0 || this.mListType == 1) && UserManager.get(getActivity()).getUserProfiles().size() > 1) {
            this.mFilterAdapter.enableFilter(9);
            this.mFilterAdapter.enableFilter(10);
        }
        if (this.mListType == 1) {
            this.mFilterAdapter.enableFilter(5);
            this.mFilterAdapter.enableFilter(6);
            this.mFilterAdapter.enableFilter(8);
            this.mFilterAdapter.enableFilter(7);
        }
        if (this.mListType == 5) {
            this.mFilterAdapter.enableFilter(1);
        }
        if (this.mListType == 3) {
            this.mApplications.setOverrideFilter(new VolumeFilter(this.mVolumeUuid));
        }
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.mListType == 3) {
            AppHeader.createAppHeader(getActivity(), null, this.mVolumeName, null, (FrameLayout) this.mRootView.findViewById(R.id.pinned_header));
        }
    }

    private int getDefaultFilter() {
        switch (this.mListType) {
            case 2:
                return 11;
            case 4:
                return 12;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                return 0;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                return 13;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                return 14;
            default:
                return 2;
        }
    }

    protected int getMetricsCategory() {
        switch (this.mListType) {
            case 0:
                return 65;
            case 1:
                return 133;
            case 2:
                return 143;
            case 3:
                return 182;
            case 4:
                return 95;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                return 184;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                return 221;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                return 221;
            default:
                return 0;
        }
    }

    public void onResume() {
        super.onResume();
        updateView();
        updateOptionsMenu();
        if (this.mApplications != null) {
            this.mApplications.resume(this.mSortOrder);
            this.mApplications.updateLoading();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mResetAppsHelper.onSaveInstanceState(outState);
        outState.putInt("sortOrder", this.mSortOrder);
        outState.putBoolean("showSystem", this.mShowSystem);
        outState.putBoolean("hasEntries", this.mApplications.mHasReceivedLoadEntries);
    }

    public void onPause() {
        super.onPause();
        if (this.mApplications != null) {
            this.mApplications.pause();
        }
    }

    public void onStop() {
        super.onStop();
        this.mResetAppsHelper.stop();
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (this.mApplications != null) {
            this.mApplications.release();
        }
        this.mRootView = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && this.mCurrentPkgName != null) {
            if (this.mListType == 1) {
                this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
            } else if (this.mListType != 5 && this.mListType != 6 && this.mListType != 7) {
                this.mApplicationsState.requestSize(this.mCurrentPkgName, UserHandle.getUserId(this.mCurrentUid));
            } else if (this.mFinishAfterDialog) {
                getActivity().onBackPressed();
            } else {
                this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
            }
        }
    }

    private void startApplicationDetailsActivity() {
        switch (this.mListType) {
            case 1:
                startAppInfoFragment(AppNotificationSettings.class, R.string.app_notifications_title);
                return;
            case 2:
                startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label);
                return;
            case 3:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_settings);
                return;
            case 4:
                startAppInfoFragment(UsageAccessDetails.class, R.string.usage_access);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                HighPowerDetail.show(this, this.mCurrentPkgName, 1, this.mFinishAfterDialog);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                startAppInfoFragment(DrawOverlayDetails.class, R.string.overlay_settings);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                startAppInfoFragment(WriteSettingsDetails.class, R.string.write_system_settings);
                return;
            default:
                startAppInfoFragment(InstalledAppDetails.class, R.string.application_info_label);
                return;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, titleRes, this.mCurrentPkgName, this.mCurrentUid, this, 1);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mListType != 2) {
            HelpUtils.prepareHelpMenuItem(getActivity(), menu, this.mListType == 0 ? R.string.help_uri_apps : R.string.help_uri_notifications, getClass().getName());
            this.mOptionsMenu = menu;
            inflater.inflate(R.menu.manage_apps, menu);
            updateOptionsMenu();
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    public void onDestroyOptionsMenu() {
        this.mOptionsMenu = null;
    }

    void updateOptionsMenu() {
        boolean z = false;
        if (this.mOptionsMenu != null) {
            boolean z2;
            MenuItem findItem = this.mOptionsMenu.findItem(R.id.advanced);
            if (this.mListType == 0) {
                z2 = true;
            } else {
                z2 = false;
            }
            findItem.setVisible(z2);
            findItem = this.mOptionsMenu.findItem(R.id.sort_order_alpha);
            z2 = this.mListType == 3 ? this.mSortOrder != R.id.sort_order_alpha : false;
            findItem.setVisible(z2);
            findItem = this.mOptionsMenu.findItem(R.id.sort_order_size);
            z2 = this.mListType == 3 ? this.mSortOrder != R.id.sort_order_size : false;
            findItem.setVisible(z2);
            findItem = this.mOptionsMenu.findItem(R.id.show_system);
            z2 = !this.mShowSystem ? this.mListType != 5 : false;
            findItem.setVisible(z2);
            MenuItem findItem2 = this.mOptionsMenu.findItem(R.id.hide_system);
            if (this.mShowSystem && this.mListType != 5) {
                z = true;
            }
            findItem2.setVisible(z);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.advanced:
                ((SettingsActivity) getActivity()).startPreferencePanel(AdvancedAppSettings.class.getName(), null, R.string.configure_apps, null, this, 2);
                return true;
            case R.id.show_system:
            case R.id.hide_system:
                boolean z;
                if (this.mShowSystem) {
                    z = false;
                } else {
                    z = true;
                }
                this.mShowSystem = z;
                this.mApplications.rebuild(false);
                break;
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                this.mSortOrder = menuId;
                if (this.mApplications != null) {
                    this.mApplications.rebuild(this.mSortOrder);
                    break;
                }
                break;
            case R.id.reset_app_preferences:
                this.mResetAppsHelper.buildResetDialog();
                return true;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (this.mApplications != null && this.mApplications.getCount() > position) {
            AppEntry entry = this.mApplications.getAppEntry(position);
            this.mCurrentPkgName = entry.info.packageName;
            this.mCurrentUid = entry.info.uid;
            startApplicationDetailsActivity();
        }
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        this.mFilter = this.mFilterAdapter.getFilter(position);
        this.mApplications.setFilter(this.mFilter);
        if (DEBUG) {
            Log.d("ManageApplications", "Selecting filter " + this.mFilter);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void updateView() {
        updateOptionsMenu();
        Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    public void setHasDisabled(boolean hasDisabledApps) {
        if (this.mListType != 5) {
            this.mFilterAdapter.setFilterEnabled(3, hasDisabledApps);
            this.mFilterAdapter.setFilterEnabled(4, hasDisabledApps);
        }
    }
}
