package io.mrarm.irc.chat;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.Editable;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.WhoisInfo;
import io.mrarm.chatlib.irc.CommandHandlerList;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.handlers.NickCommandHandler;
import io.mrarm.chatlib.irc.handlers.WhoisCommandHandler;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.dialog.UserBottomSheetDialog;
import io.mrarm.irc.util.AutoMultilineTextListener;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.ImageViewTintUtils;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.ChatAutoCompleteEditText;
import io.mrarm.irc.view.TextFormatBar;

public class ChatFragmentSendMessageHelper implements SendMessageHelper.Callback {

    private Context mContext;
    private ChatFragment mFragment;
    private View mSendContainer;
    private ChatAutoCompleteEditText mSendText;
    private AutoMultilineTextListener mSendTextMultilineHelper;
    private ChatSuggestionsAdapter mChannelMembersListAdapter;
    private View mFormatBarDivider;
    private TextFormatBar mFormatBar;
    private ImageView mSendIcon;
    private ImageView mTabIcon;
    private View mClientCommandErrorContainer;
    private TextView mClientCommandErrorText;
    private View mServerMessagesContainer;
    private View mServerMessagesCard;
    private RecyclerView mServerMessagesList;
    private ChatServerMessagesAdapter mServerMessagesListAdapter;
    private String mCurrentChannel;

    public ChatFragmentSendMessageHelper(ChatFragment chatFragment, View rootView) {
        mContext = rootView.getContext();
        mFragment = chatFragment;
        ServerConnectionInfo connectionInfo = mFragment.getConnectionInfo();

        mFormatBar = rootView.findViewById(R.id.format_bar);
        mFormatBarDivider = rootView.findViewById(R.id.format_bar_divider);
        mSendContainer = rootView.findViewById(R.id.send_container);
        mSendText = rootView.findViewById(R.id.send_text);
        mSendIcon = rootView.findViewById(R.id.send_button);
        mTabIcon = rootView.findViewById(R.id.tab_button);

        mSendText.setFormatBar(mFormatBar);
        mSendText.setCustomSelectionActionModeCallback(new FormatItemActionMode());

        mFormatBar.setExtraButton(R.drawable.ic_close, mContext.getString(R.string.action_close), (View v) -> {
            setFormatBarVisible(false);
        });

        RecyclerView suggestionsRecyclerView = rootView.findViewById(R.id.suggestions_list);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mChannelMembersListAdapter = new ChatSuggestionsAdapter(mContext, connectionInfo, null);
        mSendText.setSuggestionsListView(rootView.findViewById(R.id.suggestions_container), rootView.findViewById(R.id.suggestions_card), suggestionsRecyclerView);
        mSendText.setAdapter(mChannelMembersListAdapter);
        mSendText.setCommandListAdapter(new CommandListSuggestionsAdapter(mContext));
        mSendText.setConnectionContext(connectionInfo);
        if (connectionInfo.getApiInstance() instanceof ServerConnectionApi)
            mSendText.setChannelTypes(((ServerConnectionApi) connectionInfo.getApiInstance())
                    .getServerConnectionData().getSupportList().getSupportedChannelTypes());
        rootView.findViewById(R.id.suggestions_dismiss).setOnTouchListener((View view, MotionEvent motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                mSendText.dismissDropDown();
            return true;
        });

        ImageViewTintUtils.setTint(mSendIcon, 0x54000000);
        int accentColor = StyledAttributesHelper.getColor(mContext, R.attr.colorAccent, 0);;

        mSendTextMultilineHelper = new AutoMultilineTextListener(mSendText);
        mSendText.addTextChangedListener(mSendTextMultilineHelper);
        mSendText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (s.length() > 0)
                ImageViewTintUtils.setTint(mSendIcon, accentColor);
            else
                ImageViewTintUtils.setTint(mSendIcon, 0x54000000);
            mClientCommandErrorContainer.setVisibility(View.GONE); // hide the error
        }));
        mSendText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            sendMessage();
            return true;
        });
        mSendIcon.setOnClickListener((View view) -> {
            sendMessage();
        });

        mTabIcon.setOnClickListener((View v) -> {
            mSendText.requestTabComplete();
        });

        mClientCommandErrorContainer = rootView.findViewById(R.id.client_command_error_card);
        mClientCommandErrorText = rootView.findViewById(R.id.client_command_error_text);
        mClientCommandErrorText.setMovementMethod(new LinkMovementMethod());
        rootView.findViewById(R.id.client_command_error_close).setOnClickListener((View v) -> mClientCommandErrorContainer.setVisibility(View.GONE));

        mServerMessagesContainer = rootView.findViewById(R.id.server_messages_container);
        mServerMessagesCard = rootView.findViewById(R.id.server_messages_card);
        mServerMessagesList = rootView.findViewById(R.id.server_messages_list);
        mServerMessagesList.setLayoutManager(new LinearLayoutManager(mContext));
        mServerMessagesListAdapter = new ChatServerMessagesAdapter(mContext);
        mServerMessagesList.setAdapter(mServerMessagesListAdapter);
        mServerMessagesList.setItemAnimator(null);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new MyItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(mServerMessagesList);
        BottomSheetBehavior.from(mServerMessagesCard).setBottomSheetCallback(mServerMessagesBottomSheetCallback);
        rootView.findViewById(R.id.server_messages_dismiss).setOnTouchListener((View view, MotionEvent motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mServerMessagesContainer.setVisibility(View.GONE);
                BottomSheetBehavior.from(mServerMessagesCard).setState(BottomSheetBehavior.STATE_COLLAPSED);
                mServerMessagesListAdapter.clear();
            }
            return true;
        });

        mSendText.requestFocus();

        updateVisibility();
    }

    public void setFormatBarVisible(boolean visible) {
        if (visible) {
            mFormatBar.setVisibility(View.VISIBLE);
            mFormatBarDivider.setVisibility(View.VISIBLE);
        } else {
            mFormatBar.setVisibility(View.GONE);
            mFormatBarDivider.setVisibility(View.GONE);
        }
    }

    public void setTabButtonVisible(boolean visible) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                mSendText.getLayoutParams();
        if (visible) {
            MarginLayoutParamsCompat.setMarginStart(layoutParams, 0);
            mTabIcon.setVisibility(View.VISIBLE);
        } else {
            MarginLayoutParamsCompat.setMarginStart(layoutParams, mContext.getResources()
                    .getDimensionPixelSize(R.dimen.message_edit_text_margin_left));
            mTabIcon.setVisibility(View.GONE);
        }
        mSendText.setLayoutParams(layoutParams);
    }

    public void setAutocorrectEnabled(boolean enabled) {
        boolean wasMultiline =
                (mSendText.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        if (enabled)
            mSendText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        else
            mSendText.setInputType(InputType.TYPE_CLASS_TEXT);

        if (wasMultiline)
            mSendText.setInputType(mSendText.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    public void setAlwaysMultiline(boolean multiline) {
        mSendTextMultilineHelper.setAlwaysMultiline(multiline);
    }

    public void setCurrentChannel(String name) {
        ChannelUIData oldUiData = mFragment.getConnectionInfo().getChatUIData()
                .getOrCreateChannelData(mCurrentChannel);
        oldUiData.setCurrentText(mSendText.getText());
        mCurrentChannel = name;
        ChannelUIData uiData = mFragment.getConnectionInfo().getChatUIData()
                .getOrCreateChannelData(name);
        mSendText.setHistory(uiData.getSentMessageHistory());
        mSendText.setText(uiData.getCurrentText());
        mSendText.setSelection(mSendText.getText().length());
    }

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersListAdapter.setMembers(members);
    }

    public void updateVisibility() {
        mSendContainer.setVisibility(mFragment.getConnectionInfo().isConnected() ||
                mFragment.getConnectionInfo().isConnecting() ? View.VISIBLE : View.GONE);
    }

    public void setMessageFieldTypeface(Typeface typeface) {
        mSendText.setTypeface(typeface);
    }

    public void setMessageText(String text) {
        mSendText.setText(text);
    }

    public void sendMessage() {
        SendMessageHelper.sendMessage(mContext, mFragment.getConnectionInfo(),
                mFragment.getCurrentChannel(), mSendText.getText(), this);
    }

    @Override
    public void onMessageSent() {
        mSendText.setText("");
    }

    @Override
    public void onRawCommandExecuted(String clientCommand, String sentCommand) {
        if (sentCommand.startsWith("QUIT ") || sentCommand.equals("QUIT")) {
            mFragment.getConnectionInfo().notifyUserExecutedQuit();
        }
        setupCommandResultHandler((IRCConnection) mFragment.getConnectionInfo().getApiInstance(),
                clientCommand, sentCommand);
    }

    @Override
    public void onNoCommandHandlerFound(String message) {
        ColoredTextBuilder builder = new ColoredTextBuilder();
        builder.append(mContext.getString(R.string.command_error_not_found));
        builder.append("  ");
        builder.append(mContext.getString(R.string.command_send_raw), new ClickableSpan() {
            @Override
            public void onClick(View view) {
                ((IRCConnection) mFragment.getConnectionInfo().getApiInstance()).sendCommandRaw(
                        message.substring(1), null, null);
                mClientCommandErrorContainer.setVisibility(View.GONE);
                mFragment.getConnectionInfo().getChatUIData().getOrCreateChannelData(null)
                        .addHistoryMessage(message);
                mSendText.setText("");
            }
        });
        onClientCommandError(builder.getSpannable());
    }

    @Override
    public void onClientCommandError(CharSequence message) {
        mClientCommandErrorText.setText(message);
        mClientCommandErrorContainer.setVisibility(View.VISIBLE);
        mSendText.dismissDropDown();
    }

    private void setupCommandResultHandler(IRCConnection connection, String clientCommand, String sentCommand) {
        String[] params = sentCommand.split(" ");
        if (params.length == 0)
            return;
        CommandHandlerList l = connection.getServerConnectionData().getCommandHandlerList();
        if (params[0].equalsIgnoreCase("WHOIS")) {
            l.getHandler(WhoisCommandHandler.class).onRequested(params.length > 1 ? params[1] : null, (WhoisInfo whoisInfo) -> {
                mFragment.getActivity().runOnUiThread(() -> {
                    UserBottomSheetDialog dialog = new UserBottomSheetDialog(mContext);
                    dialog.setConnection(mFragment.getConnectionInfo());
                    dialog.setData(whoisInfo);
                    Dialog d = dialog.show();
                    ((MainActivity) mFragment.getActivity()).setFragmentDialog(d);
                });
            }, (String n, int i, String m) -> {
                notifyCommandFailed(clientCommand, m);
            });
        } else if (params[0].equalsIgnoreCase("NICK")) {
            if (params.length > 1 && params[1].equals(connection.getServerConnectionData().getUserNick()))
                return;
            l.getHandler(NickCommandHandler.class).onRequested(params.length > 1 ? params[1] : null, null, (String n, int i, String m) -> {
                notifyCommandFailed(clientCommand, m);
            });
        } else if (params[0].equalsIgnoreCase("JOIN")) {
            if (params.length < 2)
                return;
            mFragment.setAutoOpenChannel(params[1]);
        }
    }

    private void notifyCommandFailed(String command, CharSequence message) {
        if (mFragment.getActivity() == null)
            return;
        mFragment.getActivity().runOnUiThread(() -> {
            mServerMessagesListAdapter.addItem(new ChatServerMessagesAdapter.CommandErrorItem(mServerMessagesListAdapter, command, message, mCommandErrorClickListener));
            BottomSheetBehavior b = BottomSheetBehavior.from(mServerMessagesCard);
            if (b.getState() != BottomSheetBehavior.STATE_EXPANDED)
                b.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mServerMessagesContainer.setVisibility(View.VISIBLE);
        });
    }


    public boolean hasSendMessageTextSelection() {
        return (mSendText != null && mSendText.getSelectionEnd() - mSendText.getSelectionStart() > 0);
    }

    private final BottomSheetBehavior.BottomSheetCallback mServerMessagesBottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                mServerMessagesContainer.setVisibility(View.GONE);
                mServerMessagesListAdapter.clear();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }

    };

    private final ChatServerMessagesAdapter.CommandErrorItem.OnClickListener mCommandErrorClickListener = (ChatServerMessagesAdapter.CommandErrorItem i) -> {
        mSendText.setText(i.getCommand());
        mSendText.setSelection(i.getCommand().length());
        mServerMessagesListAdapter.removeItem(i);
        if (mServerMessagesListAdapter.getItemCount() == 0)
            mServerMessagesContainer.setVisibility(View.GONE);
    };


    private class FormatItemActionMode implements ActionMode.Callback {

        private MenuItem mFormatMenuItem;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mFormatMenuItem = menu.add(R.string.message_format)
                    .setIcon(R.drawable.ic_text_format);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mFormatMenuItem == item) {
                setFormatBarVisible(true);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

    }

    private class MyItemTouchHelperCallback extends ItemTouchHelper.Callback {

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.START | ItemTouchHelper.END);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            mServerMessagesListAdapter.removeItem(viewHolder.getAdapterPosition());
            if (mServerMessagesListAdapter.getItemCount() == 0)
                mServerMessagesContainer.setVisibility(View.GONE);
        }

    }
}
