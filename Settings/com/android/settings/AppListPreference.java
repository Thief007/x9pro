package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAppListExt;
import java.util.ArrayList;
import java.util.List;

public class AppListPreference extends ListPreference {
    private Drawable[] mEntryDrawables;
    IAppListExt mExt;
    private boolean mShowItemNone = false;

    public class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        private Drawable[] mImageDrawables = null;
        private int mSelectedIndex = 0;

        public AppArrayAdapter(Context context, int textViewResourceId, CharSequence[] objects, Drawable[] imageDrawables, int selectedIndex) {
            super(context, textViewResourceId, objects);
            this.mSelectedIndex = selectedIndex;
            this.mImageDrawables = imageDrawables;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(R.id.app_label);
            textView.setText((CharSequence) getItem(position));
            if (position == this.mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(0);
            }
            ((ImageView) view.findViewById(R.id.app_image)).setImageDrawable(this.mImageDrawables[position]);
            if (AppListPreference.this.mExt == null) {
                AppListPreference.this.mExt = UtilsExt.getAppListPlugin(getContext());
            }
            return AppListPreference.this.mExt.addLayoutAppView(view, textView, (TextView) view.findViewById(R.id.default_label), position, this.mImageDrawables[position], parent);
        }
    }

    private static class SavedState implements Parcelable {
        public static Creator<SavedState> CREATOR = new C00471();
        public final CharSequence[] entryValues;
        public final boolean showItemNone;
        public final Parcelable superState;
        public final CharSequence value;

        static class C00471 implements Creator<SavedState> {
            C00471() {
            }

            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source.readCharSequenceArray(), source.readCharSequence(), source.readInt() != 0, source.readParcelable(getClass().getClassLoader()));
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        }

        public SavedState(CharSequence[] entryValues, CharSequence value, boolean showItemNone, Parcelable superState) {
            this.entryValues = entryValues;
            this.value = value;
            this.showItemNone = showItemNone;
            this.superState = superState;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeCharSequenceArray(this.entryValues);
            dest.writeCharSequence(this.value);
            dest.writeInt(this.showItemNone ? 1 : 0);
            dest.writeParcelable(this.superState, flags);
        }
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setShowItemNone(boolean showItemNone) {
        this.mShowItemNone = showItemNone;
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        int i = 0;
        PackageManager pm = getContext().getPackageManager();
        int length = packageNames.length;
        if (this.mShowItemNone) {
            i = 1;
        }
        int entryCount = length + i;
        List<CharSequence> applicationNames = new ArrayList(entryCount);
        List<CharSequence> validatedPackageNames = new ArrayList(entryCount);
        List<Drawable> entryDrawables = new ArrayList(entryCount);
        int selectedIndex = -1;
        for (int i2 = 0; i2 < packageNames.length; i2++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageNames[i2].toString(), 0);
                if (this.mExt == null) {
                    this.mExt = UtilsExt.getAppListPlugin(getContext());
                }
                this.mExt.setAppListItem(appInfo.packageName, i2);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedPackageNames.add(appInfo.packageName);
                entryDrawables.add(appInfo.loadIcon(pm));
                if (defaultPackageName != null && appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = i2;
                }
            } catch (NameNotFoundException e) {
            }
        }
        if (this.mShowItemNone) {
            applicationNames.add(getContext().getResources().getText(R.string.app_list_preference_none));
            validatedPackageNames.add("");
            entryDrawables.add(getContext().getDrawable(R.drawable.ic_remove_circle));
        }
        setEntries((CharSequence[]) applicationNames.toArray(new CharSequence[applicationNames.size()]));
        setEntryValues((CharSequence[]) validatedPackageNames.toArray(new CharSequence[validatedPackageNames.size()]));
        this.mEntryDrawables = (Drawable[]) entryDrawables.toArray(new Drawable[entryDrawables.size()]);
        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    protected ListAdapter createListAdapter() {
        String selectedValue = getValue();
        boolean contentEquals = selectedValue != null ? this.mShowItemNone ? selectedValue.contentEquals("") : false : true;
        return new AppArrayAdapter(getContext(), R.layout.app_preference_item, getEntries(), this.mEntryDrawables, contentEquals ? -1 : findIndexOfValue(selectedValue));
    }

    protected void onPrepareDialogBuilder(Builder builder) {
        builder.setAdapter(createListAdapter(), this);
        super.onPrepareDialogBuilder(builder);
    }

    protected Parcelable onSaveInstanceState() {
        return new SavedState(getEntryValues(), getValue(), this.mShowItemNone, super.onSaveInstanceState());
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            this.mShowItemNone = savedState.showItemNone;
            setPackageNames(savedState.entryValues, savedState.value);
            super.onRestoreInstanceState(savedState.superState);
            return;
        }
        super.onRestoreInstanceState(state);
    }
}
