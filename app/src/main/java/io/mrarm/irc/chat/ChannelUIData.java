package io.mrarm.irc.chat;

import android.text.NoCopySpan;
import android.text.SpannableString;

import java.util.ArrayList;
import java.util.List;

public class ChannelUIData {

    private static final int HISTORY_MAX_COUNT = 24;

    private final List<CharSequence> mSentMessageHistory = new ArrayList<>();


    public List<CharSequence> getSentMessageHistory() {
        return mSentMessageHistory;
    }

    public void addHistoryMessage(CharSequence msg) {
        SpannableString str = new SpannableString(msg);
        for (Object o : str.getSpans(0, str.length(), NoCopySpan.class))
            str.removeSpan(o);
        mSentMessageHistory.add(str);
        if (mSentMessageHistory.size() >= HISTORY_MAX_COUNT)
            mSentMessageHistory.remove(0);
    }

}
