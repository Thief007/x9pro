package com.mediatek.systemui.floatpanel;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.mediatek.systemui.floatpanel.DragSortGridView.OnReorderingListener;

public class FloatPanelView extends LinearLayout {
    private BaseStatusBar mBar;
    private Context mContext;
    private boolean mDetached;
    private FloatAppAdapter mExtentAdapter;
    private LinearLayout mExtentContainer;
    private final OnItemClickListener mExtentItemClickListener;
    private DragSortGridView mExtentView;
    private OnReorderingListener mFloatDragSortListener;
    private FloatModel mFloatModel;
    private Handler mHandler;
    private boolean mInExtensionMode;
    private final BroadcastReceiver mReceiver;
    private FloatAppAdapter mResidentAdapter;
    private final OnItemClickListener mResidentItemClickListener;
    private CustomizedHorizontalScrollView mResidentScrollView;
    private DragSortGridView mResidentView;
    private OnReorderingListener mUnFloatDragSortListener;

    public FloatPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    Log.d("FloatPanelView", "BroadcastReceiver screen off and toggleFloatPanel.");
                    FloatPanelView.this.mBar.toggleFloatPanelScreenOff();
                }
            }
        };
        this.mFloatDragSortListener = new OnReorderingListener() {
            public void onReordering(int fromPosition, int toPosition) {
                Log.w("FloatPanelView", "onReordering gridView fromPosition:" + fromPosition + ",toPosition:" + toPosition);
                FloatPanelView.this.mResidentAdapter.reorder(fromPosition, toPosition);
                FloatPanelView.this.mFloatModel.commitModify();
            }

            public void onItemSwitched(int switchedPosition) {
                Log.d("FloatPanelView", "onItemSwitched: switchedPosition = " + switchedPosition);
                FloatPanelView.this.mExtentAdapter.removeItem(switchedPosition);
            }
        };
        this.mUnFloatDragSortListener = new OnReorderingListener() {
            public void onReordering(int fromPosition, int toPosition) {
                Log.d("FloatPanelView", "onReordering gridViewHorizontal fromPosition:" + fromPosition + ",toPosition:" + toPosition);
                FloatPanelView.this.mExtentAdapter.reorder(fromPosition, toPosition);
                FloatPanelView.this.mFloatModel.commitModify();
            }

            public void onItemSwitched(int switchedPosition) {
                Log.d("FloatPanelView", "onItemSwitched gridViewHorizontal: switchedPosition = " + switchedPosition);
                FloatPanelView.this.mResidentAdapter.removeItem(switchedPosition);
            }
        };
        this.mResidentItemClickListener = new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                FloatPanelView.this.startFloatActivity(FloatPanelView.this.mResidentAdapter, position);
            }
        };
        this.mExtentItemClickListener = new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                FloatPanelView.this.startFloatActivity(FloatPanelView.this.mExtentAdapter, position);
            }
        };
        this.mContext = context;
        this.mFloatModel = new FloatModel(this);
        HandlerThread completeThread = new HandlerThread("float complete commit");
        completeThread.start();
        this.mHandler = new Handler(completeThread.getLooper());
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDetached = false;
        Log.d("FloatPanelView", "onFinishInflate: this = " + this);
        this.mExtentContainer = (LinearLayout) findViewById(R.id.extent_panel);
        this.mExtentContainer.setVisibility(4);
        this.mResidentScrollView = (CustomizedHorizontalScrollView) findViewById(R.id.resident_container);
        this.mResidentView = (DragSortGridView) findViewById(R.id.resident_grid);
        this.mExtentView = (DragSortGridView) findViewById(R.id.extent_grid);
        this.mResidentAdapter = new FloatAppAdapter(this.mContext, this.mFloatModel, 1);
        this.mResidentView.setEnableStrechMode(true);
        this.mResidentView.setEnableVerticalDragScroll(true);
        this.mResidentView.setAdapter(this.mResidentAdapter);
        this.mResidentView.setColumnWidth(this.mContext.getResources().getDimensionPixelSize(R.dimen.gridview_column_width));
        this.mResidentView.setOnReorderingListener(this.mFloatDragSortListener);
        this.mResidentView.setOnItemClickListener(this.mResidentItemClickListener);
        this.mExtentAdapter = new FloatAppAdapter(this.mContext, this.mFloatModel, 2);
        this.mExtentView.setAdapter(this.mExtentAdapter);
        this.mExtentView.setOnReorderingListener(this.mUnFloatDragSortListener);
        this.mExtentView.setOnItemClickListener(this.mExtentItemClickListener);
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        Log.d("FloatPanelView", "onConfigurationChanged : " + newConfig.orientation + ", mFloatedView childcount = " + this.mResidentView.getChildCount() + ", adapter count = " + this.mResidentAdapter.getCount() + ", mFloatedView = " + this.mResidentView + ", vis = " + this.mResidentView.getVisibility() + ", mFloatedView parent = " + this.mResidentView.getParent());
        this.mResidentView.setColumnWidth(this.mContext.getResources().getDimensionPixelSize(R.dimen.gridview_column_width));
        this.mExtentView.setColumnWidth(this.mContext.getResources().getDimensionPixelSize(R.dimen.gridview_column_width));
        this.mExtentView.setVerticalSpacing(this.mContext.getResources().getDimensionPixelSize(R.dimen.gridview_vertical_spacing));
        this.mBar.updateFloatButtonIcon(!isShown());
        if (this.mInExtensionMode) {
            this.mBar.setExtensionButtonVisibility(4);
        }
        super.onConfigurationChanged(newConfig);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("FloatPanelView", "onDetachedFromWindow........");
        this.mDetached = true;
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public void setBar(BaseStatusBar bar) {
        this.mBar = bar;
    }

    public void enterExtensionMode() {
        this.mInExtensionMode = true;
        this.mExtentContainer.setVisibility(0);
        this.mBar.setExtensionButtonVisibility(4);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        if (action == 0) {
            this.mBar.cancelCloseFloatPanel();
        } else if (action == 1) {
            this.mBar.postCloseFloatPanel();
        }
        if (event.getKeyCode() != 4) {
            return super.dispatchKeyEvent(event);
        }
        if (action == 0) {
            Log.d("FloatPanelView", "Back key down, toggleFloatPanel...");
            this.mBar.toggleFloatPanel();
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            this.mBar.cancelCloseFloatPanel();
        } else if (action == 1 || action == 3) {
            this.mBar.postCloseFloatPanel();
        }
        if (this.mDetached) {
            return super.dispatchTouchEvent(event);
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == 0 && (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())) {
            this.mBar.changgeFloatPanelFocus(false);
            return true;
        } else if (event.getAction() == 4) {
            this.mBar.changgeFloatPanelFocus(false);
            return true;
        } else {
            this.mBar.changgeFloatPanelFocus(true);
            return super.dispatchTouchEvent(event);
        }
    }

    public boolean dispatchDragEvent(DragEvent event) {
        int action = event.getAction();
        if (action == 1) {
            this.mBar.cancelCloseFloatPanel();
        } else if (action == 4) {
            this.mBar.postCloseFloatPanel();
        }
        return super.dispatchDragEvent(event);
    }

    public void refreshUI() {
        if (this.mResidentAdapter != null) {
            this.mResidentAdapter.notifyDataSetChanged();
        }
        if (this.mExtentAdapter != null) {
            this.mExtentAdapter.notifyDataSetChanged();
        }
    }

    private void startFloatActivity(FloatAppAdapter adapter, int position) {
        try {
            Intent intent = adapter.intentForPosition(position);
            if (intent != null) {
                String packageName = intent.getPackage();
                String className = intent.getComponent().getClassName();
                intent.setFlags(270533120);
                Log.d("FloatPanelView", "onItemClick: position = " + position + ",packageName = " + packageName + ", className = " + className);
                Log.d("FloatPanelView", "onItemClick: intent = " + intent);
                intent.setPackage(null);
                this.mContext.startActivity(intent);
                this.mBar.closeFloatPanel();
            }
        } catch (ActivityNotFoundException e) {
            Log.d("FloatPanelView", "startFloatActivity,ActivityNotFoundException");
        }
    }
}
