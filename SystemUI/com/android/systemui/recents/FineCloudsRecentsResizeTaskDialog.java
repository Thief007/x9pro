package com.android.systemui.recents;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.RecentsView;

public class FineCloudsRecentsResizeTaskDialog extends DialogFragment {
    private static final int[][] BUTTON_DEFINITIONS = new int[][]{new int[]{R.id.place_left, 1}, new int[]{R.id.place_right, 2}, new int[]{R.id.place_top, 3}, new int[]{R.id.place_bottom, 4}, new int[]{R.id.place_top_left, 5}, new int[]{R.id.place_top_right, 6}, new int[]{R.id.place_bottom_left, 7}, new int[]{R.id.place_bottom_right, 8}, new int[]{R.id.place_full, 9}};
    private Rect[] mBounds = new Rect[]{new Rect(), new Rect(), new Rect(), new Rect()};
    private FragmentManager mFragmentManager;
    private FineCloudsRecentsActivity mRecentsActivity;
    private RecentsView mRecentsView;
    private View mResizeTaskDialogContent;
    private SystemServicesProxy mSsp;
    private Task[] mTasks = new Task[]{null, null, null, null};

    public FineCloudsRecentsResizeTaskDialog(FragmentManager mgr, FineCloudsRecentsActivity activity) {
        this.mFragmentManager = mgr;
        this.mRecentsActivity = activity;
        this.mSsp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
    }

    void showResizeTaskDialog(Task mainTask, RecentsView rv) {
        this.mTasks[0] = mainTask;
        this.mRecentsView = rv;
        show(this.mFragmentManager, "FineCloudsRecentsResizeTaskDialog");
    }

    private void createResizeTaskDialog(Context context, LayoutInflater inflater, Builder builder) {
        builder.setTitle(R.string.recents_caption_resize);
        this.mResizeTaskDialogContent = inflater.inflate(R.layout.recents_task_resize_dialog, null, false);
        for (int i = 0; i < BUTTON_DEFINITIONS.length; i++) {
            Button b = (Button) this.mResizeTaskDialogContent.findViewById(BUTTON_DEFINITIONS[i][0]);
            if (b != null) {
                final int action = BUTTON_DEFINITIONS[i][1];
                b.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        FineCloudsRecentsResizeTaskDialog.this.placeTasks(action);
                    }
                });
            }
        }
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                FineCloudsRecentsResizeTaskDialog.this.dismiss();
            }
        });
        builder.setView(this.mResizeTaskDialogContent);
    }

    private void placeTasks(int arrangement) {
        int i;
        Rect rect = this.mSsp.getWindowRect();
        for (i = 0; i < this.mBounds.length; i++) {
            this.mBounds[i].set(rect);
            if (i != 0) {
                this.mTasks[i] = null;
            }
        }
        int additionalTasks = 0;
        switch (arrangement) {
            case 1:
                this.mBounds[0].right = this.mBounds[0].centerX();
                this.mBounds[1].left = this.mBounds[0].right;
                additionalTasks = 1;
                break;
            case 2:
                this.mBounds[1].right = this.mBounds[1].centerX();
                this.mBounds[0].left = this.mBounds[1].right;
                additionalTasks = 1;
                break;
            case 3:
                this.mBounds[0].bottom = this.mBounds[0].centerY();
                this.mBounds[1].top = this.mBounds[0].bottom;
                additionalTasks = 1;
                break;
            case 4:
                this.mBounds[1].bottom = this.mBounds[1].centerY();
                this.mBounds[0].top = this.mBounds[1].bottom;
                additionalTasks = 1;
                break;
            case 5:
                this.mBounds[0].right = this.mBounds[0].centerX();
                this.mBounds[0].bottom = this.mBounds[0].centerY();
                this.mBounds[1].left = this.mBounds[0].right;
                this.mBounds[1].bottom = this.mBounds[0].bottom;
                this.mBounds[2].right = this.mBounds[0].right;
                this.mBounds[2].top = this.mBounds[0].bottom;
                this.mBounds[3].left = this.mBounds[0].right;
                this.mBounds[3].top = this.mBounds[0].bottom;
                additionalTasks = 3;
                break;
            case 6:
                this.mBounds[0].left = this.mBounds[0].centerX();
                this.mBounds[0].bottom = this.mBounds[0].centerY();
                this.mBounds[1].right = this.mBounds[0].left;
                this.mBounds[1].bottom = this.mBounds[0].bottom;
                this.mBounds[2].left = this.mBounds[0].left;
                this.mBounds[2].top = this.mBounds[0].bottom;
                this.mBounds[3].right = this.mBounds[0].left;
                this.mBounds[3].top = this.mBounds[0].bottom;
                additionalTasks = 3;
                break;
            case 7:
                this.mBounds[0].right = this.mBounds[0].centerX();
                this.mBounds[0].top = this.mBounds[0].centerY();
                this.mBounds[1].left = this.mBounds[0].right;
                this.mBounds[1].top = this.mBounds[0].top;
                this.mBounds[2].right = this.mBounds[0].right;
                this.mBounds[2].bottom = this.mBounds[0].top;
                this.mBounds[3].left = this.mBounds[0].right;
                this.mBounds[3].bottom = this.mBounds[0].top;
                additionalTasks = 3;
                break;
            case 8:
                this.mBounds[0].left = this.mBounds[0].centerX();
                this.mBounds[0].top = this.mBounds[0].centerY();
                this.mBounds[1].right = this.mBounds[0].left;
                this.mBounds[1].top = this.mBounds[0].top;
                this.mBounds[2].left = this.mBounds[0].left;
                this.mBounds[2].bottom = this.mBounds[0].top;
                this.mBounds[3].right = this.mBounds[0].left;
                this.mBounds[3].bottom = this.mBounds[0].top;
                additionalTasks = 3;
                break;
        }
        i = 1;
        while (i <= additionalTasks && this.mTasks[i - 1] != null) {
            this.mTasks[i] = this.mRecentsView.getNextTaskOrTopTask(this.mTasks[i - 1]);
            if (this.mTasks[i] == this.mTasks[0]) {
                this.mTasks[i] = null;
            }
            i++;
        }
        dismiss();
        this.mRecentsActivity.dismissRecentsToHomeWithoutTransitionAnimation();
        for (i = additionalTasks; i >= 0; i--) {
            if (this.mTasks[i] != null) {
                this.mSsp.resizeTask(this.mTasks[i].key.id, this.mBounds[i]);
            }
        }
        for (i = additionalTasks; i >= 0; i--) {
            if (this.mTasks[i] != null) {
                this.mRecentsView.launchTask(this.mTasks[i]);
            }
        }
    }

    public Dialog onCreateDialog(Bundle args) {
        Context context = getActivity();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        Builder builder = new Builder(getActivity());
        createResizeTaskDialog(context, inflater, builder);
        return builder.create();
    }
}
