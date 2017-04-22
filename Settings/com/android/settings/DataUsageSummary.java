package com.android.settings;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.settings.drawable.InsetBoundsDrawable;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.net.SummaryForAllUidLoader;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartDataUsageView.DataUsageChartListener;
import com.android.settings.widget.ChartNetworkSeriesView;
import com.google.android.collect.Lists;
import com.mediatek.datausage.OverViewTabAdapter;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.IDataUsageSummaryExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import libcore.util.Objects;

public class DataUsageSummary extends HighlightingFragment implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.data_usage_summary_title);
            data.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.key = "data_usage_enable_mobile";
            data.title = res.getString(R.string.data_usage_enable_mobile);
            data.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.key = "data_usage_disable_mobile_limit";
            data.title = res.getString(R.string.data_usage_disable_mobile_limit);
            data.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.key = "data_usage_cycle";
            data.title = res.getString(R.string.data_usage_cycle);
            data.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data);
            return result;
        }
    };
    private static final boolean SHOW_OVERVIEW_TAB;
    private static IDataUsageSummaryExt mDataUsageSummaryExt;
    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());
    private DataUsageAdapter mAdapter;
    private TextView mAppBackground;
    private View mAppDetail;
    private TextView mAppForeground;
    private ImageView mAppIcon;
    private Switch mAppRestrict;
    private OnClickListener mAppRestrictListener = new C00955();
    private View mAppRestrictView;
    private Button mAppSettings;
    private Intent mAppSettingsIntent;
    private LinearLayout mAppSwitches;
    private ViewGroup mAppTitles;
    private TextView mAppTotal;
    private boolean mBinding;
    private ChartDataUsageView mChart;
    private ChartData mChartData;
    private final LoaderCallbacks<ChartData> mChartDataCallbacks = new C00988();
    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        public void onWarningChanged() {
            DataUsageSummary.this.setPolicyWarningBytes(DataUsageSummary.this.mChart.getWarningBytes());
        }

        public void onLimitChanged() {
            DataUsageSummary.this.setPolicyLimitBytes(DataUsageSummary.this.mChart.getLimitBytes());
            DataUsageSummary.this.updateBody();
        }

        public void requestWarningEdit() {
            WarningEditorFragment.show(DataUsageSummary.this);
        }

        public void requestLimitEdit() {
            LimitEditorFragment.show(DataUsageSummary.this);
        }
    };
    public boolean mClickFlag = false;
    private AppItem mCurrentApp = null;
    private SubscriptionInfo mCurrentSir = null;
    private String mCurrentTab = null;
    private CycleAdapter mCycleAdapter;
    private OnItemSelectedListener mCycleListener = new C00977();
    private Spinner mCycleSpinner;
    private TextView mCycleSummary;
    private View mCycleView;
    private Switch mDataEnabled;
    private OnClickListener mDataEnabledListener = new C00933();
    private boolean mDataEnabledSupported;
    private View mDataEnabledView;
    private final BroadcastReceiver mDateStateReceiver = new DataStateBroadcastReceiver();
    private ChartNetworkSeriesView mDetailedSeries;
    private Switch mDisableAtLimit;
    private OnClickListener mDisableAtLimitListener = new C00944();
    private boolean mDisableAtLimitSupported;
    private View mDisableAtLimitView;
    private View mDisclaimer;
    private TextView mEmpty;
    private TabContentFactory mEmptyTabContent = new C00911();
    private boolean mFlagClicked = false;
    private ViewGroup mHeader;
    private int mInsetSide = 0;
    private String mIntentTab = null;
    private boolean mIsAirplaneModeOn;
    private OnItemClickListener mListListener = new C00966();
    private ListView mListView;
    private MenuItem mMenuCellularNetworks;
    private MenuItem mMenuRestrictBackground;
    private MenuItem mMenuShowEthernet;
    private MenuItem mMenuShowWifi;
    private MenuItem mMenuSimCards;
    private final Map<String, Boolean> mMobileDataEnabled = new HashMap();
    private Map<Integer, String> mMobileTagMap;
    private INetworkManagementService mNetworkService;
    private LinearLayout mNetworkSwitches;
    private ViewGroup mNetworkSwitchesContainer;
    private ExpandableListView mOverViewExpList;
    private OverViewTabAdapter mOverviewAdapter;
    private NetworkPolicyEditor mPolicyEditor;
    private NetworkPolicyManager mPolicyManager;
    private SharedPreferences mPrefs;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("DataUsage", "onReceive broadcast , action =  " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                DataUsageSummary.this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                DataUsageSummary.this.updateBody();
            } else if (action.equals("com.mediatek.server.action.ACTION_POLICY_CREATED")) {
                DataUsageSummary.this.mPolicyEditor.read();
                DataUsageSummary.this.updateBody();
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                DataUsageSummary.this.updateDataUiForCdmaCard(intent);
            } else if (DataUsageSummary.this.isSimSwitchAction(action)) {
                DataUsageSummary.this.updatePolicy(false);
            }
        }
    };
    private long mRestoreCycleEnd = 0;
    private long mRestoreCycleStart = 0;
    private ChartNetworkSeriesView mSeries;
    private String mShowAppImmediatePkg;
    private boolean mShowEthernet = false;
    private boolean mShowWifi = false;
    private SimHotSwapHandler mSimHotSwapHandler;
    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private View mStupidPadding;
    private List<SubscriptionInfo> mSubInfoList;
    private SubscriptionManager mSubscriptionManager;
    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new C00999();
    private TabHost mTabHost;
    private OnTabChangeListener mTabListener = new C00922();
    private TabWidget mTabWidget;
    private ViewGroup mTabsContainer;
    private TelephonyManager mTelephonyManager;
    private NetworkTemplate mTemplate;
    private UidDetailProvider mUidDetailProvider;
    private String myCurrentTab = null;

    class C00911 implements TabContentFactory {
        C00911() {
        }

        public View createTabContent(String tag) {
            return new View(DataUsageSummary.this.mTabHost.getContext());
        }
    }

    class C00922 implements OnTabChangeListener {
        C00922() {
        }

        public void onTabChanged(String tabId) {
            Log.d("DataUsage", "onTabChanged, tabId: " + tabId);
            DataUsageSummary.this.updateBody();
        }
    }

    class C00933 implements OnClickListener {
        C00933() {
        }

        public void onClick(View v) {
            if (!DataUsageSummary.this.mBinding) {
                boolean enabled = !DataUsageSummary.this.mDataEnabled.isChecked();
                String currentTab = DataUsageSummary.this.mCurrentTab;
                Log.d("DataUsage", "mDataEnabledListener... dataEanbled: " + enabled + "mCurrentTab: " + DataUsageSummary.this.mCurrentTab);
                if (DataUsageSummary.isMobileTab(currentTab)) {
                    MetricsLogger.action(DataUsageSummary.this.getContext(), 178, enabled);
                    if (enabled) {
                        if (Utils.showSimCardTile(DataUsageSummary.this.getActivity())) {
                            DataUsageSummary.this.handleMultiSimDataDialog();
                        } else {
                            DataUsageSummary.this.setMobileDataEnabled(DataUsageSummary.this.getSubId(currentTab), true);
                        }
                    } else if (DataUsageSummary.mDataUsageSummaryExt.needToShowDialog()) {
                        ConfirmDataDisableFragment.show(DataUsageSummary.this, DataUsageSummary.this.getSubId(DataUsageSummary.this.mCurrentTab));
                    } else {
                        DataUsageSummary.this.setMobileDataEnabled(DataUsageSummary.this.getSubId(currentTab), false);
                    }
                    if (currentTab == null || currentTab.equals(DataUsageSummary.this.myCurrentTab)) {
                        DataUsageSummary.this.mClickFlag = false;
                    } else {
                        DataUsageSummary.this.myCurrentTab = DataUsageSummary.this.mCurrentTab;
                        DataUsageSummary.this.mClickFlag = true;
                    }
                }
                DataUsageSummary.this.updatePolicy(false);
            }
        }
    }

    class C00944 implements OnClickListener {
        C00944() {
        }

        public void onClick(View v) {
            if (!DataUsageSummary.this.mDisableAtLimit.isChecked()) {
                ConfirmLimitFragment.show(DataUsageSummary.this);
            } else {
                DataUsageSummary.this.setPolicyLimitBytes(-1);
            }
        }
    }

    class C00955 implements OnClickListener {
        C00955() {
        }

        public void onClick(View v) {
            if (!DataUsageSummary.this.mAppRestrict.isChecked()) {
                ConfirmAppRestrictFragment.show(DataUsageSummary.this);
            } else {
                DataUsageSummary.this.setAppRestrictBackground(false);
            }
        }
    }

    class C00966 implements OnItemClickListener {
        C00966() {
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Context context = view.getContext();
            AppItem app = (AppItem) parent.getItemAtPosition(position);
            if (DataUsageSummary.this.mUidDetailProvider != null && app != null) {
                AppDetailsFragment.show(DataUsageSummary.this, app, DataUsageSummary.this.mUidDetailProvider.getUidDetail(app.key, true).label);
            }
        }
    }

    class C00977 implements OnItemSelectedListener {
        C00977() {
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (cycle instanceof CycleChangeItem) {
                CycleEditorFragment.show(DataUsageSummary.this);
                DataUsageSummary.this.mCycleSpinner.setSelection(0);
                return;
            }
            Log.d("DataUsage", "showing cycle " + cycle + ", start=" + cycle.start + ", end=" + cycle.end + "]");
            DataUsageSummary.this.mChart.setVisibleRange(cycle.start, cycle.end);
            DataUsageSummary.this.updateDetailData();
        }

        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    class C00988 implements LoaderCallbacks<ChartData> {
        C00988() {
        }

        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(DataUsageSummary.this.getActivity(), DataUsageSummary.this.mStatsSession, args);
        }

        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            Log.d("DataUsage", "onLoadFinished, mChartData.network: " + data.network + "mChartData.detail" + data.detail);
            DataUsageSummary.this.mChartData = data;
            DataUsageSummary.this.mChart.bindNetworkStats(DataUsageSummary.this.mChartData.network);
            DataUsageSummary.this.mChart.bindDetailNetworkStats(DataUsageSummary.this.mChartData.detail);
            DataUsageSummary.this.updatePolicy(true);
            DataUsageSummary.this.updateAppDetail();
            if (DataUsageSummary.this.mChartData.detail != null) {
                DataUsageSummary.this.mListView.smoothScrollToPosition(0);
            }
        }

        public void onLoaderReset(Loader<ChartData> loader) {
            DataUsageSummary.this.mChartData = null;
            DataUsageSummary.this.mChart.bindNetworkStats(null);
            DataUsageSummary.this.mChart.bindDetailNetworkStats(null);
        }
    }

    class C00999 implements LoaderCallbacks<NetworkStats> {
        C00999() {
        }

        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(DataUsageSummary.this.getActivity(), DataUsageSummary.this.mStatsSession, args);
        }

        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            DataUsageSummary.this.mAdapter.bindStats(data, DataUsageSummary.this.mPolicyManager.getUidsWithPolicy(1));
            updateEmptyVisible();
        }

        public void onLoaderReset(Loader<NetworkStats> loader) {
            DataUsageSummary.this.mAdapter.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            int i;
            int i2 = 0;
            boolean isEmpty = DataUsageSummary.this.mAdapter.isEmpty() && !DataUsageSummary.this.isAppDetailMode();
            TextView -get11 = DataUsageSummary.this.mEmpty;
            if (isEmpty) {
                i = 0;
            } else {
                i = 8;
            }
            -get11.setVisibility(i);
            View -get17 = DataUsageSummary.this.mStupidPadding;
            if (!isEmpty) {
                i2 = 8;
            }
            -get17.setVisibility(i2);
        }
    }

    public static class AppDetailsFragment extends Fragment {
        public static void show(DataUsageSummary parent, AppItem app, CharSequence label) {
            show(parent, app, label, true);
        }

        public static void show(DataUsageSummary parent, AppItem app, CharSequence label, boolean addToBack) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("app", app);
                AppDetailsFragment fragment = new AppDetailsFragment();
                fragment.setArguments(args);
                fragment.setTargetFragment(parent, 0);
                FragmentTransaction ft = parent.getFragmentManager().beginTransaction();
                ft.add(fragment, "appDetails");
                if (addToBack) {
                    ft.addToBackStack("appDetails");
                }
                ft.setBreadCrumbTitle(parent.getResources().getString(R.string.data_usage_app_summary_title));
                ft.commitAllowingStateLoss();
            }
        }

        public void onStart() {
            super.onStart();
            DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = (AppItem) getArguments().getParcelable("app");
            target.updateBody();
        }

        public void onStop() {
            super.onStop();
            DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = null;
            target.updateBody();
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case 16908332:
                    getFragmentManager().popBackStack();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        public void onDestroy() {
            super.onDestroy();
            DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            if (target.isResumed() && target.mTabHost.getCurrentTabTag().equals("Overview")) {
                Log.d("DataUsage", "Overview tab and finish activity");
                getActivity().finish();
            }
        }
    }

    public static class AppItem implements Comparable<AppItem>, Parcelable {
        public static final Creator<AppItem> CREATOR = new C01001();
        public int category;
        public final int key;
        public boolean restricted;
        public long total;
        public SparseBooleanArray uids;

        static class C01001 implements Creator<AppItem> {
            C01001() {
            }

            public AppItem createFromParcel(Parcel in) {
                return new AppItem(in);
            }

            public AppItem[] newArray(int size) {
                return new AppItem[size];
            }
        }

        public AppItem() {
            this.uids = new SparseBooleanArray();
            this.key = 0;
        }

        public AppItem(int key) {
            this.uids = new SparseBooleanArray();
            this.key = key;
        }

        public AppItem(Parcel parcel) {
            this.uids = new SparseBooleanArray();
            this.key = parcel.readInt();
            this.uids = parcel.readSparseBooleanArray();
            this.total = parcel.readLong();
        }

        public void addUid(int uid) {
            this.uids.put(uid, true);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.key);
            dest.writeSparseBooleanArray(this.uids);
            dest.writeLong(this.total);
        }

        public int describeContents() {
            return 0;
        }

        public int compareTo(AppItem another) {
            int comparison = Integer.compare(this.category, another.category);
            if (comparison == 0) {
                return Long.compare(another.total, this.total);
            }
            return comparison;
        }
    }

    public static class ConfirmAppRestrictFragment extends DialogFragment {

        class C01011 implements DialogInterface.OnClickListener {
            C01011() {
            }

            public void onClick(DialogInterface dialog, int which) {
                DataUsageSummary target = (DataUsageSummary) ConfirmAppRestrictFragment.this.getTargetFragment();
                if (target != null) {
                    target.setAppRestrictBackground(true);
                }
            }
        }

        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                ConfirmAppRestrictFragment dialog = new ConfirmAppRestrictFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmAppRestrict");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            String dialogTitle = DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_dialog_title), IDataUsageSummaryExt.TAG_BG_DATA_APP_DIALOG_TITLE);
            String dialogMessage = DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_dialog), IDataUsageSummaryExt.TAG_BG_DATA_APP_DIALOG_MESSAGE);
            builder.setTitle(dialogTitle);
            builder.setMessage(dialogMessage);
            builder.setPositiveButton(17039370, new C01011());
            builder.setNegativeButton(17039360, null);
            return builder.create();
        }
    }

    public static class ConfirmDataDisableFragment extends DialogFragment {
        static int mSubId;

        class C01021 implements DialogInterface.OnClickListener {
            C01021() {
            }

            public void onClick(DialogInterface dialog, int which) {
                DataUsageSummary target = (DataUsageSummary) ConfirmDataDisableFragment.this.getTargetFragment();
                if (target != null) {
                    target.setMobileDataEnabled(ConfirmDataDisableFragment.mSubId, false);
                }
            }
        }

        public static void show(DataUsageSummary parent, int subId) {
            mSubId = subId;
            if (parent.isAdded()) {
                ConfirmDataDisableFragment dialog = new ConfirmDataDisableFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmDataDisable");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            builder.setMessage(R.string.data_usage_disable_mobile);
            builder.setPositiveButton(17039370, new C01021());
            builder.setNegativeButton(17039360, null);
            return builder.create();
        }
    }

    public static class ConfirmLimitFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                NetworkPolicy policy = parent.mPolicyEditor.getPolicy(parent.mTemplate);
                if (policy != null) {
                    CharSequence message;
                    long limitBytes;
                    Resources res = parent.getResources();
                    long minLimitBytes = (long) (((float) policy.warningBytes) * 1.2f);
                    String currentTab = parent.mCurrentTab;
                    if ("3g".equals(currentTab)) {
                        message = res.getString(R.string.data_usage_limit_dialog_mobile);
                        limitBytes = Math.max(5368709120L, minLimitBytes);
                    } else if ("4g".equals(currentTab)) {
                        message = res.getString(R.string.data_usage_limit_dialog_mobile);
                        limitBytes = Math.max(5368709120L, minLimitBytes);
                    } else if (DataUsageSummary.isMobileTab(currentTab)) {
                        message = res.getString(R.string.data_usage_limit_dialog_mobile);
                        limitBytes = Math.max(5368709120L, minLimitBytes);
                    } else {
                        throw new IllegalArgumentException("unknown current tab: " + currentTab);
                    }
                    Bundle args = new Bundle();
                    args.putCharSequence("message", message);
                    args.putLong("limitBytes", limitBytes);
                    ConfirmLimitFragment dialog = new ConfirmLimitFragment();
                    dialog.setArguments(args);
                    dialog.setTargetFragment(parent, 0);
                    dialog.show(parent.getFragmentManager(), "confirmLimit");
                }
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            CharSequence message = getArguments().getCharSequence("message");
            final long limitBytes = getArguments().getLong("limitBytes");
            Builder builder = new Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(message);
            builder.setPositiveButton(17039370, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    DataUsageSummary target = (DataUsageSummary) ConfirmLimitFragment.this.getTargetFragment();
                    if (target != null) {
                        target.setPolicyLimitBytes(limitBytes);
                    }
                }
            });
            return builder.create();
        }
    }

    public static class ConfirmRestrictFragment extends DialogFragment {

        class C01041 implements DialogInterface.OnClickListener {
            C01041() {
            }

            public void onClick(DialogInterface dialog, int which) {
                DataUsageSummary target = (DataUsageSummary) ConfirmRestrictFragment.this.getTargetFragment();
                if (target != null) {
                    target.setRestrictBackground(true);
                }
            }
        }

        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                ConfirmRestrictFragment dialog = new ConfirmRestrictFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmRestrict");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            Builder builder = new Builder(context);
            builder.setTitle(R.string.data_usage_restrict_background_title);
            builder.setTitle(DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_restrict_background_title), IDataUsageSummaryExt.TAG_BG_DATA_APP_DIALOG_TITLE));
            if (Utils.hasMultipleUsers(context)) {
                builder.setMessage(R.string.data_usage_restrict_background_multiuser);
            } else {
                builder.setMessage(DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_restrict_background), IDataUsageSummaryExt.TAG_BG_DATA_MENU_DIALOG_MESSAGE));
            }
            builder.setPositiveButton(17039370, new C01041());
            builder.setNegativeButton(17039360, null);
            return builder.create();
        }
    }

    public static class CycleAdapter extends ArrayAdapter<CycleItem> {
        private final CycleChangeItem mChangeItem;
        private boolean mChangePossible = false;
        private boolean mChangeVisible = false;

        public CycleAdapter(Context context) {
            super(context, R.layout.data_usage_cycle_item);
            setDropDownViewResource(R.layout.data_usage_cycle_item_dropdown);
            this.mChangeItem = new CycleChangeItem(context);
        }

        public void setChangePossible(boolean possible) {
            this.mChangePossible = possible;
            updateChange();
        }

        public void setChangeVisible(boolean visible) {
            this.mChangeVisible = visible;
            updateChange();
        }

        private void updateChange() {
            remove(this.mChangeItem);
            if (this.mChangePossible && this.mChangeVisible) {
                add(this.mChangeItem);
            }
        }

        public int findNearestPosition(CycleItem target) {
            if (target != null) {
                for (int i = getCount() - 1; i >= 0; i--) {
                    CycleItem item = (CycleItem) getItem(i);
                    if (!(item instanceof CycleChangeItem) && item.compareTo(target) >= 0) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    public static class CycleItem implements Comparable<CycleItem> {
        public long end;
        public CharSequence label;
        public long start;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = DataUsageSummary.formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        public String toString() {
            return this.label.toString();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof CycleItem)) {
                return false;
            }
            CycleItem another = (CycleItem) o;
            if (this.start == another.start && this.end == another.end) {
                z = true;
            }
            return z;
        }

        public int compareTo(CycleItem another) {
            return Long.compare(this.start, another.start);
        }
    }

    public static class CycleChangeItem extends CycleItem {
        public CycleChangeItem(Context context) {
            super(context.getString(R.string.data_usage_change_cycle));
        }
    }

    public static class CycleEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                CycleEditorFragment dialog = new CycleEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "cycleEditor");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            Builder builder = new Builder(context);
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.data_usage_cycle_editor, null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            int cycleDay = editor.getPolicyCycleDay(template);
            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(cycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);
            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cycleDayPicker.clearFocus();
                    editor.setPolicyCycleDay(template, cycleDayPicker.getValue(), new Time().timezone);
                    target.updatePolicy(true);
                }
            });
            return builder.create();
        }
    }

    private class DataStateBroadcastReceiver extends BroadcastReceiver {
        private DataStateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            DataUsageSummary.this.updateBody();
            if (intent.getAction().equals("android.intent.action.ACTION_MOBILE_DATA_ENABLE")) {
                int state = intent.getIntExtra("reason", -1);
                if (state == 1 && DataUsageSummary.isMobileTab(DataUsageSummary.this.mCurrentTab)) {
                    if (DataUsageSummary.this.mClickFlag) {
                        DataUsageSummary.this.myCurrentTab = DataUsageSummary.this.mCurrentTab;
                        return;
                    }
                    DataUsageSummary.this.mBinding = true;
                    DataUsageSummary.this.mDataEnabled.setChecked(true);
                    DataUsageSummary.this.setMobileDataEnabled(DataUsageSummary.this.getSubId(DataUsageSummary.this.mCurrentTab), true);
                    DataUsageSummary.this.mBinding = false;
                    DataUsageSummary.this.myCurrentTab = DataUsageSummary.this.mCurrentTab;
                } else if (state == 0 && DataUsageSummary.isMobileTab(DataUsageSummary.this.mCurrentTab)) {
                    if (DataUsageSummary.this.mClickFlag) {
                        DataUsageSummary.this.myCurrentTab = DataUsageSummary.this.mCurrentTab;
                        DataUsageSummary.this.mClickFlag = false;
                        return;
                    }
                    DataUsageSummary.this.mBinding = true;
                    DataUsageSummary.this.mDataEnabled.setChecked(false);
                    DataUsageSummary.this.setMobileDataEnabled(DataUsageSummary.this.getSubId(DataUsageSummary.this.mCurrentTab), false);
                    DataUsageSummary.this.mBinding = false;
                }
                DataUsageSummary.this.updatePolicy(false);
            }
        }
    }

    public static class DataUsageAdapter extends BaseAdapter {
        private final Context mContext;
        private final int mInsetSide;
        private ArrayList<AppItem> mItems = Lists.newArrayList();
        private long mLargest;
        private final UidDetailProvider mProvider;
        private final UserManager mUm;

        public DataUsageAdapter(Context context, UserManager userManager, UidDetailProvider provider, int insetSide) {
            this.mContext = context;
            this.mProvider = (UidDetailProvider) Preconditions.checkNotNull(provider);
            this.mInsetSide = insetSide;
            this.mUm = userManager;
        }

        public void bindStats(NetworkStats stats, int[] restrictedUids) {
            int i;
            this.mItems.clear();
            this.mLargest = 0;
            int currentUserId = ActivityManager.getCurrentUser();
            List<UserHandle> profiles = this.mUm.getUserProfiles();
            SparseArray<AppItem> knownItems = new SparseArray();
            Entry entry = null;
            int size = stats != null ? stats.size() : 0;
            for (i = 0; i < size; i++) {
                int collapseKey;
                int category;
                entry = stats.getValues(i, entry);
                int uid = entry.uid;
                int userId = UserHandle.getUserId(uid);
                if (UserHandle.isApp(uid)) {
                    if (profiles.contains(new UserHandle(userId))) {
                        if (userId != currentUserId) {
                            accumulate(UidDetailProvider.buildKeyForUser(userId), knownItems, entry, 0);
                        }
                        collapseKey = uid;
                        category = 2;
                    } else if (this.mUm.getUserInfo(userId) == null) {
                        collapseKey = -4;
                        category = 2;
                    } else {
                        collapseKey = UidDetailProvider.buildKeyForUser(userId);
                        category = 0;
                    }
                } else if (uid == -4 || uid == -5) {
                    collapseKey = uid;
                    category = 2;
                } else {
                    collapseKey = 1000;
                    category = 2;
                }
                accumulate(collapseKey, knownItems, entry, category);
            }
            for (int uid2 : restrictedUids) {
                if (profiles.contains(new UserHandle(UserHandle.getUserId(uid2)))) {
                    AppItem item = (AppItem) knownItems.get(uid2);
                    if (item == null) {
                        item = new AppItem(uid2);
                        item.total = -1;
                        this.mItems.add(item);
                        knownItems.put(item.key, item);
                    }
                    item.restricted = true;
                }
            }
            if (!this.mItems.isEmpty()) {
                AppItem title = new AppItem();
                title.category = 1;
                this.mItems.add(title);
            }
            PackageManager pm = this.mContext.getPackageManager();
            for (i = 0; i < this.mItems.size(); i++) {
                if (this.mItems.get(i) != null) {
                    String label = pm.getNameForUid(((AppItem) this.mItems.get(i)).key);
                    if (label != null && ("com.android.cleanprocesstool".equals(label) || "com.android.settings".equals(label) || "com.android.launcher3".equals(label) || "com.android.Pet.mediaproxy".equals(label) || "com.android.systemui".equals(label))) {
                        this.mItems.remove(i);
                        knownItems.remove(((AppItem) this.mItems.get(i)).key);
                    }
                }
            }
            Collections.sort(this.mItems);
            notifyDataSetChanged();
        }

        private void accumulate(int collapseKey, SparseArray<AppItem> knownItems, Entry entry, int itemCategory) {
            int uid = entry.uid;
            AppItem item = (AppItem) knownItems.get(collapseKey);
            if (item == null) {
                item = new AppItem(collapseKey);
                item.category = itemCategory;
                this.mItems.add(item);
                knownItems.put(item.key, item);
            }
            item.addUid(uid);
            item.total += entry.rxBytes + entry.txBytes;
            if (this.mLargest < item.total) {
                this.mLargest = item.total;
            }
        }

        public int getCount() {
            return this.mItems.size();
        }

        public Object getItem(int position) {
            return this.mItems.get(position);
        }

        public long getItemId(int position) {
            return (long) ((AppItem) this.mItems.get(position)).key;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public int getItemViewType(int position) {
            if (((AppItem) this.mItems.get(position)).category == 1) {
                return 1;
            }
            return 0;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            if (position > this.mItems.size()) {
                throw new ArrayIndexOutOfBoundsException();
            } else if (getItemViewType(position) == 0) {
                return true;
            } else {
                return false;
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            AppItem item = (AppItem) this.mItems.get(position);
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (getItemViewType(position) == 1) {
                if (convertView == null) {
                    convertView = Utils.inflateCategoryHeader(inflater, parent);
                }
                ((TextView) convertView.findViewById(16908310)).setText(R.string.data_usage_app);
            } else {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.data_usage_item, parent, false);
                    inflater.inflate(R.layout.widget_progress_bar, (ViewGroup) convertView.findViewById(16908312));
                    if (this.mInsetSide > 0) {
                        convertView.setPaddingRelative(this.mInsetSide, 0, this.mInsetSide, 0);
                    }
                }
                Context context = parent.getContext();
                TextView summary = (TextView) convertView.findViewById(16908304);
                ProgressBar progress = (ProgressBar) convertView.findViewById(16908301);
                UidDetailTask.bindView(this.mProvider, item, convertView);
                if (!item.restricted || item.total > 0) {
                    summary.setText(android.text.format.Formatter.formatFileSize(context, item.total));
                    progress.setVisibility(0);
                } else {
                    summary.setText(R.string.data_usage_app_restricted);
                    progress.setVisibility(8);
                }
                progress.setProgress(this.mLargest != 0 ? (int) ((item.total * 100) / this.mLargest) : 0);
            }
            return convertView;
        }
    }

    public static class DeniedRestrictFragment extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            String deniedRestrictBgDataTitle = DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_background), IDataUsageSummaryExt.TAG_BG_DATA_SWITCH);
            String deniedRestrictBgDataMessage = DataUsageSummary.mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_restrict_denied_dialog), IDataUsageSummaryExt.TAG_BG_DATA_RESTRICT_DENY_MESSAGE);
            builder.setTitle(deniedRestrictBgDataTitle);
            builder.setMessage(deniedRestrictBgDataMessage);
            builder.setPositiveButton(17039370, null);
            return builder.create();
        }
    }

    public static class LimitEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                LimitEditorFragment dialog = new LimitEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "limitEditor");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            Builder builder = new Builder(context);
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            long warningBytes = editor.getPolicyWarningBytes(template);
            long limitBytes = editor.getPolicyLimitBytes(template);
            bytesPicker.setMaxValue(Integer.MAX_VALUE);
            if (warningBytes == -1 || limitBytes <= 0) {
                bytesPicker.setMinValue(0);
            } else {
                bytesPicker.setMinValue(((int) (warningBytes / 1048576)) + 1);
            }
            bytesPicker.setValue((int) (limitBytes / 1048576));
            bytesPicker.setWrapSelectorWheel(false);
            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bytesPicker.clearFocus();
                    editor.setPolicyLimitBytes(template, ((long) bytesPicker.getValue()) * 1048576);
                    target.updatePolicy(false);
                }
            });
            return builder.create();
        }
    }

    private static class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
        private final AppItem mItem;
        private final UidDetailProvider mProvider;
        private final View mTarget;

        private UidDetailTask(UidDetailProvider provider, AppItem item, View target) {
            this.mProvider = (UidDetailProvider) Preconditions.checkNotNull(provider);
            this.mItem = (AppItem) Preconditions.checkNotNull(item);
            this.mTarget = (View) Preconditions.checkNotNull(target);
        }

        public static void bindView(UidDetailProvider provider, AppItem item, View target) {
            UidDetailTask existing = (UidDetailTask) target.getTag();
            if (existing != null) {
                existing.cancel(false);
            }
            UidDetail cachedDetail = provider.getUidDetail(item.key, false);
            if (cachedDetail != null) {
                bindView(cachedDetail, target);
            } else {
                target.setTag(new UidDetailTask(provider, item, target).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]));
            }
        }

        private static void bindView(UidDetail detail, View target) {
            ImageView icon = (ImageView) target.findViewById(16908294);
            TextView title = (TextView) target.findViewById(16908310);
            if (detail != null) {
                icon.setImageDrawable(detail.icon);
                title.setText(detail.label);
                title.setContentDescription(detail.contentDescription);
                return;
            }
            icon.setImageDrawable(null);
            title.setText(null);
        }

        protected void onPreExecute() {
            bindView(null, this.mTarget);
        }

        protected UidDetail doInBackground(Void... params) {
            return this.mProvider.getUidDetail(this.mItem.key, true);
        }

        protected void onPostExecute(UidDetail result) {
            bindView(result, this.mTarget);
        }
    }

    public static class WarningEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                WarningEditorFragment dialog = new WarningEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "warningEditor");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            Builder builder = new Builder(context);
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            long warningBytes = editor.getPolicyWarningBytes(template);
            long limitBytes = editor.getPolicyLimitBytes(template);
            bytesPicker.setMinValue(0);
            if (limitBytes == -1) {
                bytesPicker.setMaxValue(Integer.MAX_VALUE);
            } else if (limitBytes == 0) {
                bytesPicker.setMaxValue(0);
            } else {
                bytesPicker.setMaxValue(((int) (limitBytes / 1048576)) - 1);
            }
            bytesPicker.setValue((int) (warningBytes / 1048576));
            bytesPicker.setWrapSelectorWheel(false);
            builder.setTitle(R.string.data_usage_warning_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bytesPicker.clearFocus();
                    editor.setPolicyWarningBytes(template, ((long) bytesPicker.getValue()) * 1048576);
                    target.updatePolicy(false);
                }
            });
            return builder.create();
        }
    }

    protected int getMetricsCategory() {
        return 37;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        this.mNetworkService = Stub.asInterface(ServiceManager.getService("network_management"));
        this.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
        this.mPolicyManager = NetworkPolicyManager.from(context);
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mPrefs = getActivity().getSharedPreferences("data_usage", 0);
        this.mPolicyEditor = new NetworkPolicyEditor(this.mPolicyManager);
        this.mPolicyEditor.read();
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        this.mMobileTagMap = initMobileTabTag(this.mSubInfoList);
        try {
            if (!this.mNetworkService.isBandwidthControlEnabled()) {
                Log.w("DataUsage", "No bandwidth control; leaving");
                getActivity().finish();
            }
        } catch (RemoteException e) {
            Log.w("DataUsage", "No bandwidth control; leaving");
            getActivity().finish();
        }
        try {
            this.mStatsSession = this.mStatsService.openSession();
        } catch (RemoteException e2) {
            throw new RuntimeException(e2);
        } catch (IllegalStateException e3) {
            Log.d("DataUsage", "onCreate, exception happen: " + e3 + " , so finish current activity");
            getActivity().finish();
        }
        this.mShowWifi = this.mPrefs.getBoolean("show_wifi", false);
        this.mShowEthernet = this.mPrefs.getBoolean("show_ethernet", false);
        if (!hasReadyMobileRadio(context)) {
            this.mShowWifi = true;
            this.mShowEthernet = true;
        }
        this.mUidDetailProvider = new UidDetailProvider(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mShowAppImmediatePkg = arguments.getString("showAppImmediatePkg");
        }
        setHasOptionsMenu(true);
        if (SHOW_OVERVIEW_TAB) {
            this.mOverviewAdapter = new OverViewTabAdapter(getActivity(), this.mStatsSession);
        }
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            public void onSimHotSwap() {
                if (DataUsageSummary.this.getActivity() != null) {
                    Log.d("DataUsage", "onSimHotSwap, finish Activity~~");
                    DataUsageSummary.this.getActivity().finish();
                }
            }
        });
        mDataUsageSummaryExt = UtilsExt.getDataUsageSummaryPlugin(getActivity().getApplicationContext());
        mDataUsageSummaryExt.create(this.mMobileDataEnabled);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.data_usage_summary, container, false);
        this.mTabHost = (TabHost) view.findViewById(16908306);
        this.mTabsContainer = (ViewGroup) view.findViewById(R.id.tabs_container);
        this.mTabWidget = (TabWidget) view.findViewById(16908307);
        this.mListView = (ListView) view.findViewById(16908298);
        if (this.mListView.getScrollBarStyle() == 33554432) {
        }
        this.mInsetSide = 0;
        Utils.prepareCustomPreferencesList(container, view, this.mListView, false);
        this.mTabHost.setup();
        this.mTabHost.setOnTabChangedListener(this.mTabListener);
        this.mHeader = (ViewGroup) inflater.inflate(R.layout.data_usage_header, this.mListView, false);
        this.mHeader.setClickable(true);
        this.mListView.addHeaderView(new View(context), null, true);
        this.mListView.addHeaderView(this.mHeader, null, true);
        this.mListView.setItemsCanFocus(true);
        if (SHOW_OVERVIEW_TAB) {
            createViewForOverviewTab(context, view);
        }
        if (this.mInsetSide > 0) {
            insetListViewDrawables(this.mListView, this.mInsetSide);
            this.mHeader.setPaddingRelative(this.mInsetSide, 0, this.mInsetSide, 0);
        }
        this.mNetworkSwitchesContainer = (ViewGroup) this.mHeader.findViewById(R.id.network_switches_container);
        this.mNetworkSwitches = (LinearLayout) this.mHeader.findViewById(R.id.network_switches);
        this.mDataEnabled = new Switch(inflater.getContext());
        this.mDataEnabled.setClickable(false);
        this.mDataEnabled.setFocusable(false);
        this.mDataEnabledView = inflatePreference(inflater, this.mNetworkSwitches, this.mDataEnabled);
        this.mDataEnabledView.setTag(R.id.preference_highlight_key, "data_usage_enable_mobile");
        this.mDataEnabledView.setClickable(true);
        this.mDataEnabledView.setFocusable(true);
        this.mDataEnabledView.setOnClickListener(this.mDataEnabledListener);
        updateScreenEnableState();
        mDataUsageSummaryExt.setDataEnableClickListener(getActivity(), this.mDataEnabledView, this.mDataEnabled, this.mDataEnabledListener);
        this.mNetworkSwitches.addView(this.mDataEnabledView);
        this.mDisableAtLimit = new Switch(inflater.getContext());
        this.mDisableAtLimit.setClickable(false);
        this.mDisableAtLimit.setFocusable(false);
        this.mDisableAtLimitView = inflatePreference(inflater, this.mNetworkSwitches, this.mDisableAtLimit);
        this.mDisableAtLimitView.setTag(R.id.preference_highlight_key, "data_usage_disable_mobile_limit");
        this.mDisableAtLimitView.setClickable(true);
        this.mDisableAtLimitView.setFocusable(true);
        this.mDisableAtLimitView.setOnClickListener(this.mDisableAtLimitListener);
        this.mNetworkSwitches.addView(this.mDisableAtLimitView);
        this.mCycleView = inflater.inflate(R.layout.data_usage_cycles, this.mNetworkSwitches, false);
        this.mCycleView.setTag(R.id.preference_highlight_key, "data_usage_cycle");
        this.mCycleSpinner = (Spinner) this.mCycleView.findViewById(R.id.cycles_spinner);
        this.mCycleAdapter = new CycleAdapter(context);
        this.mCycleSpinner.setAdapter(this.mCycleAdapter);
        this.mCycleSpinner.setOnItemSelectedListener(this.mCycleListener);
        this.mCycleSummary = (TextView) this.mCycleView.findViewById(R.id.cycle_summary);
        this.mNetworkSwitches.addView(this.mCycleView);
        this.mSeries = (ChartNetworkSeriesView) view.findViewById(R.id.series);
        this.mDetailedSeries = (ChartNetworkSeriesView) view.findViewById(R.id.detail_series);
        this.mChart = (ChartDataUsageView) this.mHeader.findViewById(R.id.chart);
        this.mChart.setListener(this.mChartListener);
        this.mChart.bindNetworkPolicy(null);
        this.mAppDetail = this.mHeader.findViewById(R.id.app_detail);
        this.mAppIcon = (ImageView) this.mAppDetail.findViewById(R.id.app_icon);
        this.mAppTitles = (ViewGroup) this.mAppDetail.findViewById(R.id.app_titles);
        this.mAppForeground = (TextView) this.mAppDetail.findViewById(R.id.app_foreground);
        this.mAppBackground = (TextView) this.mAppDetail.findViewById(R.id.app_background);
        this.mAppSwitches = (LinearLayout) this.mAppDetail.findViewById(R.id.app_switches);
        this.mAppSettings = (Button) this.mAppDetail.findViewById(R.id.app_settings);
        this.mAppRestrict = new Switch(inflater.getContext());
        this.mAppRestrict.setClickable(false);
        this.mAppRestrict.setFocusable(false);
        this.mAppRestrictView = inflatePreference(inflater, this.mAppSwitches, this.mAppRestrict);
        this.mAppRestrictView.setClickable(true);
        this.mAppRestrictView.setFocusable(true);
        this.mAppRestrictView.setOnClickListener(this.mAppRestrictListener);
        this.mAppSwitches.addView(this.mAppRestrictView);
        this.mDisclaimer = this.mHeader.findViewById(R.id.disclaimer);
        this.mEmpty = (TextView) this.mHeader.findViewById(16908292);
        this.mStupidPadding = this.mHeader.findViewById(R.id.stupid_padding);
        this.mAdapter = new DataUsageAdapter(context, (UserManager) context.getSystemService("user"), this.mUidDetailProvider, this.mInsetSide);
        this.mListView.setOnItemClickListener(this.mListListener);
        this.mListView.setAdapter(this.mAdapter);
        showRequestedAppIfNeeded(view);
        return view;
    }

    private void showRequestedAppIfNeeded(View rootView) {
        if (this.mShowAppImmediatePkg != null) {
            try {
                int uid = getActivity().getPackageManager().getPackageUid(this.mShowAppImmediatePkg, UserHandle.myUserId());
                AppItem app = new AppItem(uid);
                app.addUid(uid);
                UidDetail detail = this.mUidDetailProvider.getUidDetail(app.key, true);
                AppHeader.createAppHeader(getActivity(), detail.icon, detail.label, null, (FrameLayout) rootView.findViewById(R.id.pinned_header));
                AppDetailsFragment.show(this, app, detail.label, true);
            } catch (NameNotFoundException e) {
                Log.w("DataUsage", "Could not find " + this.mShowAppImmediatePkg, e);
                Toast.makeText(getActivity(), getString(R.string.unknown_app), 1).show();
                getActivity().finish();
            }
        }
    }

    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d("DataUsage", "onViewStateRestored");
        this.mIntentTab = computeTabFromIntent(getActivity().getIntent());
        restoreSavedTab(savedInstanceState);
        updateTabs();
    }

    private void registerDateStateReceiver() {
        getActivity().registerReceiver(this.mDateStateReceiver, new IntentFilter("android.intent.action.ACTION_MOBILE_DATA_ENABLE"));
    }

    public void onResume() {
        super.onResume();
        Log.d("DataUsage", "onResume()... start");
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("com.mediatek.server.action.ACTION_POLICY_CREATED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        if (CdmaUtils.isCdmaCardCompetionForData(getActivity()) || CdmaUtils.isCdamCardAndGsmCard(getActivity())) {
            intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        }
        getActivity().registerReceiver(this.mReceiver, intentFilter);
        getView().post(new Runnable() {
            public void run() {
                DataUsageSummary.this.highlightViewIfNeeded();
            }
        });
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(2000);
                    DataUsageSummary.this.mStatsService.forceUpdate();
                } catch (InterruptedException e) {
                } catch (RemoteException e2) {
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                if (DataUsageSummary.this.isAdded()) {
                    DataUsageSummary.this.updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        if (SHOW_OVERVIEW_TAB && this.mOverviewAdapter != null) {
            this.mOverviewAdapter.bindOperaMaxLoader();
        }
        registerDateStateReceiver();
        Log.d("DataUsage", "onResume()... end");
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem;
        boolean z;
        boolean z2 = true;
        Context context = getActivity();
        boolean appDetailMode = isAppDetailMode();
        boolean isOwner = ActivityManager.getCurrentUser() == 0;
        this.mMenuShowWifi = menu.findItem(R.id.data_usage_menu_show_wifi);
        if (hasWifiRadio(context) && hasReadyMobileRadio(context)) {
            menuItem = this.mMenuShowWifi;
            if (appDetailMode) {
                z = false;
            } else {
                z = true;
            }
            menuItem.setVisible(z);
        } else {
            this.mMenuShowWifi.setVisible(false);
        }
        this.mMenuShowEthernet = menu.findItem(R.id.data_usage_menu_show_ethernet);
        if (hasEthernet(context) && hasReadyMobileRadio(context)) {
            menuItem = this.mMenuShowEthernet;
            if (appDetailMode) {
                z = false;
            } else {
                z = true;
            }
            menuItem.setVisible(z);
        } else {
            this.mMenuShowEthernet.setVisible(false);
        }
        this.mMenuRestrictBackground = menu.findItem(R.id.data_usage_menu_restrict_background);
        this.mMenuRestrictBackground.setTitle(mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_background), IDataUsageSummaryExt.TAG_BG_DATA_SWITCH));
        menuItem = this.mMenuRestrictBackground;
        z = hasReadyMobileRadio(context) && isOwner && !appDetailMode;
        menuItem.setVisible(z);
        MenuItem metered = menu.findItem(R.id.data_usage_menu_metered);
        if (hasReadyMobileRadio(context) || hasWifiRadio(context)) {
            if (appDetailMode) {
                z2 = false;
            }
            metered.setVisible(z2);
        } else {
            metered.setVisible(false);
        }
        this.mMenuSimCards = menu.findItem(R.id.data_usage_menu_sim_cards);
        this.mMenuSimCards.setVisible(false);
        this.mMenuCellularNetworks = menu.findItem(R.id.data_usage_menu_cellular_networks);
        MenuItem menuItem2 = this.mMenuCellularNetworks;
        if (!hasReadyMobileRadio(context) || appDetailMode) {
            isOwner = false;
        }
        menuItem2.setVisible(isOwner);
        MenuItem help = menu.findItem(R.id.data_usage_menu_help);
        String helpUrl = getResources().getString(R.string.help_url_data_usage);
        if (TextUtils.isEmpty(helpUrl)) {
            help.setVisible(false);
        } else {
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl, getClass().getName());
        }
        updateMenuTitles();
    }

    private void updateMenuTitles() {
        if (this.mPolicyManager.getRestrictBackground()) {
            this.mMenuRestrictBackground.setTitle(R.string.data_usage_menu_allow_background);
        } else {
            this.mMenuRestrictBackground.setTitle(mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_background), IDataUsageSummaryExt.TAG_BG_DATA_SWITCH));
        }
        if (this.mShowWifi) {
            this.mMenuShowWifi.setTitle(R.string.data_usage_menu_hide_wifi);
        } else {
            this.mMenuShowWifi.setTitle(R.string.data_usage_menu_show_wifi);
        }
        if (this.mShowEthernet) {
            this.mMenuShowEthernet.setTitle(R.string.data_usage_menu_hide_ethernet);
        } else {
            this.mMenuShowEthernet.setTitle(R.string.data_usage_menu_show_ethernet);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean z = false;
        switch (item.getItemId()) {
            case R.id.data_usage_menu_restrict_background:
                if (!this.mPolicyManager.getRestrictBackground()) {
                    ConfirmRestrictFragment.show(this);
                } else {
                    setRestrictBackground(false);
                }
                return true;
            case R.id.data_usage_menu_show_wifi:
                if (!this.mShowWifi) {
                    z = true;
                }
                this.mShowWifi = z;
                this.mPrefs.edit().putBoolean("show_wifi", this.mShowWifi).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            case R.id.data_usage_menu_show_ethernet:
                if (!this.mShowEthernet) {
                    z = true;
                }
                this.mShowEthernet = z;
                this.mPrefs.edit().putBoolean("show_ethernet", this.mShowEthernet).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            case R.id.data_usage_menu_metered:
                ((SettingsActivity) getActivity()).startPreferencePanel(DataUsageMeteredSettings.class.getCanonicalName(), null, R.string.data_usage_metered_title, null, this, 0);
                return true;
            case R.id.data_usage_menu_sim_cards:
                return true;
            case R.id.data_usage_menu_cellular_networks:
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    public void onDestroy() {
        this.mDataEnabledView = null;
        this.mDisableAtLimitView = null;
        mDataUsageSummaryExt.destroy();
        getLoaderManager().destroyLoader(2);
        if (this.mUidDetailProvider != null) {
            this.mUidDetailProvider.clearCache();
        }
        this.mUidDetailProvider = null;
        TrafficStats.closeQuietly(this.mStatsSession);
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        super.onDestroy();
    }

    private void ensureLayoutTransitions() {
        if (this.mShowAppImmediatePkg == null && this.mChart.getLayoutTransition() == null) {
            this.mTabsContainer.setLayoutTransition(buildLayoutTransition());
            this.mHeader.setLayoutTransition(buildLayoutTransition());
            this.mNetworkSwitchesContainer.setLayoutTransition(buildLayoutTransition());
            LayoutTransition chartTransition = buildLayoutTransition();
            chartTransition.disableTransitionType(2);
            chartTransition.disableTransitionType(3);
            this.mChart.setLayoutTransition(chartTransition);
        }
    }

    private static LayoutTransition buildLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setAnimateParentHierarchy(false);
        return transition;
    }

    private void updateTabs() {
        int i = 0;
        Log.d("DataUsage", "updateTabs~~");
        Context context = getActivity();
        this.mTabHost.clearAllTabs();
        if (SHOW_OVERVIEW_TAB) {
            this.mTabHost.addTab(buildTabSpec("Overview", (int) R.string.datausage_overview_tab));
            Log.d("DataUsage", "updateTabs, Add OVERVIEW TAB");
        }
        int simCount = this.mTelephonyManager.getSimCount();
        List<SubscriptionInfo> sirs = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        this.mMobileTagMap = initMobileTabTag(sirs);
        if (sirs != null) {
            for (SubscriptionInfo sir : sirs) {
                addMobileTab(context, sir, simCount > 1);
            }
        }
        if (this.mShowWifi && hasWifiRadio(context)) {
            this.mTabHost.addTab(buildTabSpec("wifi", (int) R.string.data_usage_tab_wifi));
        }
        if (this.mShowEthernet && hasEthernet(context)) {
            this.mTabHost.addTab(buildTabSpec("ethernet", (int) R.string.data_usage_tab_ethernet));
        }
        boolean noTabs = this.mTabWidget.getTabCount() == 0;
        boolean multipleTabs = this.mTabWidget.getTabCount() > 1;
        TabWidget tabWidget = this.mTabWidget;
        if (!multipleTabs) {
            i = 8;
        }
        tabWidget.setVisibility(i);
        Log.d("DataUsage", "mIntentTab: " + this.mIntentTab + ", currentTab: " + this.mTabHost.getCurrentTabTag());
        if (this.mIntentTab != null) {
            if (Objects.equal(this.mIntentTab, this.mTabHost.getCurrentTabTag())) {
                updateBody();
            } else {
                this.mTabHost.setCurrentTabByTag(this.mIntentTab);
            }
            this.mIntentTab = null;
        } else if (noTabs) {
            updateBody();
        }
    }

    private TabSpec buildTabSpec(String tag, int titleRes) {
        return this.mTabHost.newTabSpec(tag).setIndicator(getText(titleRes)).setContent(this.mEmptyTabContent);
    }

    private TabSpec buildTabSpec(String tag, CharSequence title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    private void updateBody() {
        this.mBinding = true;
        if (isAdded()) {
            Context context = getActivity();
            Resources resources = context.getResources();
            String currentTab = this.mTabHost.getCurrentTabTag();
            boolean isOwner = ActivityManager.getCurrentUser() == 0;
            if (currentTab == null) {
                Log.w("DataUsage", "no tab selected; hiding body");
                this.mListView.setVisibility(8);
            } else if (SHOW_OVERVIEW_TAB && "Overview".equals(currentTab)) {
                Log.d("DataUsage", "updateBody(), update overview tab~~");
                this.mListView.setVisibility(8);
                this.mOverViewExpList.setVisibility(0);
                this.mOverviewAdapter.updateAdapter();
                this.mOverviewAdapter.notifyDataSetChanged();
                getActivity().invalidateOptionsMenu();
            } else {
                if (SHOW_OVERVIEW_TAB) {
                    this.mOverViewExpList.setVisibility(8);
                }
                this.mListView.setVisibility(0);
                this.mCurrentTab = currentTab;
                mDataUsageSummaryExt.setCurrentTab(getSubId(this.mCurrentTab));
                Log.d("DataUsage", "updateBody() with currentTab=" + currentTab);
                this.mDataEnabledSupported = isOwner;
                this.mDisableAtLimitSupported = true;
                Log.d("DataUsage", "updateBody() isMobileTab=" + isMobileTab(currentTab));
                if (isMobileTab(currentTab)) {
                    Log.d("DataUsage", "updateBody() mobile tab");
                    setPreferenceTitle(this.mDataEnabledView, (int) R.string.data_usage_enable_mobile);
                    setPreferenceTitle(this.mDisableAtLimitView, (int) R.string.data_usage_disable_mobile_limit);
                    this.mDataEnabledSupported = isMobileDataAvailable(getSubId(currentTab));
                    this.mTemplate = NetworkTemplate.buildTemplateMobileAll(getActiveSubscriberId(context, getSubId(currentTab)));
                    CdmaUtils.fillTemplateForCdmaLte(this.mTemplate, getSubId(currentTab));
                    this.mTemplate = NetworkTemplate.normalize(this.mTemplate, this.mTelephonyManager.getMergedSubscriberIds());
                } else if ("3g".equals(currentTab)) {
                    Log.d("DataUsage", "updateBody() 3g tab");
                    setPreferenceTitle(this.mDataEnabledView, (int) R.string.data_usage_enable_3g);
                    setPreferenceTitle(this.mDisableAtLimitView, (int) R.string.data_usage_disable_3g_limit);
                    this.mTemplate = NetworkTemplate.buildTemplateMobile3gLower(getActiveSubscriberId(context));
                } else if ("4g".equals(currentTab)) {
                    Log.d("DataUsage", "updateBody() 4g tab");
                    setPreferenceTitle(this.mDataEnabledView, (int) R.string.data_usage_enable_4g);
                    setPreferenceTitle(this.mDisableAtLimitView, (int) R.string.data_usage_disable_4g_limit);
                    this.mTemplate = NetworkTemplate.buildTemplateMobile4g(getActiveSubscriberId(context));
                } else if ("wifi".equals(currentTab)) {
                    Log.d("DataUsage", "updateBody() wifi tab");
                    this.mDataEnabledSupported = false;
                    this.mDisableAtLimitSupported = false;
                    this.mTemplate = NetworkTemplate.buildTemplateWifiWildcard();
                } else if ("ethernet".equals(currentTab)) {
                    Log.d("DataUsage", "updateBody() ethernet tab");
                    this.mDataEnabledSupported = false;
                    this.mDisableAtLimitSupported = false;
                    this.mTemplate = NetworkTemplate.buildTemplateEthernet();
                } else {
                    Log.d("DataUsage", "updateBody() unknown tab");
                    throw new IllegalStateException("unknown tab: " + currentTab);
                }
                Log.d("DataUsage", "mDataEnabledSupported: " + this.mDataEnabledSupported + ", mDisableAtLimitSupported" + this.mDisableAtLimitSupported);
                this.mPolicyEditor.read();
                NetworkPolicy policy = this.mPolicyEditor.getPolicy(this.mTemplate);
                if (policy != null) {
                    long currentTime = System.currentTimeMillis();
                    long start = NetworkPolicyManager.computeLastCycleBoundary(currentTime, policy);
                    long totalBytes = 0;
                    try {
                        totalBytes = this.mStatsService.getNetworkTotalBytes(policy.template, start, currentTime);
                    } catch (RuntimeException e) {
                    } catch (RemoteException e2) {
                    }
                    if (!policy.isOverLimit(totalBytes) || policy.lastLimitSnooze >= start) {
                        ((TextView) this.mDataEnabledView.findViewById(16908304)).setVisibility(8);
                    } else {
                        setPreferenceSummary(this.mDataEnabledView, getString(R.string.data_usage_cellular_data_summary));
                    }
                }
                getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, this.mCurrentApp), this.mChartDataCallbacks);
                getActivity().invalidateOptionsMenu();
                this.mBinding = false;
                int seriesColor = context.getColor(R.color.sim_noitification);
                if (this.mCurrentTab != null && this.mCurrentTab.length() > "mobile".length()) {
                    SubscriptionInfo sir = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(Integer.parseInt(this.mCurrentTab.substring("mobile".length(), this.mCurrentTab.length())));
                    if (sir != null) {
                        seriesColor = sir.getIconTint();
                    }
                }
                int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor), Color.blue(seriesColor));
                this.mSeries.setChartColor(-16777216, seriesColor, secondaryColor);
                this.mDetailedSeries.setChartColor(-16777216, seriesColor, secondaryColor);
                updateScreenEnableState();
            }
        }
    }

    private boolean isAppDetailMode() {
        boolean z;
        String str = "DataUsage";
        StringBuilder append = new StringBuilder().append("isAppDetailMode? ");
        if (this.mCurrentApp != null) {
            z = true;
        } else {
            z = false;
        }
        Log.d(str, append.append(z).toString());
        return this.mCurrentApp != null;
    }

    private void updateAppDetail() {
        Context context = getActivity();
        PackageManager pm = context.getPackageManager();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if (isAppDetailMode()) {
            this.mAppDetail.setVisibility(0);
            this.mCycleAdapter.setChangeVisible(false);
            this.mChart.setVisibility(8);
            this.mChart.bindNetworkPolicy(null);
            int uid = this.mCurrentApp.key;
            UidDetail detail = this.mUidDetailProvider.getUidDetail(uid, true);
            this.mAppIcon.setImageDrawable(detail.icon);
            this.mAppTitles.removeAllViews();
            View title = null;
            TextView appTitle;
            if (detail.detailLabels != null) {
                int n = detail.detailLabels.length;
                for (int i = 0; i < n; i++) {
                    CharSequence label = detail.detailLabels[i];
                    CharSequence contentDescription = detail.detailContentDescriptions[i];
                    title = inflater.inflate(R.layout.data_usage_app_title, this.mAppTitles, false);
                    appTitle = (TextView) title.findViewById(R.id.app_title);
                    appTitle.setText(label);
                    appTitle.setContentDescription(contentDescription);
                    this.mAppTitles.addView(title);
                }
            } else {
                title = inflater.inflate(R.layout.data_usage_app_title, this.mAppTitles, false);
                appTitle = (TextView) title.findViewById(R.id.app_title);
                appTitle.setText(detail.label);
                appTitle.setContentDescription(detail.contentDescription);
                this.mAppTitles.addView(title);
            }
            if (title != null) {
                this.mAppTotal = (TextView) title.findViewById(R.id.app_summary);
            } else {
                this.mAppTotal = null;
            }
            String[] packageNames = pm.getPackagesForUid(uid);
            if (packageNames == null || packageNames.length <= 0) {
                this.mAppSettingsIntent = null;
                this.mAppSettings.setOnClickListener(null);
                this.mAppSettings.setVisibility(8);
            } else {
                this.mAppSettingsIntent = new Intent("android.intent.action.MANAGE_NETWORK_USAGE");
                this.mAppSettingsIntent.addCategory("android.intent.category.DEFAULT");
                boolean matchFound = false;
                for (String packageName : packageNames) {
                    this.mAppSettingsIntent.setPackage(packageName);
                    if (pm.resolveActivity(this.mAppSettingsIntent, 0) != null) {
                        matchFound = true;
                        break;
                    }
                }
                final int i2 = uid;
                this.mAppSettings.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (DataUsageSummary.this.isAdded()) {
                            DataUsageSummary.this.getActivity().startActivityAsUser(DataUsageSummary.this.mAppSettingsIntent, new UserHandle(UserHandle.getUserId(i2)));
                        }
                    }
                });
                this.mAppSettings.setEnabled(matchFound);
                this.mAppSettings.setVisibility(0);
            }
            updateDetailData();
            if (UserHandle.isApp(uid) && !this.mPolicyManager.getRestrictBackground() && isBandwidthControlEnabled() && hasReadyMobileRadio(context)) {
                String appBgDataTitle = mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_background), IDataUsageSummaryExt.TAG_BG_DATA_SWITCH);
                String appBgDataSummary = mDataUsageSummaryExt.customizeBackgroundString(getString(R.string.data_usage_app_restrict_background_summary), IDataUsageSummaryExt.TAG_BG_DATA_SUMMARY);
                setPreferenceTitle(this.mAppRestrictView, appBgDataTitle);
                setPreferenceSummary(this.mAppRestrictView, appBgDataSummary);
                this.mAppRestrictView.setVisibility(0);
                this.mAppRestrict.setChecked(getAppRestrictBackground());
            } else {
                this.mAppRestrictView.setVisibility(8);
            }
            return;
        }
        this.mAppDetail.setVisibility(8);
        this.mCycleAdapter.setChangeVisible(true);
        this.mChart.setVisibility(0);
        this.mChart.bindDetailNetworkStats(null);
    }

    private void setPolicyWarningBytes(long warningBytes) {
        Log.d("DataUsage", "setPolicyWarningBytes()");
        this.mPolicyEditor.setPolicyWarningBytes(this.mTemplate, warningBytes);
        updatePolicy(false);
    }

    private void setPolicyLimitBytes(long limitBytes) {
        Log.d("DataUsage", "setPolicyLimitBytes()");
        if (this.mTemplate == null) {
            Log.w("DataUsage", "template is null, just return");
            return;
        }
        int rule = this.mTemplate.getMatchRule();
        Log.d("DataUsage", "rule = " + rule);
        if (rule != 7) {
            this.mPolicyEditor.setPolicyLimitBytes(this.mTemplate, limitBytes);
            updatePolicy(false);
        }
    }

    private boolean isMobileDataEnabled(int subId) {
        Log.d("DataUsage", "isMobileDataEnabled:+ subId=" + subId);
        if (this.mMobileDataEnabled.get(String.valueOf(subId)) != null) {
            boolean isEnable = ((Boolean) this.mMobileDataEnabled.get(String.valueOf(subId))).booleanValue();
            Log.d("DataUsage", "isMobileDataEnabled: != null, subId=" + subId + " isEnable=" + isEnable);
            return isEnable;
        }
        isEnable = this.mTelephonyManager.getDataEnabled(subId);
        Log.d("DataUsage", "isMobileDataEnabled: == null, subId=" + subId + " isEnable=" + isEnable);
        return isEnable;
    }

    private void setMobileDataEnabled(int subId, boolean enabled) {
        Log.d("DataUsage", "setMobileDataEnabled()");
        this.mTelephonyManager.setDataEnabled(subId, enabled);
        this.mMobileDataEnabled.put(String.valueOf(subId), Boolean.valueOf(enabled));
        updatePolicy(false);
    }

    private boolean isNetworkPolicyModifiable(NetworkPolicy policy) {
        boolean z;
        String str = "DataUsage";
        StringBuilder append = new StringBuilder().append("policy isn't null ? ");
        if (policy != null) {
            z = true;
        } else {
            z = false;
        }
        Log.d(str, append.append(z).append(", bandWidthControl: ").append(isBandwidthControlEnabled()).append(", isChecked: ").append(this.mDataEnabled.isChecked()).append(", currentUser: ").append(ActivityManager.getCurrentUser()).toString());
        if (policy != null && isBandwidthControlEnabled() && this.mDataEnabled.isChecked() && ActivityManager.getCurrentUser() == 0) {
            return true;
        }
        return false;
    }

    private boolean isBandwidthControlEnabled() {
        try {
            return this.mNetworkService.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            Log.w("DataUsage", "problem talking with INetworkManagementService: " + e);
            return false;
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        this.mPolicyManager.setRestrictBackground(restrictBackground);
        updateMenuTitles();
    }

    private boolean getAppRestrictBackground() {
        if ((this.mPolicyManager.getUidPolicy(this.mCurrentApp.key) & 1) != 0) {
            return true;
        }
        return false;
    }

    private void setAppRestrictBackground(boolean restrictBackground) {
        Log.d("DataUsage", "setAppRestrictBackground()");
        this.mPolicyManager.setUidPolicy(this.mCurrentApp.key, restrictBackground ? 1 : 0);
        this.mAppRestrict.setChecked(restrictBackground);
    }

    public void updatePolicy(boolean refreshCycle) {
        int i;
        boolean z = true;
        int i2 = 0;
        boolean z2 = this.mDataEnabledSupported;
        boolean disableAtLimitVisible = this.mDisableAtLimitSupported;
        if (isAppDetailMode()) {
            z2 = false;
            disableAtLimitVisible = false;
        }
        if (isMobileTab(this.mCurrentTab)) {
            this.mBinding = true;
            this.mDataEnabled.setChecked(isMobileDataEnabled(getSubId(this.mCurrentTab)));
            this.mBinding = false;
        }
        NetworkPolicy policy = this.mPolicyEditor.getPolicy(this.mTemplate);
        if (isNetworkPolicyModifiable(policy) && isMobileDataAvailable(getSubId(this.mCurrentTab))) {
            Switch switchR = this.mDisableAtLimit;
            if (policy == null || policy.limitBytes == -1) {
                z = false;
            }
            switchR.setChecked(z);
            if (!isAppDetailMode()) {
                this.mChart.bindNetworkPolicy(policy);
            }
        } else {
            disableAtLimitVisible = false;
            this.mChart.bindNetworkPolicy(null);
        }
        Log.d("DataUsage", "updatePolicy(), dataEnabledVisible: " + z2 + " ,disableAtLimitVisible" + disableAtLimitVisible);
        View view = this.mDataEnabledView;
        if (z2) {
            i = 0;
        } else {
            i = 8;
        }
        view.setVisibility(i);
        View view2 = this.mDisableAtLimitView;
        if (!disableAtLimitVisible) {
            i2 = 8;
        }
        view2.setVisibility(i2);
        updateScreenEnableState();
        if (refreshCycle) {
            updateCycleList(policy);
        }
    }

    private void updateCycleList(NetworkPolicy policy) {
        long cycleEnd;
        long cycleStart;
        boolean includeCycle;
        CycleItem previousItem = (CycleItem) this.mCycleSpinner.getSelectedItem();
        this.mCycleAdapter.clear();
        Context context = this.mCycleSpinner.getContext();
        NetworkStatsHistory.Entry entry = null;
        if (this.mRestoreCycleStart > 0 && this.mRestoreCycleEnd > 0) {
            previousItem = new CycleItem(context, this.mRestoreCycleStart, this.mRestoreCycleEnd);
            this.mRestoreCycleStart = 0;
            this.mRestoreCycleEnd = 0;
        }
        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (this.mChartData != null) {
            historyStart = this.mChartData.network.getStart();
            historyEnd = this.mChartData.network.getEnd();
        }
        long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) {
            historyStart = now;
        }
        if (historyEnd == Long.MIN_VALUE) {
            historyEnd = now + 1;
        }
        boolean hasCycles = false;
        if (policy != null) {
            cycleEnd = NetworkPolicyManager.computeNextCycleBoundary(historyEnd, policy);
            while (cycleEnd > historyStart) {
                cycleStart = NetworkPolicyManager.computeLastCycleBoundary(cycleEnd, policy);
                Log.d("DataUsage", "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs=" + historyStart);
                if (this.mChartData != null) {
                    entry = this.mChartData.network.getValues(cycleStart, cycleEnd, entry);
                    includeCycle = entry.rxBytes + entry.txBytes > 0;
                } else {
                    includeCycle = true;
                }
                if (includeCycle) {
                    this.mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                    hasCycles = true;
                }
                cycleEnd = cycleStart;
            }
            this.mCycleAdapter.setChangePossible(isNetworkPolicyModifiable(policy));
        }
        if (!hasCycles) {
            cycleEnd = historyEnd;
            while (cycleEnd > historyStart) {
                cycleStart = cycleEnd - 2419200000L;
                if (this.mChartData != null) {
                    entry = this.mChartData.network.getValues(cycleStart, cycleEnd, entry);
                    includeCycle = entry.rxBytes + entry.txBytes > 0;
                } else {
                    includeCycle = true;
                }
                if (includeCycle) {
                    this.mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                }
                cycleEnd = cycleStart;
            }
            this.mCycleAdapter.setChangePossible(false);
        }
        if (this.mCycleAdapter.getCount() > 0) {
            int position = this.mCycleAdapter.findNearestPosition(previousItem);
            this.mCycleSpinner.setSelection(position);
            if (Objects.equal((CycleItem) this.mCycleAdapter.getItem(position), previousItem)) {
                updateDetailData();
                return;
            } else {
                this.mCycleListener.onItemSelected(this.mCycleSpinner, null, position, 0);
                return;
            }
        }
        updateDetailData();
    }

    private void disableDataForOtherSubscriptions(SubscriptionInfo currentSir) {
        if (this.mSubInfoList != null) {
            for (SubscriptionInfo subInfo : this.mSubInfoList) {
                if (subInfo.getSubscriptionId() != currentSir.getSubscriptionId()) {
                    setMobileDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    private void handleMultiSimDataDialog() {
        final Context context = getActivity();
        final SubscriptionInfo currentSir = getCurrentTabSubInfo(context);
        if (currentSir != null) {
            SubscriptionInfo nextSir = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
            if (!Utils.showSimCardTile(context) || (nextSir != null && currentSir != null && currentSir.getSubscriptionId() == nextSir.getSubscriptionId())) {
                setMobileDataEnabled(currentSir.getSubscriptionId(), true);
                if (!(nextSir == null || currentSir == null || currentSir.getSubscriptionId() != nextSir.getSubscriptionId())) {
                    disableDataForOtherSubscriptions(currentSir);
                }
                updateBody();
            } else if (CdmaUtils.isCdmaCardCompetionForData(context) || CdmaUtils.isSwitchCdmaCardToGsmCard(context, currentSir.getSubscriptionId())) {
                handleCdmaCard(context, currentSir);
            } else {
                String previousName;
                if (nextSir == null) {
                    previousName = context.getResources().getString(R.string.sim_selection_required_pref);
                } else {
                    previousName = nextSir.getDisplayName().toString();
                }
                Builder builder = new Builder(getActivity());
                builder.setTitle(R.string.sim_change_data_title);
                builder.setMessage(getActivity().getResources().getString(R.string.sim_change_data_message, new Object[]{currentSir.getDisplayName(), previousName}));
                customizeDialogContent(context, builder, getActivity().getResources().getString(R.string.sim_change_data_title), getActivity().getResources().getString(R.string.sim_change_data_message, new Object[]{currentSir.getDisplayName(), previousName}));
                builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (TelecomManager.from(context).isInCall()) {
                            Toast.makeText(context, R.string.default_data_switch_err_msg1, 0).show();
                            return;
                        }
                        DataUsageSummary.this.mSubscriptionManager.setDefaultDataSubId(currentSir.getSubscriptionId());
                        DataUsageSummary.this.setMobileDataEnabled(currentSir.getSubscriptionId(), true);
                        DataUsageSummary.this.disableDataForOtherSubscriptions(currentSir);
                        DataUsageSummary.this.updateBody();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.create().show();
            }
        }
    }

    private void updateDetailData() {
        long totalBytes;
        long start = this.mChart.getInspectStart();
        long end = this.mChart.getInspectEnd();
        long now = System.currentTimeMillis();
        Log.d("DataUsage", "updateDetailData()... start: " + start + "end: " + end + "now: " + now);
        Context context = getActivity();
        NetworkStatsHistory.Entry entry = null;
        if (!isAppDetailMode() || this.mChartData == null || this.mChartData.detail == null) {
            if (this.mChartData != null) {
                entry = this.mChartData.network.getValues(start, end, now, null);
            }
            this.mCycleSummary.setVisibility(0);
            getLoaderManager().restartLoader(3, SummaryForAllUidLoader.buildArgs(this.mTemplate, start, end), this.mSummaryCallbacks);
        } else {
            entry = this.mChartData.detailDefault.getValues(start, end, now, null);
            long defaultBytes = entry.rxBytes + entry.txBytes;
            entry = this.mChartData.detailForeground.getValues(start, end, now, entry);
            long foregroundBytes = entry.rxBytes + entry.txBytes;
            totalBytes = defaultBytes + foregroundBytes;
            if (this.mAppTotal != null) {
                this.mAppTotal.setText(android.text.format.Formatter.formatFileSize(context, totalBytes));
            }
            this.mAppBackground.setText(android.text.format.Formatter.formatFileSize(context, defaultBytes));
            this.mAppForeground.setText(android.text.format.Formatter.formatFileSize(context, foregroundBytes));
            entry = this.mChartData.detail.getValues(start, end, now, null);
            getLoaderManager().destroyLoader(3);
            this.mCycleSummary.setVisibility(8);
        }
        totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
        String totalPhrase = android.text.format.Formatter.formatFileSize(context, totalBytes);
        Log.d("DataUsage", "updateDetailData()... totalBytes: " + totalBytes);
        this.mCycleSummary.setText(totalPhrase);
        if (!isMobileTab(this.mCurrentTab) && !"3g".equals(this.mCurrentTab) && !"4g".equals(this.mCurrentTab)) {
            this.mDisclaimer.setVisibility(8);
        } else if (isAppDetailMode()) {
            this.mDisclaimer.setVisibility(8);
        } else {
            this.mDisclaimer.setVisibility(0);
        }
        ensureLayoutTransitions();
    }

    private static String getActiveSubscriberId(Context context) {
        String actualSubscriberId = TelephonyManager.from(context).getSubscriberId();
        String retVal = SystemProperties.get("test.subscriberid", actualSubscriberId);
        Log.d("DataUsage", "getActiveSubscriberId=" + retVal + " actualSubscriberId=" + actualSubscriberId);
        return retVal;
    }

    private static String getActiveSubscriberId(Context context, int subId) {
        String retVal = TelephonyManager.from(context).getSubscriberId(subId);
        Log.d("DataUsage", "getActiveSubscriberId=" + retVal + " subId=" + subId);
        return retVal;
    }

    static {
        boolean z;
        if (FeatureOption.MTK_A1_FEATURE) {
            z = false;
        } else {
            z = true;
        }
        SHOW_OVERVIEW_TAB = z;
    }

    public static String formatDateRange(Context context, long start, long end) {
        String formatter;
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            formatter = DateUtils.formatDateRange(context, sFormatter, start, end, 65552, null).toString();
        }
        return formatter;
    }

    private static String computeTabFromIntent(Intent intent) {
        NetworkTemplate template = (NetworkTemplate) intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
        if (template == null) {
            int subId = intent.getIntExtra("subscription", -1);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                return "mobile" + String.valueOf(subId);
            }
            return null;
        }
        switch (template.getMatchRule()) {
            case 1:
                return "mobile";
            case 2:
                return "3g";
            case 3:
                return "4g";
            case 4:
                return "wifi";
            default:
                return null;
        }
    }

    public static boolean hasReadyMobileRadio(Context context) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        TelephonyManager tele = TelephonyManager.from(context);
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            Log.d("DataUsage", "hasReadyMobileRadio: subInfoList=null");
            return false;
        }
        boolean isReady = true;
        for (SubscriptionInfo subInfo : subInfoList) {
            int i;
            if (tele.getSimState(subInfo.getSimSlotIndex()) == 5) {
                i = 1;
            } else {
                i = 0;
            }
            isReady &= i;
            Log.d("DataUsage", "hasReadyMobileRadio: subInfo=" + subInfo);
        }
        boolean z = conn.isNetworkSupported(0) ? isReady : false;
        Log.d("DataUsage", "hasReadyMobileRadio: conn.isNetworkSupported(TYPE_MOBILE)=" + conn.isNetworkSupported(0) + " isReady=" + isReady);
        return z;
    }

    public static boolean hasReadyMobileRadio(Context context, int subId) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        boolean isReady = TelephonyManager.from(context).getSimState(SubscriptionManager.getSlotId(subId)) == 5;
        boolean z = conn.isNetworkSupported(0) ? isReady : false;
        Log.d("DataUsage", "hasReadyMobileRadio: subId=" + subId + " conn.isNetworkSupported(TYPE_MOBILE)=" + conn.isNetworkSupported(0) + " isReady=" + isReady);
        return z;
    }

    public static boolean hasWifiRadio(Context context) {
        return ConnectivityManager.from(context).isNetworkSupported(1);
    }

    public boolean hasEthernet(Context context) {
        boolean hasEthernet = ConnectivityManager.from(context).isNetworkSupported(9);
        if (this.mStatsSession != null) {
            try {
                long ethernetBytes = this.mStatsSession.getSummaryForNetwork(NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE).getTotalBytes();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        ethernetBytes = 0;
        if (!hasEthernet || ethernetBytes <= 0) {
            return false;
        }
        return true;
    }

    private static View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        View view = inflater.inflate(R.layout.preference, root, false);
        ((LinearLayout) view.findViewById(16908312)).addView(widget, new LayoutParams(-2, -2));
        return view;
    }

    private static void insetListViewDrawables(ListView view, int insetSide) {
        Drawable selector = view.getSelector();
        Drawable divider = view.getDivider();
        Drawable stub = new ColorDrawable(0);
        view.setSelector(stub);
        view.setDivider(stub);
        view.setSelector(new InsetBoundsDrawable(selector, insetSide));
        view.setDivider(new InsetBoundsDrawable(divider, insetSide));
    }

    private static void setPreferenceTitle(View parent, int resId) {
        ((TextView) parent.findViewById(16908310)).setText(resId);
    }

    private static void setPreferenceSummary(View parent, CharSequence string) {
        TextView summary = (TextView) parent.findViewById(16908304);
        summary.setVisibility(0);
        summary.setText(string);
    }

    private void addMobileTab(Context context, SubscriptionInfo subInfo, boolean isMultiSim) {
        if (subInfo == null || this.mMobileTagMap == null) {
            Log.d("DataUsage", "addMobileTab: subInfoList is null");
        } else if (!hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
        } else {
            if (isMultiSim) {
                this.mTabHost.addTab(buildTabSpec((String) this.mMobileTagMap.get(Integer.valueOf(subInfo.getSubscriptionId())), subInfo.getDisplayName()));
            } else {
                this.mTabHost.addTab(buildTabSpec((String) this.mMobileTagMap.get(Integer.valueOf(subInfo.getSubscriptionId())), (int) R.string.data_usage_tab_mobile));
            }
        }
    }

    private SubscriptionInfo getCurrentTabSubInfo(Context context) {
        if (!(this.mSubInfoList == null || this.mTabHost == null)) {
            int currentTagIndex = this.mTabHost.getCurrentTab();
            int i = SHOW_OVERVIEW_TAB ? 1 : 0;
            for (SubscriptionInfo subInfo : this.mSubInfoList) {
                if (hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
                    int i2 = i + 1;
                    if (i == currentTagIndex) {
                        return subInfo;
                    }
                    i = i2;
                }
            }
        }
        return null;
    }

    private Map<Integer, String> initMobileTabTag(List<SubscriptionInfo> subInfoList) {
        Map<Integer, String> map = null;
        if (subInfoList != null) {
            map = new HashMap();
            for (SubscriptionInfo subInfo : subInfoList) {
                map.put(Integer.valueOf(subInfo.getSubscriptionId()), "mobile" + String.valueOf(subInfo.getSubscriptionId()));
            }
        }
        return map;
    }

    private static boolean isMobileTab(String currentTab) {
        return currentTab != null ? currentTab.contains("mobile") : false;
    }

    private int getSubId(String currentTab) {
        if (this.mMobileTagMap != null) {
            for (Integer subId : this.mMobileTagMap.keySet()) {
                if (((String) this.mMobileTagMap.get(subId)).equals(currentTab)) {
                    return subId.intValue();
                }
            }
        }
        Log.e("DataUsage", "currentTab = " + currentTab + " non mobile tab called this function");
        return -1;
    }

    private boolean isMobileDataAvailable(int subId) {
        return this.mSubscriptionManager.getActiveSubscriptionInfo(subId) != null;
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mDateStateReceiver);
        getActivity().unregisterReceiver(this.mReceiver);
        if (this.mOverviewAdapter != null) {
            this.mOverviewAdapter.unbindOperaMaxLoader();
        }
    }

    private void createViewForOverviewTab(Context context, View view) {
        this.mOverViewExpList = (ExpandableListView) view.findViewById(R.id.overview_list);
        this.mOverViewExpList.addHeaderView(new View(context), null, true);
        this.mOverViewExpList.setAdapter(this.mOverviewAdapter);
        this.mOverViewExpList.expandGroup(0);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTabHost != null) {
            outState.putString("restore_current_tab", this.mTabHost.getCurrentTabTag());
            if (this.mCycleSpinner != null) {
                CycleItem currentItem = (CycleItem) this.mCycleSpinner.getSelectedItem();
                if (currentItem != null) {
                    outState.putLong("restore_current_start", currentItem.start);
                    outState.putLong("restore_current_end", currentItem.end);
                }
            }
        }
    }

    private void restoreSavedTab(Bundle savedInstanceState) {
        String string = savedInstanceState != null ? savedInstanceState.getString("restore_current_tab") : null;
        if (string != null) {
            Log.d("DataUsage", "restoreSavedTab savedTab = " + string);
            this.mIntentTab = string;
            this.mRestoreCycleStart = savedInstanceState.getLong("restore_current_start");
            this.mRestoreCycleEnd = savedInstanceState.getLong("restore_current_end");
        }
    }

    private void customizeDialogContent(Context context, Builder builder, String title, String message) {
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(context);
        builder.setTitle(miscExt.customizeSimDisplayString(title, -1));
        builder.setMessage(miscExt.customizeSimDisplayString(message, -1));
    }

    private static void setPreferenceTitle(View parent, String text) {
        ((TextView) parent.findViewById(16908310)).setText(text);
    }

    private void handleCdmaCard(Context context, SubscriptionInfo currentSir) {
        this.mCurrentSir = currentSir;
        CdmaUtils.startAlertCdmaDialog(context, currentSir.getSubscriptionId(), 0);
        this.mFlagClicked = true;
    }

    private void updateDataUiForCdmaCard(Intent intent) {
        int defaultDataSubId = intent.getIntExtra("subscription", -1);
        Log.d("DataUsage", "defaultDataSubId: " + defaultDataSubId + " mFlagClicked: " + this.mFlagClicked);
        if (this.mFlagClicked && this.mCurrentSir != null && defaultDataSubId == this.mCurrentSir.getSubscriptionId()) {
            setMobileDataEnabled(this.mCurrentSir.getSubscriptionId(), true);
            disableDataForOtherSubscriptions(this.mCurrentSir);
            updateBody();
            this.mFlagClicked = false;
        }
    }

    private void updateScreenEnableState() {
        boolean z = false;
        boolean isCapabilitySwitching = TelephonyUtils.isCapabilitySwitching();
        if (this.mDataEnabled != null) {
            boolean z2;
            Switch switchR = this.mDataEnabled;
            if (this.mIsAirplaneModeOn || isCapabilitySwitching) {
                z2 = false;
            } else {
                z2 = mDataUsageSummaryExt.isAllowDataEnable(this.mDataEnabled, getSubId(this.mCurrentTab));
            }
            switchR.setEnabled(z2);
        }
        if (this.mDataEnabledView != null) {
            View view = this.mDataEnabledView;
            if (!(this.mIsAirplaneModeOn || isCapabilitySwitching)) {
                z = mDataUsageSummaryExt.isAllowDataEnable(this.mDataEnabledView.findViewById(16908310), getSubId(this.mCurrentTab));
            }
            view.setEnabled(z);
        }
    }

    private boolean isSimSwitchAction(String action) {
        if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
            return true;
        }
        return action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
    }
}
