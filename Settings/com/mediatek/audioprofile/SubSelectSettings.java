package com.mediatek.audioprofile;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.mediatek.widget.AccountViewAdapter;
import com.mediatek.widget.AccountViewAdapter.AccountElements;
import java.util.ArrayList;
import java.util.List;

public class SubSelectSettings extends ListFragment {
    private AccountViewAdapter mAdapter;
    private List<Integer> mSlotIdList = new ArrayList();
    private List<SubscriptionInfo> mSubInfoList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = getActivity().getIntent().getStringExtra("android.intent.extra.TITLE");
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    public void onResume() {
        super.onResume();
        this.mSubInfoList = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (this.mSubInfoList == null || this.mSubInfoList.size() <= 0) {
            getActivity().finish();
        } else {
            setListAdapter(getAdapter(this.mSubInfoList));
        }
    }

    private ListAdapter getAdapter(List<SubscriptionInfo> subInfoList) {
        if (this.mAdapter == null) {
            this.mAdapter = new AccountViewAdapter(getActivity(), getAccountsData(subInfoList));
        } else {
            this.mAdapter.updateData(getAccountsData(subInfoList));
        }
        return this.mAdapter;
    }

    private List<AccountElements> getAccountsData(List<SubscriptionInfo> subInfoList) {
        List<AccountElements> accounts = new ArrayList();
        for (SubscriptionInfo record : subInfoList) {
            accounts.add(new AccountElements(record.getIconTint(), record.getDisplayName().toString(), record.getNumber()));
        }
        return accounts;
    }

    public void onListItemClick(ListView listView, View v, int position, long id) {
        Intent intent = new Intent();
        SubscriptionInfo record = (SubscriptionInfo) this.mSubInfoList.get(position);
        long subId = (long) record.getSubscriptionId();
        int slotId = record.getSimSlotIndex();
        Log.d("SubSelectSettings", "onListItemClick with slotId = " + slotId + " subId = " + subId);
        intent.putExtra("slot", slotId);
        intent.putExtra("subscription", subId);
        getActivity().setResult(-1, intent);
        getActivity().finish();
    }
}
