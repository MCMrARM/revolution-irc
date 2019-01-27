package io.mrarm.irc.util.theme.live;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.R;

public class LiveThemeManager {

    private Activity mActivity;
    private Map<Integer, Integer> mColors = new HashMap<>();
    private Map<Integer, List<ColorPropertyApplier>> mColorAppliers = new HashMap<>();

    public LiveThemeManager(Activity activity) {
        mActivity = activity;

        if (activity instanceof AppCompatActivity)
            addColorProperty(R.attr.colorPrimary, (c) -> {
                ((AppCompatActivity) activity).getSupportActionBar().setBackgroundDrawable(
                        new ColorDrawable(c));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        activity.findViewById(R.id.action_bar_container) != null)
                    activity.findViewById(R.id.action_bar_container).invalidateOutline();
            });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            addColorProperty(R.attr.colorPrimaryDark, (c) -> activity.getWindow().setStatusBarColor(c));
        addColorProperty(android.R.attr.colorBackground, (c) ->
                activity.getWindow().setBackgroundDrawable(new ColorDrawable(c)));
    }

    public int getColor(int res) {
        res = mapKnownProperty(res);
        return mColors.get(res);
    }

    public void setColorProperty(int res, int value) {
        mColors.put(res, value);
        List<ColorPropertyApplier> appliers = mColorAppliers.get(res);
        if (appliers != null) {
            for (ColorPropertyApplier applier : appliers)
                applier.onApplyColor(value);
        }
    }

    public void addColorProperty(int res, LiveThemeManager.ColorPropertyApplier applier) {
        res = mapKnownProperty(res);
        List<ColorPropertyApplier> appliers = mColorAppliers.get(res);
        if (appliers == null) {
            appliers = new ArrayList<>();
            mColorAppliers.put(res, appliers);
        }
        appliers.add(applier);
        Integer color = mColors.get(res);
        if (color != null)
            applier.onApplyColor(color);
    }

    private int mapKnownProperty(int res) {
        if (res == R.color.lt_colorPrimary)
            res = R.attr.colorPrimary;
        if (res == R.color.lt_colorPrimaryDark)
            res = R.attr.colorPrimaryDark;
        if (res == R.color.lt_colorAccent)
            res = R.attr.colorAccent;
        if (res == R.color.lt_colorBackgroundFloating)
            res = R.attr.colorBackgroundFloating;
        if (res == R.color.lt_textColorPrimary)
            res = android.R.attr.textColorPrimary;
        if (res == R.color.lt_textColorSecondary)
            res = android.R.attr.textColorSecondary;
        if (res == R.color.lt_actionBarTextColorPrimary)
            res = R.attr.actionBarTextColorPrimary;
        if (res == R.color.lt_actionBarTextColorSecondary)
            res = R.attr.actionBarTextColorSecondary;

        return res;
    }

    public interface ColorPropertyApplier {

        void onApplyColor(int color);

    }

}
