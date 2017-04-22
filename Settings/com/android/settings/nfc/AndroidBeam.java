package com.android.settings.nfc;

import android.app.ActionBar;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.mediatek.beam.BeamShareHistory;
import com.mediatek.settings.FeatureOption;

public class AndroidBeam extends InstrumentedFragment implements OnSwitchChangeListener {
    private boolean mBeamDisallowed;
    private NfcAdapter mNfcAdapter;
    private CharSequence mOldActivityTitle;
    private SwitchBar mSwitchBar;
    private View mView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActivity().getActionBar();
        this.mOldActivityTitle = actionBar.getTitle();
        actionBar.setTitle(R.string.android_beam_settings_title);
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        this.mBeamDisallowed = ((UserManager) getActivity().getSystemService("user")).hasUserRestriction("no_outgoing_beam");
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, (int) R.string.help_uri_beam, getClass().getName());
        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
            menu.add(0, 0, 0, R.string.beam_share_history_title).setShowAsAction(1);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            ((SettingsActivity) getActivity()).startPreferencePanel(BeamShareHistory.class.getName(), null, 0, null, null, 0);
        }
        return super.onOptionsItemSelected(item);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
            this.mView = inflater.inflate(R.layout.android_beam_plus, container, false);
            Utils.prepareCustomPreferencesList(container, this.mView, this.mView, false);
        } else {
            this.mView = inflater.inflate(R.layout.android_beam, container, false);
        }
        return this.mView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        boolean z;
        boolean z2 = false;
        super.onActivityCreated(savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        SwitchBar switchBar = this.mSwitchBar;
        if (this.mBeamDisallowed) {
            z = false;
        } else {
            z = this.mNfcAdapter.isNdefPushEnabled();
        }
        switchBar.setChecked(z);
        this.mSwitchBar.addOnSwitchChangeListener(this);
        SwitchBar switchBar2 = this.mSwitchBar;
        if (!this.mBeamDisallowed) {
            z2 = true;
        }
        switchBar2.setEnabled(z2);
        this.mSwitchBar.show();
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (this.mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(this.mOldActivityTitle);
        }
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        boolean success;
        this.mSwitchBar.setEnabled(false);
        if (desiredState) {
            success = this.mNfcAdapter.enableNdefPush();
        } else {
            success = this.mNfcAdapter.disableNdefPush();
        }
        if (success) {
            this.mSwitchBar.setChecked(desiredState);
        }
        this.mSwitchBar.setEnabled(true);
    }

    protected int getMetricsCategory() {
        return 69;
    }
}
