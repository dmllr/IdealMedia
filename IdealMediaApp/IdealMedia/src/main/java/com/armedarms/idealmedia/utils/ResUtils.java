package com.armedarms.idealmedia.utils;

import android.content.Context;
import android.util.TypedValue;

import com.armedarms.idealmedia.R;

public class ResUtils {
    public static int resolve(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId;
    }
    public static int color(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
