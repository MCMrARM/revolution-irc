package io.mrarm.irc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.mrarm.chatlib.dto.ChannelModeMessageInfo;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.NickChangeMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.irc.MessageFormatSettingsActivity;
import io.mrarm.irc.R;

public class MessageBuilder {

    private static MessageBuilder sInstance;

    public static final int FORMAT_SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_PRIORITY;

    public static final String SPAN_TYPE_META_FOREGROUND = "meta_foreground";
    public static final String SPAN_TYPE_META_CHIP = "meta_chip";

    private static final String DEFAULT_TIME_FORMAT = "[HH:mm]";

    public static MessageBuilder getInstance(Context context) {
        if (sInstance == null)
            sInstance = new MessageBuilder(context.getApplicationContext());
        return sInstance;
    }

    private Context mContext;
    private SimpleDateFormat mMessageTimeFormat;
    private CharSequence mMessageFormat;
    private CharSequence mActionMessageFormat;
    private CharSequence mEventMessageFormat;

    public static SpannableString buildDefaultMessageFormat(Context context) {
        return MessageFormatSettingsActivity.buildPresetMessageFormat(context, 0);
    }

    public static SpannableString buildDefaultActionMessageFormat(Context context) {
        return MessageFormatSettingsActivity.buildActionPresetMessageFormat(context, 0);
    }

    public static SpannableString buildDefaultEventMessageFormat(Context context) {
        return MessageFormatSettingsActivity.buildEventPresetMessageFormat(context, 0);
    }

    public static JsonObject spannableToJson(CharSequence text) {
        JsonObject ret = new JsonObject();
        ret.addProperty("text", text.toString());
        JsonArray a = new JsonArray();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            for (Object span : spannable.getSpans(0, text.length(), Object.class)) {
                JsonObject o = spanToJson(span);
                if (o != null) {
                    o.addProperty("start", spannable.getSpanStart(span));
                    o.addProperty("end", spannable.getSpanEnd(span));
                    o.addProperty("flags", spannable.getSpanFlags(span));
                    a.add(o);
                }
            }
        }
        ret.add("spans", a);
        return ret;
    }

    public static SpannableString spannableFromJson(Context context, JsonObject o) {
        SpannableString spannable = new SpannableString(o.get("text").getAsString());
        for (JsonElement el : o.getAsJsonArray("spans")) {
            if (el instanceof JsonObject) {
                JsonObject obj = (JsonObject) el;
                Object span = spanFromJson(context, obj);
                if (span != null)
                    spannable.setSpan(span, obj.get("start").getAsInt(), obj.get("end").getAsInt(),
                            obj.get("flags").getAsInt());
            }
        }
        return spannable;
    }

    public static JsonObject spanToJson(Object span) {
        if (span instanceof MetaForegroundColorSpan) {
            JsonObject ret = new JsonObject();
            ret.addProperty("type", SPAN_TYPE_META_FOREGROUND);
            ret.addProperty("colorId", ((MetaForegroundColorSpan) span).getColorId());
            return ret;
        } else if (span instanceof MetaChipSpan) {
            JsonObject ret = new JsonObject();
            ret.addProperty("type", SPAN_TYPE_META_CHIP);
            ret.addProperty("chipType", ((MetaChipSpan) span).getType());
            return ret;
        }
        return SpannableStringHelper.spanToJson(span);
    }

    public static Object spanFromJson(Context context, JsonObject obj) {
        String type = obj.get("type").getAsString();
        if (type.equals(SPAN_TYPE_META_FOREGROUND))
            return new MetaForegroundColorSpan(context, obj.get("colorId").getAsInt());
        if (type.equals(SPAN_TYPE_META_CHIP))
            return new MetaChipSpan(context, obj.get("chipType").getAsInt());
        return SpannableStringHelper.spanFromJson(obj);
    }

    public MessageBuilder(Context context) {
        mContext = context;
        SharedPreferences mgr = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            mMessageTimeFormat = new SimpleDateFormat(mgr.getString(
                    SettingsHelper.PREF_MESSAGE_TIME_FORMAT, DEFAULT_TIME_FORMAT), Locale.getDefault());
        } catch (Exception ignored) {
        }
        mMessageFormat = getMessageFormat(mgr, SettingsHelper.PREF_MESSAGE_FORMAT);
        if (mMessageFormat == null)
            mMessageFormat = buildDefaultMessageFormat(context);
        mActionMessageFormat = getMessageFormat(mgr, SettingsHelper.PREF_MESSAGE_FORMAT_ACTION);
        if (mActionMessageFormat == null)
            mActionMessageFormat = buildDefaultActionMessageFormat(context);
        mEventMessageFormat = getMessageFormat(mgr, SettingsHelper.PREF_MESSAGE_FORMAT_EVENT);
        if (mEventMessageFormat == null)
            mEventMessageFormat = buildDefaultEventMessageFormat(context);
    }

    public void saveFormats() {
        SharedPreferences.Editor mgr = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        mgr.putString(SettingsHelper.PREF_MESSAGE_TIME_FORMAT, mMessageTimeFormat.toPattern());
        mgr.putString(SettingsHelper.PREF_MESSAGE_FORMAT, SettingsHelper.getGson().toJson(spannableToJson(mMessageFormat)));
        mgr.putString(SettingsHelper.PREF_MESSAGE_FORMAT_ACTION, SettingsHelper.getGson().toJson(spannableToJson(mActionMessageFormat)));
        mgr.putString(SettingsHelper.PREF_MESSAGE_FORMAT_EVENT, SettingsHelper.getGson().toJson(spannableToJson(mEventMessageFormat)));
        mgr.apply();
    }

    private CharSequence getMessageFormat(SharedPreferences mgr, String key) {
        try {
            String s = mgr.getString(key, null);
            if (s == null)
                return null;
            JsonObject o = SettingsHelper.getGson().fromJson(s, JsonObject.class);
            return spannableFromJson(mContext, o);
        } catch (Exception ignored) {
        }
        return null;
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
        builder.append(" Disconnected", new ForegroundColorSpan(mContext.getResources().getColor(R.color.messageDisconnected)));
        return builder.getSpannable();
    }

    private CharSequence buildColoredMessage(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private CharSequence buildColoredNick(String nick) {
        return buildColoredMessage(nick, IRCColorUtils.getNickColor(mContext, nick));
    }

    public CharSequence buildMessage(MessageInfo message) {
        String senderNick = message.getSender() == null ? null : message.getSender().getNick();
        switch (message.getType()) {
            case NORMAL:
                return processFormat(mMessageFormat, message.getDate(), senderNick,
                        addLinks(IRCColorUtils.getFormattedString(mContext, message.getMessage())));
            case ME:
                return processFormat(mActionMessageFormat, message.getDate(), senderNick,
                        addLinks(IRCColorUtils.getFormattedString(mContext, message.getMessage())));
            case JOIN:
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        SpannableStringHelper.getText(mContext, R.string.message_join, buildColoredNick(senderNick)));
            case PART:
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        SpannableStringHelper.getText(mContext, R.string.message_part, buildColoredNick(senderNick), message.getMessage()));
            case NICK_CHANGE: {
                String newNick = ((NickChangeMessageInfo) message).getNewNick();
                SpannableStringBuilder ssb = (SpannableStringBuilder) SpannableStringHelper.getText(mContext, R.string.message_nick_change, buildColoredNick(senderNick), buildColoredNick(newNick));
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        ssb);
            }
            case MODE:
                return processFormat(mEventMessageFormat, message.getDate(), null,
                        buildModeMessage(senderNick, ((ChannelModeMessageInfo) message).getEntries()));
            case DISCONNECT_WARNING:
                return buildDisconnectWarning(message.getDate());
        }
        return "Test";
    }

    public CharSequence buildStatusMessage(StatusMessageInfo message, CharSequence text) {
        return processFormat(mMessageFormat, message.getDate(), message.getSender(),
                IRCColorUtils.getStatusTextColor(mContext), addLinks(text));
    }

    private CharSequence buildModeMessage(String senderNick,
                                          List<ChannelModeMessageInfo.Entry> list) {
        Map<String, Set<Character>> addNickModes = new HashMap<>();
        Map<String, Set<Character>> removeNickModes = new HashMap<>();
        Set<Character> flagModes = new HashSet<>();
        Set<Character> unsetModes = new HashSet<>();
        Map<Character, Set<String>> valueModes = new HashMap<>();
        Map<Character, Set<String>> removeValueModes = new HashMap<>();

        for (ChannelModeMessageInfo.Entry entry : list) {
            if (entry.getType() == ChannelModeMessageInfo.EntryType.NICK_FLAG) {
                Set<Character> setAdd = addNickModes.get(entry.getParam());
                Set<Character> setRem = removeNickModes.get(entry.getParam());
                if (entry.isRemoved()) {
                    if (setRem == null) {
                        setRem = new HashSet<>();
                        removeNickModes.put(entry.getParam(), setRem);
                    }
                    setRem.add(entry.getMode());
                    if (setAdd != null)
                        setAdd.remove(entry.getMode());
                } else {
                    if (setAdd == null) {
                        setAdd = new HashSet<>();
                        addNickModes.put(entry.getParam(), setAdd);
                    }
                    setAdd.add(entry.getMode());
                    if (setRem != null)
                        setRem.remove(entry.getMode());
                }
            } else if (entry.getType() == ChannelModeMessageInfo.EntryType.FLAG) {
                if (entry.isRemoved()) {
                    unsetModes.add(entry.getMode());
                    flagModes.remove(entry.getMode());
                } else {
                    flagModes.add(entry.getMode());
                    unsetModes.remove(entry.getMode());
                }
            } else {
                if (entry.isRemoved()) {
                    if (entry.getType() == ChannelModeMessageInfo.EntryType.VALUE_EXACT_UNSET ||
                            entry.getType() == ChannelModeMessageInfo.EntryType.LIST) {
                        if (!removeValueModes.containsKey(entry.getMode()))
                            removeValueModes.put(entry.getMode(), new HashSet<>());
                        removeValueModes.get(entry.getMode()).add(entry.getParam());
                        if (valueModes.containsKey(entry.getMode()))
                            valueModes.get(entry.getMode()).remove(entry.getParam());
                    } else {
                        unsetModes.add(entry.getMode());
                        valueModes.remove(entry.getMode());
                    }
                } else {
                    if (!valueModes.containsKey(entry.getMode()))
                        valueModes.put(entry.getMode(), new HashSet<>());
                    valueModes.get(entry.getMode()).add(entry.getParam());
                    unsetModes.remove(entry.getMode());
                    removeValueModes.remove(entry.getMode());
                }
            }
        }

        SpannableStringBuilder msg = new SpannableStringBuilder();
        if (flagModes.size() > 0 || valueModes.size() > 0) {
            SpannableStringBuilder setBuilder = new SpannableStringBuilder();
            if (flagModes.size() > 0)
                setBuilder.append(SpannableStringHelper.format(mContext.getResources().getQuantityString(R.plurals.message_mode_channel, flagModes.size()), setToString(flagModes)));
            buildValueModeList(setBuilder, valueModes);
            if (setBuilder.length() > 0)
                appendDelim(msg, SpannableStringHelper.getText(mContext, R.string.message_mode_set, setBuilder));
        }
        if (unsetModes.size() > 0 || removeValueModes.size() > 0) {
            SpannableStringBuilder setBuilder = new SpannableStringBuilder();
            if (flagModes.size() > 0)
                setBuilder.append(SpannableStringHelper.format(mContext.getResources().getQuantityString(R.plurals.message_mode_channel, unsetModes.size()), setToString(unsetModes)));
            buildValueModeList(setBuilder, removeValueModes);
            if (setBuilder.length() > 0)
                appendDelim(msg, SpannableStringHelper.getText(mContext, R.string.message_mode_unset, setBuilder));
        }
        if (addNickModes.size() > 0) {
            SpannableStringBuilder setBuilder = new SpannableStringBuilder();
            for (Map.Entry<String, Set<Character>> entry : addNickModes.entrySet()) {
                if (entry.getValue().size() > 0)
                    appendDelim(setBuilder, SpannableStringHelper.getText(mContext, R.string.message_mode_gave_to, buildNickModeList(entry.getValue()), buildColoredNick(entry.getKey())));
            }
            if (setBuilder.length() > 0)
                appendDelim(msg, SpannableStringHelper.getText(mContext, R.string.message_mode_gave, setBuilder));
        }
        if (removeNickModes.size() > 0) {
            SpannableStringBuilder setBuilder = new SpannableStringBuilder();
            for (Map.Entry<String, Set<Character>> entry : removeNickModes.entrySet()) {
                if (entry.getValue().size() > 0)
                    appendDelim(setBuilder, SpannableStringHelper.getText(mContext, R.string.message_mode_removed_from, buildNickModeList(entry.getValue()), buildColoredNick(entry.getKey())));
            }
            if (setBuilder.length() > 0)
                appendDelim(msg, SpannableStringHelper.getText(mContext, R.string.message_mode_removed, setBuilder));
        }
        return SpannableStringHelper.getText(mContext, R.string.message_mode, buildColoredNick(senderNick), msg);
    }

    private String setToString(Set<Character> s) {
        StringBuilder builder = new StringBuilder(s.size());
        for (char c : s)
            builder.append(c);
        return builder.toString();
    }

    private void buildValueModeList(SpannableStringBuilder builder, Map<Character, Set<String>> modes) {
        for (Map.Entry<Character, Set<String>> mode : modes.entrySet()) {
            for (String val : mode.getValue()) {
                if (mode.getKey() == 'b')
                    appendDelim(builder, SpannableStringHelper.getText(mContext, R.string.message_mode_channel_ban, buildColoredMessage(val, IRCColorUtils.getBanMaskColor(mContext))));
                else
                    appendDelim(builder, SpannableStringHelper.getText(mContext, R.string.message_mode_channel_value, String.valueOf(mode.getKey()), val));
            }
        }
    }

    private SpannableStringBuilder buildNickModeList(Set<Character> s) {
        ColoredTextBuilder b = new ColoredTextBuilder();
        int l = s.size();
        for (char c : s) {
            switch (c) {
                case 'v':
                    b.append(mContext.getString(R.string.message_mode_nick_voice), new ForegroundColorSpan(mContext.getResources().getColor(R.color.memberVoice)));
                    break;
                case 'h':
                    b.append(mContext.getString(R.string.message_mode_nick_half_op), new ForegroundColorSpan(mContext.getResources().getColor(R.color.memberHalfOp)));
                    break;
                case 'o':
                    b.append(mContext.getString(R.string.message_mode_nick_op), new ForegroundColorSpan(mContext.getResources().getColor(R.color.memberOp)));
                    break;
                case 'a':
                    b.append(mContext.getString(R.string.message_mode_nick_admin), new ForegroundColorSpan(mContext.getResources().getColor(R.color.memberAdmin)));
                    break;
                default:
                    b.append("'" + c + "'");
            }

            --l;
            if (l == s.size() - 1 && l > 0)
                b.append(mContext.getString(R.string.text_and));
            else if (l > 0)
                b.append(mContext.getString(R.string.text_comma));
        }
        return b.getSpannable();
    }

    private void appendDelim(SpannableStringBuilder builder, CharSequence seq) {
        if (builder.length() > 0)
            builder.append(mContext.getString(R.string.text_comma));
        builder.append(seq);
    }

    private static CharSequence addLinks(CharSequence spannable) {
        Linkify.addLinks((Spannable) spannable, Linkify.WEB_URLS);
        return spannable;
    }

    private CharSequence processFormat(CharSequence format, Date date, String sender,
                                       CharSequence message) {
        int nickColor = sender == null ? 0 : IRCColorUtils.getNickColor(mContext, sender);
        return processFormat(format, date, sender, nickColor, message);
    }

    private CharSequence processFormat(CharSequence format, Date date, String sender,
                                       int senderColor, CharSequence message) {
        SpannableStringBuilder builder = new SpannableStringBuilder(format);
        for (MetaChipSpan span : builder.getSpans(0, builder.length(), MetaChipSpan.class)) {
            CharSequence replacement = null;
            if (span.mType == MetaChipSpan.TYPE_SENDER)
                replacement = sender;
            else if (span.mType == MetaChipSpan.TYPE_MESSAGE)
                replacement = message;
            else if (span.mType == MetaChipSpan.TYPE_TIME)
                replacement = getMessageTimeFormat().format(date);
            if (replacement != null)
                builder.replace(builder.getSpanStart(span), builder.getSpanEnd(span), replacement);
            builder.removeSpan(span);
        }
        for (MetaForegroundColorSpan span : builder.getSpans(0, builder.length(), MetaForegroundColorSpan.class)) {
            int color = MetaForegroundColorSpan.resolveColor(mContext, span.getColorId());
            if (span.getColorId() == MetaForegroundColorSpan.COLOR_SENDER)
                color = senderColor;
            builder.setSpan(new ForegroundColorSpan(color), builder.getSpanStart(span),
                    builder.getSpanEnd(span), builder.getSpanFlags(span));
            builder.removeSpan(span);
        }
        return new SpannableString(builder);
    }

    public static class MetaChipSpan extends SimpleChipSpan {

        public static final int TYPE_SENDER = 0;
        public static final int TYPE_MESSAGE = 1;
        public static final int TYPE_TIME = 2;

        public static String getTextFor(Context context, int type) {
            String text = null;
            if (type == TYPE_SENDER)
                text = context.getString(R.string.message_format_sender);
            else if (type == TYPE_MESSAGE)
                text = context.getString(R.string.message_format_message);
            else if (type == TYPE_TIME)
                text = context.getString(R.string.message_format_time);
            return text;
        }

        private int mType;

        public MetaChipSpan(Context context, int type) {
            super(context, getTextFor(context, type), true);
            mType = type;
        }

        public int getType() {
            return mType;
        }

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
