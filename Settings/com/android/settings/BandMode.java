package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class BandMode extends Activity {
    private static final String[] BAND_NAMES = new String[]{"Automatic", "EURO Band", "USA Band", "JAPAN Band", "AUS Band", "AUS2 Band"};
    private ListView mBandList;
    private ArrayAdapter mBandListAdapter;
    private OnItemClickListener mBandSelectionHandler = new C00521();
    private Handler mHandler = new C00532();
    private Phone mPhone = null;
    private DialogInterface mProgressPanel;
    private BandListItem mTargetBand = null;

    class C00521 implements OnItemClickListener {
        C00521() {
        }

        public void onItemClick(AdapterView parent, View v, int position, long id) {
            BandMode.this.getWindow().setFeatureInt(5, -1);
            BandMode.this.mTargetBand = (BandListItem) parent.getAdapter().getItem(position);
            BandMode.this.mPhone.setBandMode(BandMode.this.mTargetBand.getBand(), BandMode.this.mHandler.obtainMessage(200));
        }
    }

    class C00532 extends Handler {
        C00532() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    BandMode.this.bandListLoaded(msg.obj);
                    return;
                case 200:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    BandMode.this.getWindow().setFeatureInt(5, -2);
                    if (!BandMode.this.isFinishing()) {
                        BandMode.this.displayBandSelectionResult(ar.exception);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private static class BandListItem {
        private int mBandMode = 0;

        public BandListItem(int bm) {
            this.mBandMode = bm;
        }

        public int getBand() {
            return this.mBandMode;
        }

        public String toString() {
            if (this.mBandMode >= BandMode.BAND_NAMES.length) {
                return "Band mode " + this.mBandMode;
            }
            return BandMode.BAND_NAMES[this.mBandMode];
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(5);
        setContentView(R.layout.band_mode);
        setTitle(getString(R.string.band_mode_title));
        getWindow().setLayout(-1, -2);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mBandList = (ListView) findViewById(R.id.band);
        this.mBandListAdapter = new ArrayAdapter(this, 17367043);
        this.mBandList.setAdapter(this.mBandListAdapter);
        this.mBandList.setOnItemClickListener(this.mBandSelectionHandler);
        loadBandList();
    }

    private void loadBandList() {
        this.mProgressPanel = new Builder(this).setMessage(getString(R.string.band_mode_loading)).show();
        this.mPhone.queryAvailableBandMode(this.mHandler.obtainMessage(100));
    }

    private void bandListLoaded(AsyncResult result) {
        int i;
        if (this.mProgressPanel != null) {
            this.mProgressPanel.dismiss();
        }
        clearList();
        boolean addBandSuccess = false;
        if (result.result != null) {
            int[] bands = result.result;
            int size = bands[0];
            if (size > 0) {
                for (i = 1; i < size; i++) {
                    this.mBandListAdapter.add(new BandListItem(bands[i]));
                }
                addBandSuccess = true;
            }
        }
        if (!addBandSuccess) {
            for (i = 0; i < 6; i++) {
                this.mBandListAdapter.add(new BandListItem(i));
            }
        }
        this.mBandList.requestFocus();
    }

    private void displayBandSelectionResult(Throwable ex) {
        String status = getString(R.string.band_mode_set) + " [" + this.mTargetBand.toString() + "] ";
        if (ex != null) {
            status = status + getString(R.string.band_mode_failed);
        } else {
            status = status + getString(R.string.band_mode_succeeded);
        }
        this.mProgressPanel = new Builder(this).setMessage(status).setPositiveButton(17039370, null).show();
    }

    private void clearList() {
        while (this.mBandListAdapter.getCount() > 0) {
            this.mBandListAdapter.remove(this.mBandListAdapter.getItem(0));
        }
    }
}
