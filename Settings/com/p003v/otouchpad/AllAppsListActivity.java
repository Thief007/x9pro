package com.p003v.otouchpad;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.android.settings.R;
import com.p003v.common.AppInfo;
import com.p003v.common.SearchEditText;
import java.util.ArrayList;
import java.util.List;

public class AllAppsListActivity extends Activity implements OnItemClickListener {
    private static List<AppInfo> mAppsInfosList = null;
    public static ListView mAppsInfosListView = null;
    private static List<AppInfo> mFilterAppsList = null;
    public static AppsListAdapter mListAdapter = null;
    private List<ResolveInfo> mAllIntentApps = null;
    private LayoutInflater mInflater = null;
    private AppsListAdapter mListAdapterNull = null;
    private SearchEditText mSearchEditText = null;

    class C07811 implements TextWatcher {
        C07811() {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            AllAppsListActivity.this.filterData(s.toString());
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable s) {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v_apps_list_main);
        mAppsInfosListView = (ListView) findViewById(R.id.appinfo_list);
        mAppsInfosListView.setOnItemClickListener(this);
        this.mSearchEditText = (SearchEditText) findViewById(R.id.filter_edit);
        this.mSearchEditText.addTextChangedListener(new C07811());
        this.mInflater = LayoutInflater.from(getApplicationContext());
        this.mListAdapterNull = new AppsListAdapter(getApplicationContext());
        mAppsInfosList = this.mListAdapterNull.getDownLoadAppInfoList();
        mFilterAppsList = mAppsInfosList;
        mListAdapter = new AppsListAdapter(getApplication(), mAppsInfosList, this.mInflater);
        mAppsInfosListView.setAdapter(mListAdapter);
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        this.mAllIntentApps = getPackageManager().queryIntentActivities(mainIntent, 0);
    }

    private void filterData(String filterStr) {
        mFilterAppsList = new ArrayList();
        if (TextUtils.isEmpty(filterStr)) {
            mFilterAppsList = mAppsInfosList;
        } else {
            mFilterAppsList.clear();
            for (AppInfo sortModel : mAppsInfosList) {
                if (sortModel.getAppName().toLowerCase().indexOf(filterStr.toString().toLowerCase()) != -1) {
                    mFilterAppsList.add(sortModel);
                }
            }
        }
        mListAdapter.updateListView(mFilterAppsList);
    }

    public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
        Log.i("AllAppsListActivity", "OTouchpad itemclick position=" + position);
        for (int i = 0; i < this.mAllIntentApps.size(); i++) {
            String packageName = ((ResolveInfo) this.mAllIntentApps.get(i)).activityInfo.packageName;
            String activityName = ((ResolveInfo) this.mAllIntentApps.get(i)).activityInfo.name;
            if (((AppInfo) mFilterAppsList.get(position)).getPackageName().equals(packageName) && ((AppInfo) mFilterAppsList.get(position)).getMainActivityName().equals(activityName)) {
                Log.i("AllAppsListActivity", "OTouchpad packageName=" + packageName + "    activityName=" + activityName);
                SystemProperties.set("persist.sys.otp_pkg", packageName);
                SystemProperties.set("persist.sys.otp_act", activityName);
                finish();
            }
        }
    }
}
