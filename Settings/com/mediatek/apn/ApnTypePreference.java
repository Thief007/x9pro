package com.mediatek.apn;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

public class ApnTypePreference extends DialogPreference implements OnMultiChoiceClickListener {
    private String[] mApnTypeArray;
    private int mApnTypeNum;
    private boolean[] mCheckState;
    private ListView mListView;
    private String mTypeString;
    private boolean[] mUiCheckState;

    public ApnTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String apnType = null;
        if (context instanceof Activity) {
            apnType = ((Activity) context).getIntent().getStringExtra("apn_type");
        }
        boolean isTethering = "tethering".equals(apnType);
        String[] tempArray = UtilsExt.getApnSettingsPlugin(context).getApnTypeArray(context.getResources().getStringArray(R.array.apn_type_generic), context, apnType);
        Log.d("ApnTypePreference", "isTethering " + isTethering + " FeatureOption.MTK_VOLTE_SUPPORT" + FeatureOption.MTK_VOLTE_SUPPORT);
        if (!FeatureOption.MTK_VOLTE_SUPPORT || isTethering) {
            this.mApnTypeArray = tempArray;
        } else {
            this.mApnTypeArray = new String[(tempArray.length + 1)];
            for (int i = 0; i < tempArray.length; i++) {
                this.mApnTypeArray[i] = tempArray[i];
            }
            this.mApnTypeArray[this.mApnTypeArray.length - 1] = "ims";
        }
        if (this.mApnTypeArray != null) {
            this.mApnTypeNum = this.mApnTypeArray.length;
        }
        this.mCheckState = new boolean[this.mApnTypeNum];
    }

    public ApnTypePreference(Context context) {
        this(context, null);
    }

    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setMultiChoiceItems(this.mApnTypeArray, this.mCheckState, this);
        this.mListView = builder.create().getListView();
        this.mUiCheckState = new boolean[this.mApnTypeNum];
        for (int i = 0; i < this.mApnTypeNum; i++) {
            this.mUiCheckState[i] = this.mCheckState[i];
            Log.i("ApnTypePreference", "onPrepareDialogBuilder mUiCheckState[" + i + "]=" + this.mUiCheckState[i]);
        }
    }

    private void updateUiCheckBoxStatus() {
        for (int i = 0; i < this.mApnTypeNum; i++) {
            this.mCheckState[i] = this.mUiCheckState[i];
            Log.i("ApnTypePreference", "updateUiCheckBoxStatus mCheckState[" + i + "]=" + this.mCheckState[i]);
        }
    }

    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            updateUiCheckBoxStatus();
            updateRecord();
            callChangeListener(this.mTypeString);
            return;
        }
        initCheckedState(this.mTypeString);
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        this.mCheckState[which] = isChecked;
        this.mUiCheckState[which] = isChecked;
    }

    private void updateRecord() {
        if (this.mListView != null) {
            StringBuilder strTemp = new StringBuilder("");
            for (int i = 0; i < this.mApnTypeNum; i++) {
                if (this.mCheckState[i]) {
                    strTemp.append(this.mApnTypeArray[i]).append(',');
                }
            }
            int length = strTemp.length();
            if (length > 1) {
                this.mTypeString = strTemp.substring(0, length - 1);
            } else {
                this.mTypeString = "";
            }
            Log.i("ApnTypePreference", "mTypeString is " + this.mTypeString);
        }
    }

    public void initCheckedState(String strType) {
        Log.d("ApnTypePreference", "init CheckState: " + strType);
        if (strType != null) {
            this.mTypeString = strType;
            for (int i = 0; i < this.mApnTypeNum; i++) {
                this.mCheckState[i] = strType.contains(this.mApnTypeArray[i]);
            }
        }
    }

    public String getTypeString() {
        return this.mTypeString;
    }
}
