package com.android.setupwizardlib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.setupwizardlib.view.Illustration;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupWizardLayout extends FrameLayout {
    private ViewGroup mContainer;

    protected static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new C06651();
        boolean isProgressBarShown = false;

        static class C06651 implements Creator<SavedState> {
            C06651() {
            }

            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        }

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public SavedState(Parcel source) {
            boolean z = false;
            super(source);
            if (source.readInt() != 0) {
                z = true;
            }
            this.isProgressBarShown = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.isProgressBarShown ? 1 : 0);
        }
    }

    public SetupWizardLayout(Context context) {
        super(context);
        init(0, null, R$attr.suwLayoutTheme);
    }

    public SetupWizardLayout(Context context, int template) {
        super(context);
        init(template, null, R$attr.suwLayoutTheme);
    }

    public SetupWizardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(0, attrs, R$attr.suwLayoutTheme);
    }

    @TargetApi(11)
    public SetupWizardLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(0, attrs, defStyleAttr);
    }

    @TargetApi(11)
    public SetupWizardLayout(Context context, int template, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(template, attrs, defStyleAttr);
    }

    private void init(int template, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R$styleable.SuwSetupWizardLayout, defStyleAttr, 0);
        if (template == 0) {
            template = a.getResourceId(R$styleable.SuwSetupWizardLayout_android_layout, 0);
        }
        inflateTemplate(template);
        Drawable background = a.getDrawable(R$styleable.SuwSetupWizardLayout_suwBackground);
        if (background != null) {
            setLayoutBackground(background);
        } else {
            Drawable backgroundTile = a.getDrawable(R$styleable.SuwSetupWizardLayout_suwBackgroundTile);
            if (backgroundTile != null) {
                setBackgroundTile(backgroundTile);
            }
        }
        Drawable illustration = a.getDrawable(R$styleable.SuwSetupWizardLayout_suwIllustration);
        if (illustration != null) {
            setIllustration(illustration);
        } else {
            Drawable illustrationImage = a.getDrawable(R$styleable.SuwSetupWizardLayout_suwIllustrationImage);
            Drawable horizontalTile = a.getDrawable(R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile);
            if (!(illustrationImage == null || horizontalTile == null)) {
                setIllustration(illustrationImage, horizontalTile);
            }
        }
        int decorPaddingTop = a.getDimensionPixelSize(R$styleable.SuwSetupWizardLayout_suwDecorPaddingTop, -1);
        if (decorPaddingTop == -1) {
            decorPaddingTop = getResources().getDimensionPixelSize(R$dimen.suw_decor_padding_top);
        }
        setDecorPaddingTop(decorPaddingTop);
        float illustrationAspectRatio = a.getFloat(R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio, -1.0f);
        if (illustrationAspectRatio == -1.0f) {
            TypedValue out = new TypedValue();
            getResources().getValue(R$dimen.suw_illustration_aspect_ratio, out, true);
            illustrationAspectRatio = out.getFloat();
        }
        setIllustrationAspectRatio(illustrationAspectRatio);
        CharSequence headerText = a.getText(R$styleable.SuwSetupWizardLayout_suwHeaderText);
        if (headerText != null) {
            setHeaderText(headerText);
        }
        a.recycle();
    }

    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.isProgressBarShown = isProgressBarShown();
        return ss;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.isProgressBarShown) {
            showProgressBar();
        } else {
            hideProgressBar();
        }
    }

    public void addView(View child, int index, LayoutParams params) {
        this.mContainer.addView(child, index, params);
    }

    private void addViewInternal(View child) {
        super.addView(child, -1, generateDefaultLayoutParams());
    }

    private void inflateTemplate(int templateResource) {
        addViewInternal(onInflateTemplate(LayoutInflater.from(getContext()), templateResource));
        this.mContainer = (ViewGroup) findViewById(getContainerId());
        onTemplateInflated();
    }

    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (template == 0) {
            template = R$layout.suw_template;
        }
        return inflater.inflate(template, this, false);
    }

    protected void onTemplateInflated() {
    }

    protected int getContainerId() {
        return R$id.suw_layout_content;
    }

    public NavigationBar getNavigationBar() {
        View view = findViewById(R$id.suw_layout_navigation_bar);
        return view instanceof NavigationBar ? (NavigationBar) view : null;
    }

    public void setHeaderText(CharSequence title) {
        TextView titleView = (TextView) findViewById(R$id.suw_layout_title);
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    public void setIllustration(Drawable drawable) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view instanceof Illustration) {
            ((Illustration) view).setIllustration(drawable);
        }
    }

    public void setIllustration(int asset, int horizontalTile) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view instanceof Illustration) {
            ((Illustration) view).setIllustration(getIllustration(asset, horizontalTile));
        }
    }

    private void setIllustration(Drawable asset, Drawable horizontalTile) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view instanceof Illustration) {
            ((Illustration) view).setIllustration(getIllustration(asset, horizontalTile));
        }
    }

    public void setIllustrationAspectRatio(float aspectRatio) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view instanceof Illustration) {
            ((Illustration) view).setAspectRatio(aspectRatio);
        }
    }

    public void setDecorPaddingTop(int paddingTop) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view != null) {
            view.setPadding(view.getPaddingLeft(), paddingTop, view.getPaddingRight(), view.getPaddingBottom());
        }
    }

    public void setLayoutBackground(Drawable background) {
        View view = findViewById(R$id.suw_layout_decor);
        if (view != null) {
            view.setBackgroundDrawable(background);
        }
    }

    private void setBackgroundTile(Drawable backgroundTile) {
        if (backgroundTile instanceof BitmapDrawable) {
            ((BitmapDrawable) backgroundTile).setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
        }
        setLayoutBackground(backgroundTile);
    }

    private Drawable getIllustration(int asset, int horizontalTile) {
        Context context = getContext();
        return getIllustration(context.getResources().getDrawable(asset), context.getResources().getDrawable(horizontalTile));
    }

    @SuppressLint({"RtlHardcoded"})
    private Drawable getIllustration(Drawable asset, Drawable horizontalTile) {
        if (getContext().getResources().getBoolean(R$bool.suwUseTabletLayout)) {
            if (horizontalTile instanceof BitmapDrawable) {
                ((BitmapDrawable) horizontalTile).setTileModeX(TileMode.REPEAT);
                ((BitmapDrawable) horizontalTile).setGravity(48);
            }
            if (asset instanceof BitmapDrawable) {
                ((BitmapDrawable) asset).setGravity(51);
            }
            LayerDrawable layers = new LayerDrawable(new Drawable[]{horizontalTile, asset});
            if (VERSION.SDK_INT >= 19) {
                layers.setAutoMirrored(true);
            }
            return layers;
        }
        if (VERSION.SDK_INT >= 19) {
            asset.setAutoMirrored(true);
        }
        return asset;
    }

    public boolean isProgressBarShown() {
        View progressBar = findViewById(R$id.suw_layout_progress);
        if (progressBar == null || progressBar.getVisibility() != 0) {
            return false;
        }
        return true;
    }

    public void showProgressBar() {
        View progressBar = findViewById(R$id.suw_layout_progress);
        if (progressBar != null) {
            progressBar.setVisibility(0);
            return;
        }
        ViewStub progressBarStub = (ViewStub) findViewById(R$id.suw_layout_progress_stub);
        if (progressBarStub != null) {
            progressBarStub.inflate();
        }
    }

    public void hideProgressBar() {
        View progressBar = findViewById(R$id.suw_layout_progress);
        if (progressBar != null) {
            progressBar.setVisibility(8);
        }
    }
}
