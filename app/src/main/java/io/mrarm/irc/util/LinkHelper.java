package io.mrarm.irc.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.view.View;

import java.lang.ref.WeakReference;
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

    private static final Pattern sChannelLinksPattern = Pattern.compile("(^| )(#[a-zA-Z]*)(?=$| |,)");

    public static CharSequence addLinks(CharSequence spannable) {
        if (!(spannable instanceof Spannable))
            spannable = new SpannableString(spannable);
        Linkify.addLinks((Spannable) spannable, Linkify.WEB_URLS);
        Matcher matcher = sChannelLinksPattern.matcher(spannable);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            String text = matcher.group(2);
            ((Spannable) spannable).setSpan(new ChannelLinkSpan(text), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static class ChannelLinkSpan extends ClickableSpan {

        private final String mChannel;

        public ChannelLinkSpan(String channel) {
            mChannel = channel;
        }

        @Override
        public void onClick(View view) {
            MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(view.getContext());
            dialog.addHeader(mChannel);
            dialog.addItem(R.string.action_open, R.drawable.ic_open_in_new, (MenuBottomSheetDialog.Item item) -> {
                MainActivity activity = ((MainActivity) view.getContext());
                ChatFragment fragment = (ChatFragment) activity.getCurrentFragment();
                List<String> channels = new ArrayList<>();
                channels.add(mChannel);
                ServerConnectionInfo connection = fragment.getConnectionInfo();
                if (connection.getChannels().contains(mChannel)) {
                    activity.openServer(connection, mChannel);
                    return true;
                }
                connection.getApiInstance().joinChannels(channels, (Void v) -> {
                    if (fragment.getConnectionInfo().getChannels().contains(mChannel)) {
                        activity.runOnUiThread(() -> activity.openServer(connection, mChannel));
                        return;
                    }
                    connection.addOnChannelListChangeListener(
                            new OpenTaskChannelListListener(activity, connection, mChannel));
                }, null);
                return true;
            });
            dialog.show();
        }

    }

    private static class OpenTaskChannelListListener implements ServerConnectionInfo.ChannelListChangeListener {

        private WeakReference<MainActivity> mActivity;
        private ServerConnectionInfo mConnection;
        private String mChannel;

        public OpenTaskChannelListListener(MainActivity activity, ServerConnectionInfo info, String channel) {
            mActivity = new WeakReference<>(activity);
            mConnection = info;
            mChannel = channel;
        }

        @Override
        public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
            MainActivity activity = mActivity.get();
            if (newChannels.contains(mChannel) && activity != null) {
                activity.runOnUiThread(() -> activity.openServer(mConnection, mChannel));
            }
            connection.removeOnChannelListChangeListener(this);
        }

    }

}
