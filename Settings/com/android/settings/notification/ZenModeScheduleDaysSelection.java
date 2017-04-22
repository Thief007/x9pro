package com.android.settings.notification;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.android.settings.R;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleDaysSelection extends ScrollView {
    public static final int[] DAYS = new int[]{1, 2, 3, 4, 5, 6, 7};
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEEE");
    private final SparseBooleanArray mDays = new SparseBooleanArray();
    private final LinearLayout mLayout = new LinearLayout(this.mContext);

    public ZenModeScheduleDaysSelection(Context context, int[] days) {
        super(context);
        int hPad = context.getResources().getDimensionPixelSize(R.dimen.zen_schedule_day_margin);
        this.mLayout.setPadding(hPad, 0, hPad, 0);
        addView(this.mLayout);
        if (days != null) {
            for (int put : days) {
                this.mDays.put(put, true);
            }
        }
        this.mLayout.setOrientation(1);
        Calendar c = Calendar.getInstance();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (final int day : DAYS) {
            CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.zen_schedule_rule_day, this, false);
            c.set(7, day);
            checkBox.setText(this.mDayFormat.format(c.getTime()));
            checkBox.setChecked(this.mDays.get(day));
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ZenModeScheduleDaysSelection.this.mDays.put(day, isChecked);
                    ZenModeScheduleDaysSelection.this.onChanged(ZenModeScheduleDaysSelection.this.getDays());
                }
            });
            this.mLayout.addView(checkBox);
        }
    }

    private int[] getDays() {
        int i;
        SparseBooleanArray rt = new SparseBooleanArray(this.mDays.size());
        for (i = 0; i < this.mDays.size(); i++) {
            int day = this.mDays.keyAt(i);
            if (this.mDays.valueAt(i)) {
                rt.put(day, true);
            }
        }
        int[] rta = new int[rt.size()];
        for (i = 0; i < rta.length; i++) {
            rta[i] = rt.keyAt(i);
        }
        Arrays.sort(rta);
        return rta;
    }

    protected void onChanged(int[] days) {
    }
}
