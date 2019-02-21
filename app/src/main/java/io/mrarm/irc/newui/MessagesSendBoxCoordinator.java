package io.mrarm.irc.newui;

import android.text.Editable;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.chat.ChannelUIData;
import io.mrarm.irc.chat.ChatSuggestionsAdapter;
import io.mrarm.irc.chat.CommandListSuggestionsAdapter;
import io.mrarm.irc.chat.SendMessageHelper;
import io.mrarm.irc.config.FeatureFlags;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.view.ChatAutoCompleteEditText;

public class MessagesSendBoxCoordinator implements ChatAutoCompleteEditText.SuggestionListDropDown,
        ChannelInfoData.MemberListListener, SendMessageHelper.Callback {

    private ChatAutoCompleteEditText mSendText;
    private AppCompatImageView mSendButton;

    private CoordinatorLayout mOverlayLayout;
    private RecyclerView mSuggestionList;
    private View mSuggestionListDismissView;
    private View mSuggestionListCard;

    private ServerConnectionInfo mConnection;
    private String mChannelName;
    private ChannelInfoData mChannelInfoData;
    private ChatSuggestionsAdapter mSuggestionListAdapter;

    public MessagesSendBoxCoordinator(View sendBox, View overlay) {
        mSendText = sendBox.findViewById(R.id.send_text);
        mSendButton = sendBox.findViewById(R.id.send_button);

        mOverlayLayout = overlay.findViewById(R.id.overlay_container);
        mSuggestionList = overlay.findViewById(R.id.suggestion_list);
        mSuggestionListCard = overlay.findViewById(R.id.suggestion_list_card);
        mSuggestionListDismissView = overlay.findViewById(R.id.suggestion_list_dismiss);
        mSuggestionList.setLayoutManager(new LinearLayoutManager(sendBox.getContext()));

        mSendText.setSuggestionListDropDown(this);
        mSendText.setCommandListAdapter(new CommandListSuggestionsAdapter(sendBox.getContext()));
        mSendText.addTextChangedListener(new SimpleTextWatcher(this::onSendTextChanged));

        mSendButton.setOnClickListener((v) -> sendMessage());

        BottomSheetBehavior.from(mSuggestionListCard).setBottomSheetCallback(
                new BottomSheetDismissCallback(this::onDropDownBottomSheetHidden));
        if (FeatureFlags.areChatSuggestionListAnimationsEnabled()) {
            BottomSheetBehavior.from(mSuggestionListCard).setHideable(true);
            BottomSheetBehavior.from(mSuggestionListCard).setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    public void setConnectionContext(ServerConnectionInfo connection) {
        mConnection = connection;
        mSuggestionListAdapter = new ChatSuggestionsAdapter(mSuggestionList.getContext(),
                mConnection, null);
        mSendText.setConnectionContext(connection);
        mSendText.setAdapter(mSuggestionListAdapter);
    }

    public void setChannelContext(String channel, ChannelInfoData infoData) {
        ChannelUIData oldUiData = mConnection.getChatUIData().getOrCreateChannelData(mChannelName);
        oldUiData.setCurrentText(mSendText.getText());
        mChannelName = channel;
        if (mChannelInfoData != null)
            mChannelInfoData.addMemberListListener(this);
        mChannelInfoData = infoData;
        ChannelUIData uiData = mConnection.getChatUIData().getOrCreateChannelData(channel);
        mSendText.setHistory(uiData.getSentMessageHistory());
        mSendText.setText(uiData.getCurrentText());
        mSendText.setSelection(mSendText.length());
        mSuggestionListAdapter.setMembers(infoData.getMembers());
        infoData.addMemberListListener(this);
    }

    @Override
    public void showDropDown() {
        mSuggestionListCard.setVisibility(View.VISIBLE);
        mSuggestionListDismissView.setVisibility(View.VISIBLE);
        if (FeatureFlags.areChatSuggestionListAnimationsEnabled()) {
            mSuggestionList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    BottomSheetBehavior.from(mSuggestionListCard).setState(BottomSheetBehavior.STATE_COLLAPSED);
                    BottomSheetBehavior.from(mSuggestionListCard).setHideable(false);
                    mSuggestionList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    @Override
    public void dismissDropDown() {
        if (FeatureFlags.areChatSuggestionListAnimationsEnabled()) {
            BottomSheetBehavior.from(mSuggestionListCard).setHideable(true);
            BottomSheetBehavior.from(mSuggestionListCard).setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            onDropDownBottomSheetHidden();
        }
    }

    private void onDropDownBottomSheetHidden() {
        mSuggestionListCard.setVisibility(View.GONE);
        mSuggestionListDismissView.setVisibility(View.GONE);
        mSuggestionList.setAdapter(null);
    }

    @Override
    public void setDropDownAdapter(RecyclerView.Adapter adapter) {
        mSuggestionList.setAdapter(adapter);
    }

    @Override
    public RecyclerView.Adapter getDropDownAdapter() {
        return mSuggestionList.getAdapter();
    }

    @Override
    public void scrollDropDownToPosition(int position) {
        mSuggestionList.scrollToPosition(position);
    }

    @Override
    public void onMemberListChanged(ChannelInfoData data) {
        mSuggestionListAdapter.setMembers(data.getMembers());
    }

    private void onSendTextChanged(Editable s) {
        mSendButton.setEnabled(s.length() > 0);
    }

    public void sendMessage() {
        SendMessageHelper.sendMessage(mSendText.getContext(), mConnection, mChannelName,
                mSendText.getText(), this);
    }

    @Override
    public void onMessageSent() {
        mSendText.setText("");
    }

    @Override
    public void onRawCommandExecuted(String clientCommand, String sentCommand) {
    }

    @Override
    public void onNoCommandHandlerFound(String message) {
    }

    @Override
    public void onClientCommandError(CharSequence error) {
    }

    private static class BottomSheetDismissCallback extends BottomSheetBehavior.BottomSheetCallback {

        private Runnable mHideListener;

        public BottomSheetDismissCallback(Runnable runnable) {
            mHideListener = runnable;
        }

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN)
                mHideListener.run();
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }

    }

}
