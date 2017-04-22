package com.mediatek.beam;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.beam.BeamShareTask.BeamShareTaskMetaData;
import com.mediatek.beam.BeamShareTask.Direction;
import java.io.File;
import java.util.ArrayList;

public class BeamShareHistory extends SettingsPreferenceFragment implements OnClickListener, Callback {
    private Activity mActivity;
    private BeamShareTabAdapter mAdapter;
    private String mCilckedFileName;
    private ContentResolver mContentReslover;
    private int mCurrentDialogId = -1;
    private int mCurrentTabIndex = 0;
    private Cursor mCursor = null;
    private Handler mHandler;
    private LayoutInflater mInflater;
    private Uri mLongClickUri;
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("@M_BeamShareHistory", "ContentObserver, onChange()");
            BeamShareHistory.this.mActivity.invalidateOptionsMenu();
        }
    };
    private OnTabChangeListener mTabListener = new C06962();
    private final ArrayList<TabInfo> mTabs = new ArrayList();

    class C06962 implements OnTabChangeListener {
        C06962() {
        }

        public void onTabChanged(String tabId) {
            Log.d("@M_BeamShareHistory", "OnTabChanged");
            if (tabId.equals("Incoming")) {
                BeamShareHistory.this.mCurrentTabIndex = 0;
            } else if (tabId.equals("Outgoing")) {
                BeamShareHistory.this.mCurrentTabIndex = 1;
            }
            Log.d("@M_BeamShareHistory", "mCurrentTabIndex" + BeamShareHistory.this.mCurrentTabIndex);
            BeamShareHistory.this.mCursor = ((TabInfo) BeamShareHistory.this.mTabs.get(BeamShareHistory.this.mCurrentTabIndex)).getCursor();
            BeamShareHistory.this.mActivity.invalidateOptionsMenu();
        }
    }

    private class ClearTask extends AsyncTask<String, Void, Integer> {
        private ClearTask() {
        }

        protected Integer doInBackground(String... arg) {
            clearAllTasks();
            return Integer.valueOf(0);
        }

        private void clearAllTasks() {
            int columnIndex = BeamShareHistory.this.mCursor.getColumnIndexOrThrow("_id");
            ArrayList<Uri> uris = new ArrayList();
            BeamShareHistory.this.mCursor.moveToFirst();
            while (!BeamShareHistory.this.mCursor.isAfterLast()) {
                uris.add(Uri.withAppendedPath(BeamShareTaskMetaData.CONTENT_URI, Integer.toString(BeamShareHistory.this.mCursor.getInt(columnIndex))));
                Log.d("@M_BeamShareHistory", "clearAllTasks-----mCursor.getCount(): " + BeamShareHistory.this.mCursor.getCount());
                BeamShareHistory.this.mCursor.moveToNext();
            }
            for (Uri uri : uris) {
                BeamShareHistory.this.mContentReslover.delete(uri, null, null);
            }
        }

        protected void onPostExecute(Integer result) {
            if (result.intValue() == 0) {
                BeamShareHistory.this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    public class TabInfo implements OnItemClickListener, OnItemLongClickListener {
        private TabContentFactory mEmptyTabContent = new C06971();
        private boolean mIsIncoming;
        public final CharSequence mLabel;
        private ListView mListView;
        public final BeamShareHistory mOwner;
        public View mRootView;
        private Cursor mTabCursor = null;

        class C06971 implements TabContentFactory {
            C06971() {
            }

            public View createTabContent(String tag) {
                return TabInfo.this.buildTabSpec();
            }
        }

        public TabInfo(BeamShareHistory owner, CharSequence label, boolean isIncoming) {
            this.mOwner = owner;
            this.mLabel = label;
            this.mIsIncoming = isIncoming;
        }

        public void build(LayoutInflater inflater, TabHost tabHost) {
            Log.d("@M_BeamShareHistory", "build");
            BeamShareHistory.this.mInflater = inflater;
            tabHost.addTab(tabHost.newTabSpec(this.mIsIncoming ? "Incoming" : "Outgoing").setIndicator(this.mLabel).setContent(this.mEmptyTabContent));
        }

        private View buildTabSpec() {
            if (this.mRootView != null) {
                return this.mRootView;
            }
            this.mRootView = BeamShareHistory.this.mInflater.inflate(R.layout.beam_share_mgmt_tab, null);
            this.mListView = (ListView) this.mRootView.findViewById(16908298);
            try {
                String str;
                ContentResolver contentResolver = this.mOwner.mActivity.getContentResolver();
                Uri uri = BeamShareTaskMetaData.CONTENT_URI;
                if (this.mIsIncoming) {
                    str = "type in (0,2)";
                } else {
                    str = "type in (1,3)";
                }
                this.mTabCursor = contentResolver.query(uri, null, str, null, "_id DESC");
            } catch (CursorIndexOutOfBoundsException ex) {
                Log.e("@M_BeamShareHistory", "check empty share list error:", ex);
            }
            if (this.mTabCursor != null) {
                Log.d("@M_BeamShareHistory", "tab " + this.mLabel + ": cursor.getCount() " + this.mTabCursor.getCount());
                BeamShareHistory.this.mAdapter = new BeamShareTabAdapter(this.mOwner.mActivity, R.layout.beam_share_mgmt_item, this.mTabCursor);
                this.mListView.setAdapter(BeamShareHistory.this.mAdapter);
                this.mListView.setScrollBarStyle(16777216);
                this.mListView.setOnItemClickListener(this);
                this.mListView.setOnItemLongClickListener(this);
            }
            return this.mRootView;
        }

        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            this.mTabCursor.moveToPosition(position);
            Log.d("@M_BeamShareHistory", "onItemClick");
            BeamShareTask task = new BeamShareTask(this.mTabCursor);
            String fileData = task.getData();
            if (fileData == null) {
                Log.d("@M_BeamShareHistory", "the file name is null");
                BeamShareHistory.this.mCilckedFileName = fileData;
            } else {
                BeamShareHistory.this.mCilckedFileName = new File(fileData).getName();
            }
            if (task.getState() == 1) {
                openTransferSuccessFile(task.getData(), task.getMimeType());
            } else if (task.getDirection() == Direction.out) {
                this.mOwner.showDialog(2);
            } else {
                this.mOwner.showDialog(4);
            }
        }

        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
            Log.d("@M_BeamShareHistory", "onItemLongClick");
            this.mTabCursor.moveToPosition(position);
            BeamShareHistory.this.mLongClickUri = new BeamShareTask(this.mTabCursor).getTaskUri();
            Log.d("@M_BeamShareHistory", "Click uri: " + BeamShareHistory.this.mLongClickUri);
            BeamShareHistory.this.showDialog(5);
            return true;
        }

        private void openTransferSuccessFile(String filename, String mimeType) {
            Log.d("@M_BeamShareHistory", "openTransferSuccessFile(): filename=" + filename + " mimetype=" + mimeType);
            if (filename == null) {
                Log.d("@M_BeamShareHistory", "the file name is null");
                return;
            }
            File file = new File(filename);
            Log.d("@M_BeamShareHistory", "file name is " + file.getName());
            Uri path = Uri.parse(filename);
            if (path.getScheme() == null) {
                path = Uri.fromFile(file);
            }
            if (isSupportedDataType(path, mimeType)) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setDataAndType(path, mimeType);
                intent.setFlags(268435456);
                this.mOwner.mActivity.startActivity(intent);
            } else {
                this.mOwner.showDialog(1);
            }
        }

        private boolean isSupportedDataType(Uri data, String mimeType) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(data, mimeType);
            if (this.mOwner.mActivity.getPackageManager().queryIntentActivities(intent, 65536).size() != 0) {
                return true;
            }
            Log.d("@M_BeamShareHistory", "cannot find proper Activity to handle Intent: mime[" + mimeType + "], data[" + data + "]");
            return false;
        }

        public Cursor getCursor() {
            return this.mTabCursor;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("@M_BeamShareHistory", "onCreate()");
        this.mActivity = getActivity();
        if (this.mActivity instanceof PreferenceActivity) {
            Log.d("@M_BeamShareHistory", "onCreate() set title");
            this.mActivity.getActionBar().setTitle(R.string.beam_share_history_title);
        }
        this.mContentReslover = this.mActivity.getContentResolver();
        this.mTabs.add(new TabInfo(this, this.mActivity.getString(R.string.beam_mgmt_tab_download_title), true));
        this.mTabs.add(new TabInfo(this, this.mActivity.getString(R.string.beam_mgmt_tab_upload_title), false));
        if (this.mHandler == null) {
            this.mHandler = new Handler(this);
        }
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            this.mCilckedFileName = savedInstanceState.getString("click_file_name");
            this.mLongClickUri = (Uri) savedInstanceState.getParcelable("long_click_uri");
            this.mCurrentTabIndex = savedInstanceState.getInt("current_tab_index", this.mCurrentTabIndex);
        }
    }

    public void onDestroy() {
        int size = this.mTabs.size();
        for (int i = 0; i < size; i++) {
            Cursor cursor = ((TabInfo) this.mTabs.get(i)).getCursor();
            if (cursor != null) {
                cursor.close();
            }
        }
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        this.mActivity.invalidateOptionsMenu();
        getContentResolver().registerContentObserver(BeamShareTaskMetaData.CONTENT_URI, false, this.mSettingsObserver);
    }

    public void onPause() {
        super.onPause();
        Log.d("@M_BeamShareHistory", "onPause");
        getContentResolver().unregisterContentObserver(this.mSettingsObserver);
        Log.d("@M_BeamShareHistory", "unregister content obsever");
    }

    public boolean handleMessage(Message msg) {
        Log.d("@M_BeamShareHistory", "handleMessage: " + msg.what);
        if (msg.what != 1) {
            return false;
        }
        this.mActivity.invalidateOptionsMenu();
        return true;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String str;
        Log.d("@M_BeamShareHistory", "onCreateView");
        this.mInflater = inflater;
        View rootView = this.mInflater.inflate(R.layout.beam_share_mgmt, container, false);
        TabHost tabHost = (TabHost) rootView.findViewById(16908306);
        tabHost.setup();
        int size = this.mTabs.size();
        for (int i = 0; i < size; i++) {
            ((TabInfo) this.mTabs.get(i)).build(this.mInflater, tabHost);
        }
        tabHost.setOnTabChangedListener(this.mTabListener);
        tabHost.setCurrentTab(this.mCurrentTabIndex);
        OnTabChangeListener onTabChangeListener = this.mTabListener;
        if (this.mCurrentTabIndex == 0) {
            str = "Incoming";
        } else {
            str = "Outgoing";
        }
        onTabChangeListener.onTabChanged(str);
        return rootView;
    }

    public Dialog onCreateDialog(int id) {
        this.mCurrentDialogId = id;
        switch (id) {
            case 1:
                return new Builder(this.mActivity).setTitle(R.string.beam_share_open_file_dialog_title).setMessage(getString(R.string.beam_share_open_file_unsupported_message)).setPositiveButton(17039379, null).create();
            case 2:
                return new Builder(this.mActivity).setTitle(R.string.beam_share_transfer_fail_dialog_title).setMessage(getString(R.string.beam_share_resend_dialog_message, new Object[]{this.mCilckedFileName})).setPositiveButton(17039379, null).create();
            case 3:
                return new Builder(this.mActivity).setTitle(R.string.beam_share_clear_confirm_dialog_title).setMessage(getString(R.string.beam_share_clear_confirm_dialog_message)).setPositiveButton(17039370, this).setNegativeButton(17039369, null).create();
            case 4:
                return new Builder(this.mActivity).setTitle(R.string.beam_share_transfer_fail_dialog_title).setMessage(getString(R.string.beam_share_receive_fail_dialog_message, new Object[]{this.mCilckedFileName})).setPositiveButton(17039370, null).create();
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                return new Builder(this.mActivity).setTitle(R.string.beam_share_clear_item_dailog_title).setIcon(17301543).setMessage(getString(R.string.beam_share_clear_item_dailog_message)).setPositiveButton(R.string.beam_share_clear_item_dailog_title, this).setNegativeButton(17039369, null).create();
            default:
                return null;
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button != -1) {
            Log.d("@M_BeamShareHistory", "DialogInterface onClick return");
            return;
        }
        if (this.mCurrentDialogId == 3) {
            new ClearTask().execute(new String[0]);
        } else if (this.mCurrentDialogId == 5) {
            Log.d("@M_BeamShareHistory", "Long click uri: " + this.mLongClickUri);
            this.mContentReslover.delete(this.mLongClickUri, null, null);
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 0, 0, R.string.beam_share_mgmt_tab_menu_clear).setShowAsAction(5);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        Log.d("@M_BeamShareHistory", "onPrepareOptionsMenu");
        if (this.mCursor != null) {
            boolean menuEnabled = this.mCursor.getCount() > 0;
            Log.d("@M_BeamShareHistory", "Menu enabled status is " + this.mCursor.getCount());
            menu.findItem(0).setEnabled(menuEnabled);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 0) {
            return false;
        }
        showDialog(3);
        return true;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("click_file_name", this.mCilckedFileName);
        outState.putParcelable("long_click_uri", this.mLongClickUri);
        outState.putInt("current_tab_index", this.mCurrentTabIndex);
    }

    protected int getMetricsCategory() {
        return 69;
    }
}
