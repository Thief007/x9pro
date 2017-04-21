package com.android.systemui.qs;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;

public class QSContainer extends FrameLayout {
    LinearLayout google_search;
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            QSContainer.this.val = data.getString("value");
            Log.d("QSContainer", Log.getStackTraceString(new Throwable()));
            if (QSContainer.this.val != null) {
                QSContainer.this.keyword.setText(QSContainer.this.val);
            }
        }
    };
    TextView keyword;
    LinearLayout ll;
    Context mContext;
    private int mHeightOverride = -1;
    ArrayList<String> mList = new ArrayList();
    private QSPanel mQSPanel;
    public Timer mTimer = new Timer();
    String path2 = "http://nanohome.cn/get_keywords/geo_getcitywords.php";
    public Runnable runnable = new Runnable() {
        public void run() {
            QSContainer.this.mTimer.schedule(new TimerTask() {
                public void run() {
                    AnonymousClass2.this.update();
                    Log.i("QSContainer", "runnable");
                }
            }, 10000, 30000);
        }

        void update() {
            Message msg = new Message();
            Bundle data = new Bundle();
            String key = QSContainer.this.getTextStringFromNet();
            data.putString("value", key);
            Log.i("runnable", "keyword" + key);
            msg.setData(data);
            QSContainer.this.handler.sendMessage(msg);
        }
    };
    String val;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        this.ll = (LinearLayout) findViewById(R.id.quick_settings_panel2);
        this.mQSPanel = (QSPanel) this.ll.findViewById(R.id.quick_settings_panel);
        this.google_search = (LinearLayout) this.ll.findViewById(R.id.google_search);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBottom();
        this.google_search.setVisibility(8);
    }

    public void setHeightOverride(int heightOverride) {
        this.mHeightOverride = heightOverride;
        updateBottom();
    }

    public static byte[] getTextData(String path) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setConnectTimeout(5000);
        InputStream inStream = conn.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int len = inStream.read(buffer);
            if (len == -1) {
                return bos.toByteArray();
            }
            bos.write(buffer, 0, len);
        }
    }

    private String getTextStringFromNet() {
        String str = null;
        Random random = new Random();
        try {
            String[] keywordArray = new JSONObject(new String(getTextData(this.path2), "UTF-8")).getString("keyword").replace("[", "").replace("]", "").replace("\"", "").split(",");
            return keywordArray[random.nextInt(keywordArray.length)];
        } catch (Exception e) {
            return str;
        }
    }

    public int getDesiredHeight() {
        if (this.mQSPanel.isClosingDetail()) {
            return (this.mQSPanel.getGridHeight() + getPaddingTop()) + getPaddingBottom();
        }
        return getMeasuredHeight();
    }

    private void updateBottom() {
        setBottom(getTop() + (this.mHeightOverride != -1 ? this.mHeightOverride : getMeasuredHeight()));
    }
}
