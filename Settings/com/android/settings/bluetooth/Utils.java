package com.android.settings.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.bluetooth.DockService.DockBluetoothCallback;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.Utils.ErrorListener;

public final class Utils {
    private static final ErrorListener mErrorListener = new C03161();
    private static final BluetoothManagerCallback mOnInitCallback = new C03172();

    static class C03161 implements ErrorListener {
        C03161() {
        }

        public void onShowError(Context context, String name, int messageResId) {
            Utils.showError(context, name, messageResId);
        }
    }

    static class C03172 implements BluetoothManagerCallback {
        C03172() {
        }

        public void onBluetoothManagerInitialized(Context appContext, LocalBluetoothManager bluetoothManager) {
            bluetoothManager.getEventManager().registerCallback(new DockBluetoothCallback(appContext));
            com.android.settingslib.bluetooth.Utils.setErrorListener(Utils.mErrorListener);
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new Builder(getActivity()).setIcon(17301543).setTitle(R.string.bluetooth_error_title).setMessage(getArguments().getString("errorMessage")).setPositiveButton(17039370, null).show();
        }
    }

    private Utils() {
    }

    static AlertDialog showDisconnectDialog(Context context, AlertDialog dialog, OnClickListener disconnectListener, CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new Builder(context).setPositiveButton(17039370, disconnectListener).setNegativeButton(17039360, null).create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog.setButton(-1, context.getText(17039370), disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, new Object[]{name});
        LocalBluetoothManager manager = getLocalBtManager(context);
        if (manager != null) {
            Activity activity = (Activity) manager.getForegroundActivity();
            if (manager.isForegroundActivity()) {
                Log.d("Bluetooth.Utils", "show ErrorDialogFragment, message is " + message);
                ErrorDialogFragment dialog = new ErrorDialogFragment();
                Bundle args = new Bundle();
                args.putString("errorMessage", message);
                dialog.setArguments(args);
                dialog.show(activity.getFragmentManager(), "Error");
            } else {
                Toast.makeText(context, message, 0).show();
            }
        }
    }

    public static void updateSearchIndex(Context context, String className, String title, String screenTitle, int iconResId, boolean enabled) {
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = className;
        data.title = title;
        data.screenTitle = screenTitle;
        data.iconResId = iconResId;
        data.enabled = enabled;
        Index.getInstance(context).updateFromSearchIndexableData(data);
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }
}
