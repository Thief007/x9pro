package com.mediatek.beam;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.beam.BeamShareTask.Direction;
import java.util.Date;

public class BeamShareTabAdapter extends ResourceCursorAdapter {
    public BeamShareTabAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        CharSequence modifiedDate;
        int i = R.drawable.ic_beamplus_list_failed;
        BeamShareTask task = new BeamShareTask(cursor);
        ImageView icon = (ImageView) view.findViewById(R.id.transfer_icon);
        if (task.getDirection() == Direction.in) {
            if (task.getState() == 1) {
                i = R.drawable.ic_beamplus_list_receiver;
            }
            icon.setImageResource(i);
        } else {
            if (task.getState() == 1) {
                i = R.drawable.ic_beamplus_list_sender;
            }
            icon.setImageResource(i);
        }
        TextView textView = (TextView) view.findViewById(R.id.transfer_file);
        String filename = task.getData();
        if (filename == null) {
            filename = "";
        }
        textView.setText(filename);
        textView = (TextView) view.findViewById(R.id.modified_date);
        Date d = new Date(task.getModifiedDate());
        if (DateUtils.isToday(task.getModifiedDate())) {
            modifiedDate = DateFormat.getTimeFormat(context).format(d);
        } else {
            modifiedDate = DateFormat.getDateFormat(context).format(d);
        }
        textView.setText(modifiedDate);
        ((TextView) view.findViewById(R.id.transfer_info)).setText(Formatter.formatFileSize(context, task.getTotalBytes()));
    }
}
