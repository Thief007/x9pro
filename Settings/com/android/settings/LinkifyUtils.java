package com.android.settings;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class LinkifyUtils {

    public interface OnClickListener {
        void onClick();
    }

    private LinkifyUtils() {
    }

    public static boolean linkify(TextView textView, StringBuilder text, final OnClickListener listener) {
        int beginIndex = text.indexOf("LINK_BEGIN");
        if (beginIndex == -1) {
            textView.setText(text);
            return false;
        }
        text.delete(beginIndex, "LINK_BEGIN".length() + beginIndex);
        int endIndex = text.indexOf("LINK_END");
        if (endIndex == -1) {
            textView.setText(text);
            return false;
        }
        text.delete(endIndex, "LINK_END".length() + endIndex);
        textView.setText(text.toString(), BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        ((Spannable) textView.getText()).setSpan(new ClickableSpan() {
            public void onClick(View widget) {
                listener.onClick();
            }

            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, beginIndex, endIndex, 33);
        return true;
    }
}
