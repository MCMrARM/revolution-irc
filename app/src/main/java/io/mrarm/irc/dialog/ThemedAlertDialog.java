package io.mrarm.irc.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedAlertDialog {

    public static class Builder extends AlertDialog.Builder {

        public Builder(@NonNull Context context) {
            super(context);
        }

        public Builder(@NonNull Context context, int themeResId) {
            super(context, themeResId);
        }

        @Override
        public AlertDialog create() {
            AlertDialog ret = super.create();
            ret.setOnShowListener((DialogInterface dialogInterface) -> {
                themeButton(ret.getButton(AlertDialog.BUTTON_POSITIVE));
                themeButton(ret.getButton(AlertDialog.BUTTON_NEGATIVE));
                themeButton(ret.getButton(AlertDialog.BUTTON_NEUTRAL));
            });
            return ret;
        }
    }

    private static void themeButton(Button b) {
        if (b != null)
            b.setTextColor(ThemeHelper.getAccentColor(b.getContext()));
    }

}
