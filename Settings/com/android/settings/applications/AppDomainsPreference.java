package com.android.settings.applications;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference;

public class AppDomainsPreference extends ListDialogPreference {
    private int mNumEntries;

    public AppDomainsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.app_domains_dialog);
        setListItemLayoutResource(R.layout.app_domains_item);
    }

    public void setTitles(CharSequence[] titles) {
        this.mNumEntries = titles != null ? titles.length : 0;
        super.setTitles(titles);
    }

    public CharSequence getSummary() {
        Context context = getContext();
        if (this.mNumEntries == 0) {
            return context.getString(R.string.domain_urls_summary_none);
        }
        int whichVersion;
        CharSequence summary = super.getSummary();
        if (this.mNumEntries == 1) {
            whichVersion = R.string.domain_urls_summary_one;
        } else {
            whichVersion = R.string.domain_urls_summary_some;
        }
        return context.getString(whichVersion, new Object[]{summary});
    }

    protected void onBindListItem(View view, int index) {
        CharSequence title = getTitleAt(index);
        if (title != null) {
            ((TextView) view.findViewById(R.id.domain_name)).setText(title);
        }
    }
}
