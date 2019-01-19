package io.mrarm.irc.chat;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.util.SpannableStringHelper;

public class ChannelUIData {

    private static final int HISTORY_MAX_COUNT = 24;

    private final List<CharSequence> mSentMessageHistory = new ArrayList<>();
    private CharSequence mCurrentText = null;

    public void setCurrentText(CharSequence currentText) {
        if (currentText == null || currentText.length() == 0)
            mCurrentText = null;
        else
            mCurrentText = SpannableStringHelper.copyCharSequence(currentText);
    }

    public CharSequence getCurrentText() {
        return mCurrentText;
    }

    public List<CharSequence> getSentMessageHistory() {
        return mSentMessageHistory;
    }

    public void addHistoryMessage(CharSequence msg) {
        mSentMessageHistory.add(SpannableStringHelper.copyCharSequence(msg));
        if (mSentMessageHistory.size() >= HISTORY_MAX_COUNT)
            mSentMessageHistory.remove(0);
    }

}
