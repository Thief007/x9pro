package com.android.settings.notification;

import android.app.INotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.IConditionListener.Stub;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.List;

public class ZenModeConditionSelection extends RadioGroup {
    private Condition mCondition;
    private final List<Condition> mConditions;
    private final Context mContext;
    private final C0468H mHandler;
    private final IConditionListener mListener;
    private final INotificationManager mNoMan;

    class C04661 extends Stub {
        final /* synthetic */ ZenModeConditionSelection this$0;

        public void onConditionsReceived(Condition[] conditions) {
            if (conditions != null && conditions.length != 0) {
                this.this$0.mHandler.obtainMessage(1, conditions).sendToTarget();
            }
        }
    }

    private final class C0468H extends Handler {
        final /* synthetic */ ZenModeConditionSelection this$0;

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                this.this$0.handleConditions((Condition[]) msg.obj);
            }
        }
    }

    private RadioButton newRadioButton(Condition condition) {
        final RadioButton button = new RadioButton(this.mContext);
        button.setTag(condition);
        button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ZenModeConditionSelection.this.setCondition((Condition) button.getTag());
                }
            }
        });
        addView(button);
        return button;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestZenModeConditions(1);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        requestZenModeConditions(0);
    }

    protected void requestZenModeConditions(int relevance) {
        Log.d("ZenModeConditionSelection", "requestZenModeConditions " + Condition.relevanceToString(relevance));
        try {
            this.mNoMan.requestZenModeConditions(this.mListener, relevance);
        } catch (RemoteException e) {
        }
    }

    protected void handleConditions(Condition[] conditions) {
        for (Condition c : conditions) {
            handleCondition(c);
        }
    }

    protected void handleCondition(Condition c) {
        boolean z = true;
        if (!this.mConditions.contains(c)) {
            RadioButton v = (RadioButton) findViewWithTag(c.id);
            if ((c.state == 1 || c.state == 2) && v == null) {
                v = newRadioButton(c);
            }
            if (v != null) {
                v.setText(computeConditionText(c));
                if (c.state != 1) {
                    z = false;
                }
                v.setEnabled(z);
            }
            this.mConditions.add(c);
        }
    }

    protected void setCondition(Condition c) {
        Log.d("ZenModeConditionSelection", "setCondition " + c);
        this.mCondition = c;
    }

    private static String computeConditionText(Condition c) {
        if (!TextUtils.isEmpty(c.line1)) {
            return c.line1;
        }
        if (TextUtils.isEmpty(c.summary)) {
            return "";
        }
        return c.summary;
    }
}
