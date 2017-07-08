package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.NickChangeMessageInfo;
import io.mrarm.irc.R;

public class MessageBuilder {

    private static MessageBuilder sInstance;

    private static final int FORMAT_SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_PRIORITY;

    private static SimpleDateFormat sDefaultMessageTimeFormat = new SimpleDateFormat("[HH:mm]",
            Locale.getDefault());

    public static MessageBuilder getInstance(Context context) {
        if (sInstance == null)
            sInstance = new MessageBuilder(context.getApplicationContext());
        return sInstance;
    }

    private Context mContext;
    private SimpleDateFormat mMessageTimeFormat = sDefaultMessageTimeFormat;
    private CharSequence mMessageFormat;
    private CharSequence mActionMessageFormat;
    private CharSequence mEventMessageFormat;

    private static SpannableString buildDefaultMessageFormat(Context context) {
        SpannableString spannable = new SpannableString("%t %s: %m");
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 2, FORMAT_SPAN_FLAGS);
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_SENDER), 3, 6, FORMAT_SPAN_FLAGS);
        return spannable;
    }

    private static SpannableString buildDefaultActionMessageFormat(Context context) {
        SpannableString spannable = new SpannableString("%t * %s %m");
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 2, FORMAT_SPAN_FLAGS);
        spannable.setSpan(new StyleSpan(Typeface.ITALIC), 3, 10, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_STATUS), 3, 4, FORMAT_SPAN_FLAGS);
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_SENDER), 5, 7, FORMAT_SPAN_FLAGS);
        return spannable;
    }

    private static SpannableString buildDefaultEventMessageFormat(Context context) {
        SpannableString spannable = new SpannableString("%t * %m");
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 2, FORMAT_SPAN_FLAGS);
        spannable.setSpan(new StyleSpan(Typeface.ITALIC), 3, 7, FORMAT_SPAN_FLAGS);
        spannable.setSpan(new MetaForegroundColorSpan(context, MetaForegroundColorSpan.COLOR_STATUS), 3, 7, FORMAT_SPAN_FLAGS);
        return spannable;
    }

    public MessageBuilder(Context context) {
        mContext = context;
        mMessageFormat = buildDefaultMessageFormat(context);
        mActionMessageFormat = buildDefaultActionMessageFormat(context);
        mEventMessageFormat = buildDefaultEventMessageFormat(context);
    }

    public SimpleDateFormat getMessageTimeFormat() {
        return mMessageTimeFormat;
    }

    public void setMessageTimeFormat(String format) {
        mMessageTimeFormat = new SimpleDateFormat(format, Locale.getDefault());
    }

    public CharSequence getMessageFormat() {
        return mMessageFormat;
    }

    public void setMessageFormat(CharSequence format) {
        mMessageFormat = format;
    }

    public CharSequence getActionMessageFormat() {
        return mActionMessageFormat;
    }

    public void setActionMessageFormat(CharSequence format) {
        mActionMessageFormat = format;
    }

    public CharSequence getEventMessageFormat() {
        return mEventMessageFormat;
    }

    public void setEventMessageFormat(CharSequence format) {
        mEventMessageFormat = format;
    }

    public void appendTimestamp(ColoredTextBuilder builder, Date date) {
        builder.append(mMessageTimeFormat.format(date), new ForegroundColorSpan(mContext.getResources().getColor(R.color.messageTimestamp)));
    }

    public CharSequence buildDisconnectWarning(Date date) {
        ColoredTextBuilder builder = new ColoredTextBuilder();
        appendTimestamp(builder, date);
        builder.append("Disconnected", new ForegroundColorSpan(mContext.getResources().getColor(R.color.messageDisconnected)));
        return builder.getSpannable();
    }

    private CharSequence buildColoredNick(String nick) {
        SpannableString spannable = new SpannableString(nick);
        spannable.setSpan(new ForegroundColorSpan(IRCColorUtils.getNickColor(mContext, nick)), 0, nick.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannable;
    }

    public CharSequence buildMessage(MessageInfo message) {
        String senderNick = message.getSender() == null ? null : message.getSender().getNick();
        switch (message.getType()) {
            case NORMAL:
                return processFormat(mMessageFormat, message.getDate(), senderNick,
                        IRCColorUtils.getFormattedString(mContext, message.getMessage()));
            case ME:
                return processFormat(mActionMessageFormat, message.getDate(), senderNick,
                        IRCColorUtils.getFormattedString(mContext, message.getMessage()));
            case JOIN:
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        SpannableStringHelper.getText(mContext, R.string.message_join, buildColoredNick(senderNick)));
            case NICK_CHANGE: {
                String newNick = ((NickChangeMessageInfo) message).getNewNick();
                SpannableStringBuilder ssb = (SpannableStringBuilder) SpannableStringHelper.getText(mContext, R.string.message_nick_change, buildColoredNick(senderNick), buildColoredNick(newNick));
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        ssb);
            }
            case DISCONNECT_WARNING:
                return buildDisconnectWarning(message.getDate());
        }
        return "Test";
    }

    private CharSequence processFormat(CharSequence format, Date date, String sender,
                                       CharSequence message) {
        int nickColor = sender == null ? 0 : IRCColorUtils.getNickColor(mContext, sender);
        SpannableStringBuilder builder = new SpannableStringBuilder(format);
        for (MetaForegroundColorSpan span : builder.getSpans(0, builder.length(), MetaForegroundColorSpan.class)) {
            int color = MetaForegroundColorSpan.resolveColor(mContext, span.getColorId());
            if (span.getColorId() == MetaForegroundColorSpan.COLOR_SENDER)
                color = nickColor;
            builder.setSpan(new ForegroundColorSpan(color), builder.getSpanStart(span),
                    builder.getSpanEnd(span), builder.getSpanFlags(span));
            builder.removeSpan(span);
        }
        for (int i = 0; i < builder.length() - 1; i++) {
            if (builder.charAt(i) == '%') {
                int c = builder.charAt(++i);
                CharSequence replacement = null;
                switch (c) {
                    case 't':
                        replacement = mMessageTimeFormat.format(date);
                        break;
                    case 's':
                        replacement = sender;
                        break;
                    case 'm':
                        replacement = message;
                        break;
                    case '%':
                        replacement = "%";
                        break;
                }
                if (replacement != null) {
                    builder.replace(i - 1, i + 1, replacement);
                    i += replacement.length() - 2;
                }
            }
        }
        return new SpannableString(builder);
    }

    public static class MetaForegroundColorSpan extends ForegroundColorSpan {

        public static final int COLOR_SENDER = -1;
        public static final int COLOR_TIMESTAMP = -2;
        public static final int COLOR_STATUS = -3;

        private int mColorId;

        public static int resolveColor(Context context, int colorId) {
            if (colorId == COLOR_SENDER)
                return context.getResources().getColor(R.color.messageSenderFallbackColor);
            if (colorId == COLOR_TIMESTAMP)
                return IRCColorUtils.getTimestampTextColor(context);
            if (colorId == COLOR_STATUS)
                return IRCColorUtils.getStatusTextColor(context);
            return IRCColorUtils.getColor(context, colorId);
        }

        public MetaForegroundColorSpan(Context context, int colorId) {
            super(resolveColor(context, colorId));
            mColorId = colorId;
        }

        public int getColorId() {
            return mColorId;
        }

    }

}
