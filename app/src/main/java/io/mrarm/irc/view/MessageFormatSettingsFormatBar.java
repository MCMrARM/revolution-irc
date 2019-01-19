package io.mrarm.irc.view;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.Nullable;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import io.mrarm.irc.dialog.ColorListPickerDialog;
import io.mrarm.irc.R;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.MessageBuilder;

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
    protected ColorListPickerDialog createColorPicker(boolean fillColor, int selectedColor) {
        ColorListPickerDialog ret = super.createColorPicker(fillColor, selectedColor);
        if (!fillColor) {
            ret.setColors(getResources().getIntArray(R.array.formatTextColors), -1);
            ret.setSelectedColor(selectedColor);
            ret.setNeutralButton(R.string.message_format_sender_color,
                    (DialogInterface dialog, int which) -> {
                        setSpan(new MessageBuilder.MetaForegroundColorSpan(getContext(),
                                MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER));
                    });
            ret.setOnColorChangeListener((ColorListPickerDialog d, int newColorIndex, int color) -> {
                removeSpan(ForegroundColorSpan.class);
                if (color == IRCColorUtils.getStatusTextColor(getContext()))
                    setSpan(new MessageBuilder.MetaForegroundColorSpan(getContext(), MessageBuilder.MetaForegroundColorSpan.COLOR_STATUS));
                if (color == IRCColorUtils.getTimestampTextColor(getContext()))
                    setSpan(new MessageBuilder.MetaForegroundColorSpan(getContext(), MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP));
                else
                    setSpan(new ForegroundColorSpan(color));
                d.cancel();
            });
        }
        return ret;
    }

}