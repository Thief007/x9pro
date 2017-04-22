package com.android.settings.applications;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.Utils;

public class RunningServices extends Fragment {
    private View mLoadingContainer;
    private Menu mOptionsMenu;
    private final Runnable mRunningProcessesAvail = new C02831();
    private RunningProcessesView mRunningProcessesView;

    class C02831 implements Runnable {
        C02831() {
        }

        public void run() {
            Utils.handleLoadingContainer(RunningServices.this.mLoadingContainer, RunningServices.this.mRunningProcessesView, true, true);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.manage_applications_running, null);
        this.mRunningProcessesView = (RunningProcessesView) rootView.findViewById(R.id.running_processes);
        this.mRunningProcessesView.doCreate();
        this.mLoadingContainer = rootView.findViewById(R.id.loading_container);
        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mOptionsMenu = menu;
        menu.add(0, 1, 1, R.string.show_running_services).setShowAsAction(1);
        menu.add(0, 2, 2, R.string.show_background_processes).setShowAsAction(1);
        updateOptionsMenu();
    }

    public void onResume() {
        super.onResume();
        Utils.handleLoadingContainer(this.mLoadingContainer, this.mRunningProcessesView, this.mRunningProcessesView.doResume(this, this.mRunningProcessesAvail), false);
    }

    public void onPause() {
        super.onPause();
        this.mRunningProcessesView.doPause();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                this.mRunningProcessesView.mAdapter.setShowBackground(false);
                break;
            case 2:
                this.mRunningProcessesView.mAdapter.setShowBackground(true);
                break;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        boolean z = true;
        boolean showingBackground = this.mRunningProcessesView.mAdapter.getShowBackground();
        this.mOptionsMenu.findItem(1).setVisible(showingBackground);
        MenuItem findItem = this.mOptionsMenu.findItem(2);
        if (showingBackground) {
            z = false;
        }
        findItem.setVisible(z);
    }
}
