package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.android.settingslib.datetime.ZoneGetter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ZonePicker extends ListFragment {
    private SimpleAdapter mAlphabeticalAdapter;
    private ZoneSelectionListener mListener;
    private boolean mSortedByTimezone;
    private SimpleAdapter mTimezoneSortedAdapter;

    private static class MyComparator implements Comparator<Map<?, ?>> {
        private String mSortingKey;

        public MyComparator(String sortingKey) {
            this.mSortingKey = sortingKey;
        }

        public int compare(Map<?, ?> map1, Map<?, ?> map2) {
            Object value1 = map1.get(this.mSortingKey);
            Object value2 = map2.get(this.mSortingKey);
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (isComparable(value2)) {
                return ((Comparable) value1).compareTo(value2);
            } else {
                return -1;
            }
        }

        private boolean isComparable(Object value) {
            return value != null ? value instanceof Comparable : false;
        }
    }

    public interface ZoneSelectionListener {
        void onZoneSelected(TimeZone timeZone);
    }

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean sortedByName) {
        return constructTimezoneAdapter(context, sortedByName, R.layout.date_time_setup_custom_list_item_2);
    }

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean sortedByName, int layoutId) {
        String[] from = new String[]{"name", "gmt"};
        int[] to = new int[]{16908308, 16908309};
        MyComparator comparator = new MyComparator(sortedByName ? "name" : "offset");
        List<Map<String, Object>> sortedList = ZoneGetter.getZonesList(context);
        Collections.sort(sortedList, comparator);
        return new SimpleAdapter(context, sortedList, layoutId, from, to);
    }

    public static int getTimeZoneIndex(SimpleAdapter adapter, TimeZone tz) {
        String defaultId = tz.getID();
        int listSize = adapter.getCount();
        for (int i = 0; i < listSize; i++) {
            if (defaultId.equals((String) ((HashMap) adapter.getItem(i)).get("id"))) {
                return i;
            }
        }
        return -1;
    }

    public static TimeZone obtainTimeZoneFromItem(Object item) {
        return TimeZone.getTimeZone((String) ((Map) item).get("id"));
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        this.mTimezoneSortedAdapter = constructTimezoneAdapter(activity, false);
        this.mAlphabeticalAdapter = constructTimezoneAdapter(activity, true);
        setSorting(true);
        setHasOptionsMenu(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Utils.forcePrepareCustomPreferencesList(container, view, (ListView) view.findViewById(16908298), false);
        return view;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.zone_list_menu_sort_alphabetically).setIcon(17301660);
        menu.add(0, 2, 0, R.string.zone_list_menu_sort_by_timezone).setIcon(R.drawable.ic_menu_3d_globe);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (this.mSortedByTimezone) {
            menu.findItem(2).setVisible(false);
            menu.findItem(1).setVisible(true);
            return;
        }
        menu.findItem(2).setVisible(true);
        menu.findItem(1).setVisible(false);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                setSorting(false);
                return true;
            case 2:
                setSorting(true);
                return true;
            default:
                return false;
        }
    }

    private void setSorting(boolean sortByTimezone) {
        SimpleAdapter adapter = sortByTimezone ? this.mTimezoneSortedAdapter : this.mAlphabeticalAdapter;
        setListAdapter(adapter);
        this.mSortedByTimezone = sortByTimezone;
        int defaultIndex = getTimeZoneIndex(adapter, TimeZone.getDefault());
        if (defaultIndex >= 0) {
            setSelection(defaultIndex);
        }
    }

    public void onListItemClick(ListView listView, View v, int position, long id) {
        if (isResumed()) {
            String tzId = (String) ((Map) listView.getItemAtPosition(position)).get("id");
            ((AlarmManager) getActivity().getSystemService("alarm")).setTimeZone(tzId);
            TimeZone tz = TimeZone.getTimeZone(tzId);
            if (this.mListener != null) {
                this.mListener.onZoneSelected(tz);
            } else {
                getActivity().onBackPressed();
            }
        }
    }
}
