package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

public class SetupWizardListLayout extends SetupWizardLayout {
    private ListView mListView;

    public SetupWizardListLayout(Context context) {
        super(context);
    }

    public SetupWizardListLayout(Context context, int template) {
        super(context, template);
    }

    public SetupWizardListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(11)
    public SetupWizardListLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(11)
    public SetupWizardListLayout(Context context, int template, AttributeSet attrs, int defStyleAttr) {
        super(context, template, attrs, defStyleAttr);
    }

    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (template == 0) {
            template = R$layout.suw_list_template;
        }
        return inflater.inflate(template, this, false);
    }

    protected void onTemplateInflated() {
        this.mListView = (ListView) findViewById(16908298);
    }

    protected int getContainerId() {
        return 16908298;
    }

    public ListView getListView() {
        return this.mListView;
    }
}
