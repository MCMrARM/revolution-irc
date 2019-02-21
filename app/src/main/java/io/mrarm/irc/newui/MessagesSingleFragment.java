package io.mrarm.irc.newui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class MessagesSingleFragment extends MessagesFragment
        implements SlideableFragmentToolbar.FragmentToolbarCallback {

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

    @Override
    public SlideableFragmentToolbar.ToolbarHolder onCreateToolbar(@NonNull LayoutInflater inflater,
                                                                  @Nullable ViewGroup container) {
        return new ToolbarHolder(this, container);
    }


    public class ToolbarHolder extends SlideableFragmentToolbar.SimpleToolbarHolder {

        private TextView mName;
        private TextView mTopic;

        public ToolbarHolder(Fragment f, ViewGroup parentView) {
            super(LayoutInflater.from(f.getContext()).inflate(
                    R.layout.chat_single_toolbar, parentView, false));
            mName = getView().findViewById(R.id.name);
            mTopic = getView().findViewById(R.id.topic);
            addMenu(R.id.action_menu_view, f);
            addAnimationElement(mName, 0.3f, -0.2f);
            addAnimationElement(mTopic, 0.3f, -0.2f);

            mName.setText(getChannelName());
            mTopic.setText(mChannelInfoData.getTopic());
            mTopic.setVisibility(TextUtils.isEmpty(mChannelInfoData.getTopic())
                    ? View.GONE : View.VISIBLE);
        }

    }

}
