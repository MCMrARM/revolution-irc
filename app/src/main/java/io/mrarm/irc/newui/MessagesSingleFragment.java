package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class MessagesSingleFragment extends MessagesFragment {

    public static MessagesSingleFragment newInstance(ServerConnectionInfo server,
                                                     String channelName) {
        MessagesSingleFragment fragment = new MessagesSingleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channelName != null)
            args.putString(ARG_CHANNEL_NAME, channelName);
        fragment.setArguments(args);
        return fragment;
    }

    private ChannelInfoData mChannelInfoData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChannelInfoData = new ChannelInfoData(getConnection(), getChannelName());
        mChannelInfoData.setSortMembers(true);
        mChannelInfoData.load();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChannelInfoData.unload();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_single_fragment, container, false);
        FrameLayout cctr = view.findViewById(R.id.content);
        View cv = super.onCreateView(inflater, cctr, savedInstanceState);
        cctr.addView(cv);

        MessagesSendBoxCoordinator sendBoxCoordinator = new MessagesSendBoxCoordinator(
                view.findViewById(R.id.send_box), view.findViewById(R.id.send_overlay));
        sendBoxCoordinator.setConnectionContext(getConnection());
        sendBoxCoordinator.setChannelContext(getChannelName(), mChannelInfoData);

        return view;
    }
}
