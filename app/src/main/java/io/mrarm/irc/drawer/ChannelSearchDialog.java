package io.mrarm.irc.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.dialog.SearchDialog;

public class ChannelSearchDialog extends SearchDialog {

    private int mSecondaryTextColor;
    private int mHightlightTextColor;

    public ChannelSearchDialog(@NonNull Context context) {
        super(context);
        TypedArray ta = context.obtainStyledAttributes(new int[] { R.attr.colorBackgroundFloating, android.R.attr.textColorSecondary });
        setBackgroundColor(ta.getColor(0, 0));
        mSecondaryTextColor = ta.getColor(1, 0);
        ta.recycle();
        mHightlightTextColor = context.getResources().getColor(R.color.searchColorHighlight);
        onQueryTextChange("");
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onSuggestionClicked(int index, CharSequence suggestion) {
        String query = getCurrentQuery();
        for (ServerConnectionInfo info : ServerConnectionManager.getInstance(getContext())
                .getConnections()) {
            for (String channel : info.getChannels()) {
                int iof = channel.indexOf(query);
                if (iof != -1 && index-- == 0) {
                    ((MainActivity) getOwnerActivity()).openServer(info, channel);
                    dismiss();
                    return;
                }
            }
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        List<CharSequence> ret = new ArrayList<>();
        for (ServerConnectionInfo info : ServerConnectionManager.getInstance(getContext())
                .getConnections()) {
            for (String channel : info.getChannels()) {
                int iof = channel.indexOf(newText);
                if (iof != -1) {
                    SpannableString str = new SpannableString(channel + "  " + info.getName());
                    str.setSpan(new ForegroundColorSpan(mHightlightTextColor), iof, iof + newText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    str.setSpan(new ForegroundColorSpan(mSecondaryTextColor), channel.length() + 2, channel.length() + 2 + info.getName().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ret.add(str);
                }
            }
        }
        setSuggestions(ret);
        return true;
    }

}
