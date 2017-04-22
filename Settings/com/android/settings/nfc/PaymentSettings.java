package com.android.settings.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import java.util.List;

public class PaymentSettings extends SettingsPreferenceFragment {
    private PaymentBackend mPaymentBackend;

    protected int getMetricsCategory() {
        return 70;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPaymentBackend = new PaymentBackend(getActivity());
        setHasOptionsMenu(true);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(R.layout.nfc_payment_empty, contentRoot, false);
        contentRoot.addView(emptyView);
        getListView().setEmptyView(emptyView);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
        List<PaymentAppInfo> appInfos = this.mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            screen.addPreference(new NfcPaymentPreference(getActivity(), this.mPaymentBackend));
            screen.addPreference(new NfcForegroundPreference(getActivity(), this.mPaymentBackend));
        }
        setPreferenceScreen(screen);
    }

    public void onResume() {
        super.onResume();
        this.mPaymentBackend.onResume();
    }

    public void onPause() {
        super.onPause();
        this.mPaymentBackend.onPause();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.add(R.string.nfc_payment_how_it_works);
        menuItem.setIntent(new Intent(getActivity(), HowItWorks.class));
        menuItem.setShowAsActionFlags(0);
    }
}
