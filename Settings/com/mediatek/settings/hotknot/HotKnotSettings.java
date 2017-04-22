package com.mediatek.settings.hotknot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.mediatek.hotknot.HotKnotAdapter;
import java.util.ArrayList;
import java.util.List;

public class HotKnotSettings extends SettingsPreferenceFragment implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C07362();
    private HotKnotAdapter mAdapter;
    private HotKnotEnabler mHotKnotEnabler;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new C07351();
    private SwitchBar mSwitchBar;

    class C07351 extends BroadcastReceiver {
        C07351() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        }
    }

    static class C07362 extends BaseSearchIndexProvider {
        C07362() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            if (HotKnotAdapter.getDefaultAdapter(context) != null) {
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.hotknot_settings_title);
                data.screenTitle = res.getString(R.string.hotknot_settings_title);
                data.keywords = res.getString(R.string.hotknot_settings_title);
                result.add(data);
            }
            return result;
        }
    }

    protected int getMetricsCategory() {
        return 100003;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAdapter = HotKnotAdapter.getDefaultAdapter((SettingsActivity) getActivity());
        if (this.mAdapter == null) {
            Log.d("@M_HotKnotSettings", "Hotknot adapter is null, finish Hotknot settings");
            getActivity().finish();
        }
        this.mIntentFilter = new IntentFilter("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
    }

    public void onStart() {
        super.onStart();
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        Log.d("@M_HotKnotSettings", "onCreate, mSwitchBar = " + this.mSwitchBar);
        this.mHotKnotEnabler = new HotKnotEnabler(activity, this.mSwitchBar);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hotknot_settings, container, false);
        TextView textView = (TextView) view.findViewById(R.id.hotknot_warning_msg);
        if (textView != null) {
            textView.setText(getString(R.string.hotknot_charging_warning, new Object[]{getString(R.string.hotknot_settings_title)}));
        }
        return view;
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (this.mHotKnotEnabler != null) {
            this.mHotKnotEnabler.teardownSwitchBar();
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mHotKnotEnabler != null) {
            this.mHotKnotEnabler.resume();
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        if (this.mHotKnotEnabler != null) {
            this.mHotKnotEnabler.pause();
        }
    }
}
