package com.android.settings.deviceinfo;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

public class PrivateVolumeForget extends InstrumentedFragment {
    private final OnClickListener mConfirmListener = new C03231();
    private VolumeRecord mRecord;

    class C03231 implements OnClickListener {
        C03231() {
        }

        public void onClick(View v) {
            ForgetConfirmFragment.show(PrivateVolumeForget.this, PrivateVolumeForget.this.mRecord.getFsUuid());
        }
    }

    public static class ForgetConfirmFragment extends DialogFragment {
        public static void show(Fragment parent, String fsUuid) {
            Bundle args = new Bundle();
            args.putString("android.os.storage.extra.FS_UUID", fsUuid);
            ForgetConfirmFragment dialog = new ForgetConfirmFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "forget_confirm");
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
            final String fsUuid = getArguments().getString("android.os.storage.extra.FS_UUID");
            VolumeRecord record = storage.findRecordByUuid(fsUuid);
            Builder builder = new Builder(context);
            builder.setTitle(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_confirm_title), new CharSequence[]{record.getNickname()}));
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_confirm), new CharSequence[]{record.getNickname()}));
            builder.setPositiveButton(R.string.storage_menu_forget, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    storage.forgetVolume(fsUuid);
                    ForgetConfirmFragment.this.getActivity().finish();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }

    protected int getMetricsCategory() {
        return 42;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mRecord = ((StorageManager) getActivity().getSystemService(StorageManager.class)).findRecordByUuid(getArguments().getString("android.os.storage.extra.FS_UUID"));
        View view = inflater.inflate(R.layout.storage_internal_forget, container, false);
        Button confirm = (Button) view.findViewById(R.id.confirm);
        ((TextView) view.findViewById(R.id.body)).setText(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_details), new CharSequence[]{this.mRecord.getNickname()}));
        confirm.setOnClickListener(this.mConfirmListener);
        return view;
    }
}
