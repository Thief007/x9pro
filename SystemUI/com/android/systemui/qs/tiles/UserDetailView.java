package com.android.systemui.qs.tiles;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.PseudoGridView;
import com.android.systemui.qs.PseudoGridView.ViewGroupAdapterBridge;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.UserSwitcherController.BaseUserAdapter;
import com.android.systemui.statusbar.policy.UserSwitcherController.UserRecord;

public class UserDetailView extends PseudoGridView {
    private Adapter mAdapter;

    public static class Adapter extends BaseUserAdapter implements OnClickListener {
        private Context mContext;

        public Adapter(Context context, UserSwitcherController controller) {
            super(controller);
            this.mContext = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            UserRecord item = getItem(position);
            View v = UserDetailItemView.convertOrInflate(this.mContext, convertView, parent);
            if (v != convertView) {
                v.setOnClickListener(this);
            }
            String name = getName(this.mContext, item);
            if (item.picture == null) {
                v.bind(name, getDrawable(this.mContext, item));
            } else {
                v.bind(name, item.picture);
            }
            v.setActivated(item.isCurrent);
            v.setTag(item);
            return v;
        }

        public void onClick(View view) {
            UserRecord tag = (UserRecord) view.getTag();
            MetricsLogger.action(this.mContext, 156);
            switchTo(tag);
        }
    }

    public UserDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static UserDetailView inflate(Context context, ViewGroup parent, boolean attach) {
        return (UserDetailView) LayoutInflater.from(context).inflate(R.layout.qs_user_detail, parent, attach);
    }

    public void createAndSetAdapter(UserSwitcherController controller) {
        this.mAdapter = new Adapter(this.mContext, controller);
        ViewGroupAdapterBridge.link(this, this.mAdapter);
    }

    public void refreshAdapter() {
        this.mAdapter.refresh();
    }
}
