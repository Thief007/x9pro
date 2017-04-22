package com.android.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;

public class DropDownPreference extends Preference {
    private final ArrayAdapter<String> mAdapter;
    private Callback mCallback;
    private final Context mContext;
    private int mSelectedPosition;
    private final Spinner mSpinner;
    private final ArrayList<Object> mValues;

    public interface Callback {
        boolean onItemSelected(int i, Object obj);
    }

    class C01341 implements OnItemSelectedListener {
        C01341() {
        }

        public void onItemSelected(AdapterView<?> adapterView, View v, int position, long id) {
            DropDownPreference.this.setSelectedItem(position, true);
        }

        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    class C01352 implements OnPreferenceClickListener {
        C01352() {
        }

        public boolean onPreferenceClick(Preference preference) {
            DropDownPreference.this.mSpinner.performClick();
            return true;
        }
    }

    public DropDownPreference(Context context) {
        this(context, null);
    }

    public DropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mValues = new ArrayList();
        this.mSelectedPosition = -1;
        this.mContext = context;
        this.mAdapter = new ArrayAdapter(this.mContext, 17367049);
        this.mSpinner = new Spinner(this.mContext);
        this.mSpinner.setVisibility(4);
        this.mSpinner.setAdapter(this.mAdapter);
        this.mSpinner.setOnItemSelectedListener(new C01341());
        setPersistent(false);
        setOnPreferenceClickListener(new C01352());
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.DropDownPreference);
        CharSequence[] entries = a.getTextArray(0);
        CharSequence[] values = a.getTextArray(1);
        if (entries != null && values != null) {
            for (int i = 0; i < entries.length; i++) {
                addItem(entries[i].toString(), values[i]);
            }
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setSelectedItem(int position) {
        setSelectedItem(position, false);
    }

    public void setSelectedItem(int position, boolean fromSpinner) {
        if (!fromSpinner || position != this.mSelectedPosition) {
            Object value = this.mValues.get(position);
            if (this.mCallback == null || this.mCallback.onItemSelected(position, value)) {
                this.mSpinner.setSelection(position);
                this.mSelectedPosition = this.mSpinner.getSelectedItemPosition();
                setSummary((CharSequence) this.mAdapter.getItem(position));
                notifyDependencyChange(value == null);
            }
        }
    }

    public void setSelectedValue(Object value) {
        int i = this.mValues.indexOf(value);
        if (i > -1) {
            setSelectedItem(i);
        }
    }

    public void addItem(int captionResid, Object value) {
        addItem(this.mContext.getResources().getString(captionResid), value);
    }

    public void addItem(String caption, Object value) {
        this.mAdapter.add(caption);
        this.mValues.add(value);
    }

    public int getItemCount() {
        return this.mAdapter.getCount();
    }

    public void clearItems() {
        this.mAdapter.clear();
        this.mValues.clear();
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (!view.equals(this.mSpinner.getParent())) {
            if (this.mSpinner.getParent() != null) {
                ((ViewGroup) this.mSpinner.getParent()).removeView(this.mSpinner);
            }
            ((ViewGroup) view).addView(this.mSpinner, 0);
            LayoutParams lp = this.mSpinner.getLayoutParams();
            lp.width = 0;
            this.mSpinner.setLayoutParams(lp);
        }
    }
}
