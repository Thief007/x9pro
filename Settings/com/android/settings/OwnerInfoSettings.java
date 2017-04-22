package com.android.settings;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.android.internal.widget.LockPatternUtils;

public class OwnerInfoSettings extends DialogFragment implements OnClickListener {
    private LockPatternUtils mLockPatternUtils;
    private EditText mOwnerInfo;
    private int mUserId;
    private View mView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUserId = UserHandle.myUserId();
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mView = LayoutInflater.from(getActivity()).inflate(R.layout.ownerinfo, null);
        initView();
        return new Builder(getActivity()).setTitle(R.string.owner_info_settings_title).setView(this.mView).setPositiveButton(R.string.save, this).setNegativeButton(R.string.cancel, this).show();
    }

    private void initView() {
        String info = this.mLockPatternUtils.getOwnerInfo(this.mUserId);
        this.mOwnerInfo = (EditText) this.mView.findViewById(R.id.owner_info_edit_text);
        if (!TextUtils.isEmpty(info)) {
            this.mOwnerInfo.setText(info);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            String info = this.mOwnerInfo.getText().toString();
            this.mLockPatternUtils.setOwnerInfoEnabled(!TextUtils.isEmpty(info), this.mUserId);
            this.mLockPatternUtils.setOwnerInfo(info, this.mUserId);
            if (getTargetFragment() instanceof SecuritySettings) {
                ((SecuritySettings) getTargetFragment()).updateOwnerInfo();
            }
        }
    }

    public static void show(Fragment parent) {
        if (parent.isAdded()) {
            OwnerInfoSettings dialog = new OwnerInfoSettings();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "ownerInfo");
        }
    }
}
