package com.adnaan525.medme.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * With targetSdk 35, Android draws content edge-to-edge behind the system bars by default -
 * without this, toolbars and status-bar-adjacent content sit underneath the status bar.
 */
public final class InsetsUtils {
    private InsetsUtils() {
    }

    /** Pads the view's top by the status bar height, preserving its other padding. */
    public static void applyTopInset(View view) {
        int left = view.getPaddingLeft();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, bars.top, right, bottom);
            return insets;
        });
    }

    /** Pads the view's bottom by the navigation bar height, preserving its other padding. */
    public static void applyBottomInset(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, top, right, bars.bottom);
            return insets;
        });
    }
}
