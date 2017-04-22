package com.android.settings;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.service.persistentdata.PersistentDataBlockManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class MasterClearConfirm extends InstrumentedFragment {
    private View mContentView;
    private boolean mEraseSdCard;
    private OnClickListener mFinalClickListener = new C01531();

    class C01531 implements OnClickListener {
        C01531() {
        }

        public void onClick(View v) {
            if (!Utils.isMonkeyRunning()) {
                final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager) MasterClearConfirm.this.getActivity().getSystemService("persistent_data_block");
                if (pdbManager == null || pdbManager.getOemUnlockEnabled() || Global.getInt(MasterClearConfirm.this.getActivity().getContentResolver(), "device_provisioned", 0) == 0) {
                    MasterClearConfirm.this.doMasterClear();
                } else {
                    new AsyncTask<Void, Void, Void>() {
                        int mOldOrientation;
                        ProgressDialog mProgressDialog;

                        protected Void doInBackground(Void... params) {
                            pdbManager.wipe();
                            return null;
                        }

                        protected void onPostExecute(Void aVoid) {
                            this.mProgressDialog.hide();
                            MasterClearConfirm.this.getActivity().setRequestedOrientation(this.mOldOrientation);
                            MasterClearConfirm.this.doMasterClear();
                        }

                        protected void onPreExecute() {
                            this.mProgressDialog = C01531.this.getProgressDialog();
                            this.mProgressDialog.show();
                            this.mOldOrientation = MasterClearConfirm.this.getActivity().getRequestedOrientation();
                            MasterClearConfirm.this.getActivity().setRequestedOrientation(14);
                        }
                    }.execute(new Void[0]);
                }
            }
        }

        private ProgressDialog getProgressDialog() {
            ProgressDialog progressDialog = new ProgressDialog(MasterClearConfirm.this.getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(MasterClearConfirm.this.getActivity().getString(R.string.master_clear_progress_title));
            progressDialog.setMessage(MasterClearConfirm.this.getActivity().getString(R.string.master_clear_progress_text));
            return progressDialog;
        }
    }

    private void doMasterClear() {
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(268435456);
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", this.mEraseSdCard);
        getActivity().sendBroadcast(intent);
    }

    private void establishFinalConfirmationState() {
        this.mContentView.findViewById(R.id.execute_master_clear).setOnClickListener(this.mFinalClickListener);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (UserManager.get(getActivity()).hasUserRestriction("no_factory_reset")) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        }
        this.mContentView = inflater.inflate(R.layout.master_clear_confirm, null);
        establishFinalConfirmationState();
        setAccessibilityTitle();
        return this.mContentView;
    }

    private void setAccessibilityTitle() {
        CharSequence currentTitle = getActivity().getTitle();
        TextView confirmationMessage = (TextView) this.mContentView.findViewById(R.id.master_clear_confirm);
        if (confirmationMessage != null) {
            getActivity().setTitle(Utils.createAccessibleSequence(currentTitle, "," + confirmationMessage.getText()));
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        boolean z;
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            z = args.getBoolean("erase_sd");
        } else {
            z = false;
        }
        this.mEraseSdCard = z;
    }

    protected int getMetricsCategory() {
        return 67;
    }
}
