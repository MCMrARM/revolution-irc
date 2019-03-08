package io.mrarm.irc.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.chat.ChatFragment;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;

public class LinkHelper {

    private static final Pattern sChannelLinksPattern = Pattern.compile("(^| )(#[^ ,\u0007]+)");

    public static CharSequence addLinks(CharSequence spannable) {
        if (!(spannable instanceof Spannable))
            spannable = new SpannableString(spannable);
        Linkify.addLinks((Spannable) spannable, Linkify.WEB_URLS);
        Matcher matcher = sChannelLinksPattern.matcher(spannable);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            String text = matcher.group(2);
            for (Object o : ((Spannable) spannable).getSpans(start, end, URLSpan.class))
                ((Spannable) spannable).removeSpan(o);
            ((Spannable) spannable).setSpan(new ChannelLinkSpan(text), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static class ChannelLinkSpan extends ClickableSpan {

        private final String mChannel;

        public ChannelLinkSpan(String channel) {
            mChannel = channel;
        }

        private MainActivity findActivity(Context ctx) {
            if (ctx instanceof MainActivity)
                return (MainActivity) ctx;
            if (ctx instanceof ContextWrapper)
                return findActivity(((ContextWrapper) ctx).getBaseContext());
            return null;
        }

        @Override
        public void onClick(View view) {
            MainActivity activity = findActivity(view.getContext());
            MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(view.getContext());
            dialog.addHeader(mChannel);
            dialog.addItem(R.string.action_open, R.drawable.ic_open_in_new, (MenuBottomSheetDialog.Item item) -> {
                ChatFragment fragment = (ChatFragment) activity.getCurrentFragment();
                List<String> channels = new ArrayList<>();
                channels.add(mChannel);
                ServerConnectionInfo connection = fragment.getConnectionInfo();
                if (connection.hasChannel(mChannel)) {
                    activity.openServer(connection, mChannel);
                    return true;
                }
                fragment.setAutoOpenChannel(mChannel);
                connection.getApiInstance().joinChannels(channels, null, null);
                return true;
            });
            dialog.show();
            activity.setFragmentDialog(dialog);
        }

    }

}
