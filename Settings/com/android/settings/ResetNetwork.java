package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;

public class ResetNetwork extends InstrumentedFragment {
    private View mContentView;
    private Button mInitiateButton;
    private final OnClickListener mInitiateListener = new C01741();
    private Spinner mSubscriptionSpinner;
    private List<SubscriptionInfo> mSubscriptions;

    class C01741 implements OnClickListener {
        C01741() {
        }

        public void onClick(View v) {
            if (!ResetNetwork.this.runKeyguardConfirmation(55)) {
                ResetNetwork.this.showFinalConfirmation();
            }
        }
    }

    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, getActivity().getResources().getText(R.string.reset_network_title));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 55) {
            if (resultCode == -1) {
                showFinalConfirmation();
            } else {
                establishInitialState();
            }
        }
    }

    private void showFinalConfirmation() {
        Bundle args = new Bundle();
        if (this.mSubscriptions != null && this.mSubscriptions.size() > 0) {
            args.putInt("subscription", ((SubscriptionInfo) this.mSubscriptions.get(this.mSubscriptionSpinner.getSelectedItemPosition())).getSubscriptionId());
        }
        ((SettingsActivity) getActivity()).startPreferencePanel(ResetNetworkConfirm.class.getName(), args, R.string.reset_network_confirm_title, null, null, 0);
    }

    private void establishInitialState() {
        this.mSubscriptionSpinner = (Spinner) this.mContentView.findViewById(R.id.reset_network_subscription);
        this.mSubscriptions = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (this.mSubscriptions == null || this.mSubscriptions.size() <= 0) {
            this.mSubscriptionSpinner.setVisibility(4);
        } else {
            int defaultSubscription = SubscriptionManager.getDefaultDataSubId();
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultVoiceSubId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSmsSubId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSubId();
            }
            int selectedIndex = 0;
            int size = this.mSubscriptions.size();
            List<String> subscriptionNames = new ArrayList();
            for (SubscriptionInfo record : this.mSubscriptions) {
                if (record.getSubscriptionId() == defaultSubscription) {
                    selectedIndex = subscriptionNames.size();
                }
                String name = record.getDisplayName().toString();
                if (TextUtils.isEmpty(name)) {
                    name = record.getNumber();
                }
                if (TextUtils.isEmpty(name)) {
                    name = record.getCarrierName().toString();
                }
                if (TextUtils.isEmpty(name)) {
                    name = String.format("MCC:%s MNC:%s Slot:%s Id:%s", new Object[]{Integer.valueOf(record.getMcc()), Integer.valueOf(record.getMnc()), Integer.valueOf(record.getSimSlotIndex()), Integer.valueOf(record.getSubscriptionId())});
                }
                subscriptionNames.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), 17367048, subscriptionNames);
            adapter.setDropDownViewResource(17367049);
            this.mSubscriptionSpinner.setAdapter(adapter);
            this.mSubscriptionSpinner.setSelection(selectedIndex);
            this.mSubscriptionSpinner.setVisibility(0);
        }
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_reset_network);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!Process.myUserHandle().isOwner() || UserManager.get(getActivity()).hasUserRestriction("no_network_reset")) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        }
        this.mContentView = inflater.inflate(R.layout.reset_network, null);
        establishInitialState();
        return this.mContentView;
    }

    protected int getMetricsCategory() {
        return 83;
    }
}
