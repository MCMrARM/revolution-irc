package io.mrarm.irc.util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import io.mrarm.irc.ColorPickerDialog;
import io.mrarm.irc.R;

public class MessageFormatSettingsFormatBar extends TextFormatBar {

    public MessageFormatSettingsFormatBar(Context context) {
        super(context);
    }

    public MessageFormatSettingsFormatBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageFormatSettingsFormatBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected ColorPickerDialog createColorPicker(boolean fillColor, int selectedColor) {
        ColorPickerDialog ret = super.createColorPicker(fillColor, selectedColor);
        if (!fillColor) {
            ret.setColors(getResources().getIntArray(R.array.colorPickerColors), -1);
            ret.setSelectedColor(selectedColor);
            ret.setNeutralButton(R.string.message_format_sender_color,
                    (DialogInterface dialog, int which) -> {
                        setSpan(new MessageBuilder.MetaForegroundColorSpan(getContext(),
                                MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER));
                    });
        }
        return ret;
    }

}