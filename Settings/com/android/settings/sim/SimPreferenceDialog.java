package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Resources;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;

public class SimPreferenceDialog extends Activity {
    private final String SIM_NAME = "sim_name";
    private final String TINT_POS = "tint_pos";
    Builder mBuilder;
    private String[] mColorStrings;
    private Context mContext;
    View mDialogLayout;
    private ISettingsMiscExt mMiscExt;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private int mSlotId;
    private SubscriptionInfo mSubInfoRecord;
    private SubscriptionManager mSubscriptionManager;
    private int[] mTintArr;
    private int mTintSelectorPos;

    class C05191 implements OnSimHotSwapListener {
        C05191() {
        }

        public void onSimHotSwap() {
            Log.d("SimPreferenceDialog", "onSimHotSwap, finish Activity~~");
            SimPreferenceDialog.this.finish();
        }
    }

    class C05224 implements OnClickListener {
        C05224() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
            SimPreferenceDialog.this.finish();
        }
    }

    class C05235 implements OnKeyListener {
        C05235() {
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode != 4) {
                return false;
            }
            SimPreferenceDialog.this.finish();
            return true;
        }
    }

    class C05246 implements OnCancelListener {
        C05246() {
        }

        public void onCancel(DialogInterface dialog) {
            SimPreferenceDialog.this.finish();
        }
    }

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        private class ViewHolder {
            ImageView icon;
            TextView label;
            ShapeDrawable swatch;

            private ViewHolder() {
            }
        }

        public SelectColorAdapter(Context context, int resource, String[] arr) {
            super(context, resource, arr);
            this.mContext = context;
            this.mResId = resource;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView;
            ViewHolder holder;
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            Resources res = this.mContext.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
            int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);
            if (convertView == null) {
                rowView = inflater.inflate(this.mResId, null);
                holder = new ViewHolder();
                ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                drawable.setIntrinsicHeight(iconSize);
                drawable.setIntrinsicWidth(iconSize);
                drawable.getPaint().setStrokeWidth((float) strokeWidth);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                holder.swatch = drawable;
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            holder.label.setText((CharSequence) getItem(position));
            holder.swatch.getPaint().setColor(SimPreferenceDialog.this.mTintArr[position]);
            holder.swatch.getPaint().setStyle(Style.FILL_AND_STROKE);
            holder.icon.setVisibility(0);
            holder.icon.setImageDrawable(holder.swatch);
            return rowView;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            ViewHolder holder = (ViewHolder) rowView.getTag();
            if (SimPreferenceDialog.this.mTintSelectorPos == position) {
                holder.swatch.getPaint().setStyle(Style.FILL_AND_STROKE);
            } else {
                holder.swatch.getPaint().setStyle(Style.STROKE);
            }
            holder.icon.setVisibility(0);
            return rowView;
        }
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = this;
        this.mSlotId = getIntent().getExtras().getInt("slot_id", -1);
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mSubInfoRecord = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(this.mSlotId);
        if (this.mSubInfoRecord == null) {
            Log.w("SimPreferenceDialog", "mSubInfoRecord is null, finish the activity");
            finish();
            return;
        }
        this.mTintArr = this.mContext.getResources().getIntArray(17235978);
        this.mColorStrings = this.mContext.getResources().getStringArray(R.array.color_picker);
        this.mTintSelectorPos = 0;
        this.mBuilder = new Builder(this.mContext);
        this.mDialogLayout = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.multi_sim_dialog, null);
        this.mBuilder.setView(this.mDialogLayout);
        this.mMiscExt = UtilsExt.getMiscPlugin(getApplicationContext());
        this.mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getApplicationContext());
        createEditDialog(bundle);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C05191());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("tint_pos", this.mTintSelectorPos);
        savedInstanceState.putString("sim_name", ((EditText) this.mDialogLayout.findViewById(R.id.sim_name)).getText().toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int pos = savedInstanceState.getInt("tint_pos");
        ((Spinner) this.mDialogLayout.findViewById(R.id.spinner)).setSelection(pos);
        this.mTintSelectorPos = pos;
        ((EditText) this.mDialogLayout.findViewById(R.id.sim_name)).setText(savedInstanceState.getString("sim_name"));
    }

    private void createEditDialog(Bundle bundle) {
        Resources res = this.mContext.getResources();
        ((EditText) this.mDialogLayout.findViewById(R.id.sim_name)).setText(this.mSubInfoRecord.getDisplayName());
        customizeSimNameTitle(this.mDialogLayout);
        final Spinner tintSpinner = (Spinner) this.mDialogLayout.findViewById(R.id.spinner);
        SelectColorAdapter adapter = new SelectColorAdapter(this.mContext, R.layout.settings_color_picker_item, this.mColorStrings);
        adapter.setDropDownViewResource(17367049);
        tintSpinner.setAdapter(adapter);
        for (int i = 0; i < this.mTintArr.length; i++) {
            if (this.mTintArr[i] == this.mSubInfoRecord.getIconTint()) {
                tintSpinner.setSelection(i);
                this.mTintSelectorPos = i;
                break;
            }
        }
        tintSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                tintSpinner.setSelection(pos);
                SimPreferenceDialog.this.mTintSelectorPos = pos;
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        TextView numberView = (TextView) this.mDialogLayout.findViewById(R.id.number);
        String rawNumber = tm.getLine1NumberForSubscriber(this.mSubInfoRecord.getSubscriptionId());
        if (TextUtils.isEmpty(rawNumber)) {
            numberView.setText(res.getString(17039374));
        } else {
            numberView.setText(PhoneNumberUtils.formatNumber(rawNumber));
        }
        String simCarrierName = tm.getSimOperatorNameForSubscription(this.mSubInfoRecord.getSubscriptionId());
        TextView carrierView = (TextView) this.mDialogLayout.findViewById(R.id.carrier);
        if (TextUtils.isEmpty(simCarrierName)) {
            simCarrierName = this.mContext.getString(17039374);
        }
        carrierView.setText(simCarrierName);
        this.mBuilder.setTitle(String.format(res.getString(R.string.sim_editor_title), new Object[]{Integer.valueOf(this.mSubInfoRecord.getSimSlotIndex() + 1)}));
        customizeDialogTitle(this.mBuilder);
        this.mBuilder.setPositiveButton(R.string.okay, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String displayName = ((EditText) SimPreferenceDialog.this.mDialogLayout.findViewById(R.id.sim_name)).getText().toString();
                int subId = SimPreferenceDialog.this.mSubInfoRecord.getSubscriptionId();
                SimPreferenceDialog.this.mSubInfoRecord.setDisplayName(displayName);
                SimPreferenceDialog.this.mSubscriptionManager.setDisplayName(displayName, subId, 2);
                int tintSelected = tintSpinner.getSelectedItemPosition();
                int subscriptionId = SimPreferenceDialog.this.mSubInfoRecord.getSubscriptionId();
                int tint = SimPreferenceDialog.this.mTintArr[tintSelected];
                SimPreferenceDialog.this.mSubInfoRecord.setIconTint(tint);
                SimPreferenceDialog.this.mSubscriptionManager.setIconTint(tint, subscriptionId);
                dialog.dismiss();
                SimPreferenceDialog.this.finish();
            }
        });
        this.mBuilder.setNegativeButton(R.string.cancel, new C05224());
        this.mSimManagementExt.hideSimEditorView(this.mDialogLayout, this.mContext);
        this.mBuilder.setOnKeyListener(new C05235());
        this.mBuilder.setOnCancelListener(new C05246());
        this.mBuilder.create().show();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
    }

    private void customizeSimNameTitle(View dialogLayout) {
        int subId = -1;
        if (this.mSubInfoRecord != null) {
            subId = this.mSubInfoRecord.getSubscriptionId();
        }
        TextView nameTitle = (TextView) dialogLayout.findViewById(R.id.sim_name_title);
        nameTitle.setText(this.mMiscExt.customizeSimDisplayString(nameTitle.getText().toString(), subId));
        ((EditText) dialogLayout.findViewById(R.id.sim_name)).setHint(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_name_hint), subId));
    }

    private void customizeDialogTitle(Builder builder) {
        if (this.mSubInfoRecord != null) {
            builder.setTitle(String.format(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_editor_title), this.mSubInfoRecord.getSubscriptionId()), new Object[]{Integer.valueOf(this.mSubInfoRecord.getSimSlotIndex() + 1)}));
        }
    }
}
