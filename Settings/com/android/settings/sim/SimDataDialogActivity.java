package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class SimDataDialogActivity extends Activity {
    private static String TAG = "SimDataDialogActivity";
    private BroadcastReceiver mSubReceiver = new C05071();

    class C05071 extends BroadcastReceiver {
        C05071() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d(SimDataDialogActivity.TAG, "mSubReceiver action = " + intent.getAction());
            SimDataDialogActivity.this.finish();
        }
    }

    class C05093 implements OnKeyListener {
        C05093() {
        }

        public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                SimDataDialogActivity.this.finish();
            }
            return true;
        }
    }

    class C05104 implements OnCancelListener {
        C05104() {
        }

        public void onCancel(DialogInterface dialogInterface) {
            SimDataDialogActivity.this.finish();
        }
    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private final float OPACITY = 0.54f;
        private Context mContext;
        private int mDialogId;
        private int mResId;
        private List<SubscriptionInfo> mSubInfoList;

        private class ViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private ViewHolder() {
            }
        }

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList, Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            this.mContext = context;
            this.mResId = resource;
            this.mDialogId = dialogId;
            this.mSubInfoList = subInfoList;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView;
            ViewHolder holder;
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            if (convertView == null) {
                rowView = inflater.inflate(this.mResId, null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            SubscriptionInfo sir = (SubscriptionInfo) this.mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText((CharSequence) getItem(position));
                holder.summary.setText("");
                holder.icon.setImageDrawable(SimDataDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(0.54f);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(this.mContext));
            }
            return rowView;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switch (null) {
            case null:
                createDialog(this, 0).show();
                Log.e(TAG, "set data pick");
                return;
            default:
                throw new IllegalArgumentException("Invalid dialog type 0 sent.");
        }
    }

    private static void setDefaultDataSubId(Context context, int subId) {
        if (TelecomManager.from(context).isInCall()) {
            Toast.makeText(context, R.string.default_data_switch_err_msg1, 0).show();
            return;
        }
        SubscriptionManager.from(context).setDefaultDataSubId(subId);
        Toast.makeText(context, R.string.data_switch_started, 1).show();
    }

    public Dialog createDialog(Context context, int id) {
        ArrayList<String> list = new ArrayList();
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();
        final int i = id;
        final List<SubscriptionInfo> list2 = subInfoList;
        final Context context2 = context;
        OnClickListener c05082 = new OnClickListener() {
            public void onClick(DialogInterface dialog, int value) {
                switch (i) {
                    case 0:
                        SimDataDialogActivity.setDefaultDataSubId(context2, ((SubscriptionInfo) list2.get(value)).getSubscriptionId());
                        SimDataDialogActivity.this.setMobileDataEnabled(context2, true);
                        if (SubscriptionManager.getDefaultDataSubId() >= 0) {
                            SimDataDialogActivity.this.finish();
                        } else {
                            SimDataDialogActivity.this.finish();
                        }
                        return;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + i + " in SIM dialog.");
                }
            }
        };
        OnKeyListener c05093 = new C05093();
        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList();
        ArrayList<SubscriptionInfo> smsSubInfoList = new ArrayList();
        for (int i2 = 0; i2 < selectableSubInfoLength; i2++) {
            CharSequence displayName = ((SubscriptionInfo) subInfoList.get(i2)).getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }
        String[] arr = (String[]) list.toArray(new String[0]);
        Builder builder = new Builder(context);
        ListAdapter adapter = new SelectAccountListAdapter(getAdapterData(id, subInfoList, callsSubInfoList, smsSubInfoList), builder.getContext(), R.layout.select_account_list_item, arr, id);
        switch (id) {
            case 0:
                builder.setTitle(R.string.select_sim_for_data);
                Dialog dialog = builder.setAdapter(adapter, c05082).create();
                dialog.setOnKeyListener(c05093);
                dialog.setOnCancelListener(new C05104());
                return dialog;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    private List<SubscriptionInfo> getAdapterData(int id, List<SubscriptionInfo> subInfoList, ArrayList<SubscriptionInfo> arrayList, ArrayList<SubscriptionInfo> arrayList2) {
        switch (id) {
            case 0:
                return subInfoList;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    public void setMobileDataEnabled(Context context, boolean enabled) {
        Log.d(TAG, "setMobileDataEnabled: enabled=" + enabled);
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        mTelephonyManager.setDataEnabled(enabled);
        if (TelephonyManager.getDefault().getSimCount() > 1 && enabled) {
            int subId = SubscriptionManager.getDefaultDataSubId();
            if (subId >= 0) {
                int phoneId;
                if (SubscriptionManager.getPhoneId(subId) == 0) {
                    phoneId = 1;
                } else {
                    phoneId = 0;
                }
                subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
                if (subId >= 0 && mTelephonyManager.getDataEnabled(subId)) {
                    mTelephonyManager.setDataEnabled(subId, false);
                }
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }
}
