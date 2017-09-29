package io.mrarm.irc.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.WhoisInfo;
import io.mrarm.chatlib.irc.CommandHandlerList;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.handlers.NickCommandHandler;
import io.mrarm.chatlib.irc.handlers.WhoisCommandHandler;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.dialog.UserBottomSheetDialog;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.ImageViewTintUtils;
import io.mrarm.irc.util.SimpleTextVariableList;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.ThemeHelper;
import io.mrarm.irc.view.ChatAutoCompleteEditText;
import io.mrarm.irc.view.TextFormatBar;

public class ChatFragmentSendMessageHelper {

    private Context mContext;
    private ChatFragment mFragment;
    private View mSendContainer;
    private ChatAutoCompleteEditText mSendText;
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
        mSendText.setHistory(connectionInfo.getSentMessageHistory());
        if (connectionInfo.getApiInstance() instanceof ServerConnectionApi)
            mSendText.setChannelTypes(((ServerConnectionApi) connectionInfo.getApiInstance())
                    .getServerConnectionData().getSupportList().getSupportedChannelTypes());
        rootView.findViewById(R.id.suggestions_dismiss).setOnTouchListener((View view, MotionEvent motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                mSendText.dismissDropDown();
            return true;
        });

        ImageViewTintUtils.setTint(mSendIcon, 0x54000000);

        mSendText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (s.length() > 0)
                ImageViewTintUtils.setTint(mSendIcon, ThemeHelper.getAccentColor(mFragment.getContext()));
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

    public void setDoubleTapCompleteEnabled(boolean enabled) {
        if (enabled) {
            GestureDetector detector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    mSendText.requestTabComplete();
                    return true;
                }
            });
            mSendText.setOnTouchListener((View v, MotionEvent event) -> detector.onTouchEvent(event));
        } else {
            mSendText.setOnTouchListener(null);
        }
    }

    public void setAutocorrectEnabled(boolean enabled) {
        if (enabled)
            mSendText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        else
            mSendText.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersListAdapter.setMembers(members);
    }

    public void updateVisibility() {
        mSendContainer.setVisibility(mFragment.getConnectionInfo().isConnected() ? View.VISIBLE : View.GONE);
    }

    public void setMessageText(String text) {
        mSendText.setText(text);
    }

    public void sendMessage() {
        String text = IRCColorUtils.convertSpannableToIRCString(mContext, mSendText.getText());
        if (text.length() == 0 || !mFragment.getConnectionInfo().isConnected())
            return;
        String channel = mFragment.getCurrentChannel();
        if (text.charAt(0) == '/') {
            SimpleTextVariableList vars = new SimpleTextVariableList();
            vars.set(CommandAliasManager.VAR_CHANNEL, channel);
            vars.set(CommandAliasManager.VAR_MYNICK, mFragment.getConnectionInfo().getUserNick());
            try {
                IRCConnection conn = (IRCConnection) mFragment.getConnectionInfo().getApiInstance();
                CommandAliasManager.ProcessCommandResult result = CommandAliasManager
                        .getInstance(mContext).processCommand(conn.getServerConnectionData(),
                                text.substring(1), vars);
                if (result != null) {
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        setupCommandResultHandler(conn, text, result.text);
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        if (channel == null)
                            throw new RuntimeException();
                        if (!mFragment.getConnectionInfo().getChannels().contains(result.channel)) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(result.channel);
                            mFragment.getConnectionInfo().getApiInstance().joinChannels(list, (Void v) -> {
                                conn.sendMessage(result.channel, result.text, null, null);
                            }, null);
                        } else {
                            conn.sendMessage(result.channel, result.text, null, null);
                        }
                    } else {
                        throw new RuntimeException();
                    }
                    mFragment.getConnectionInfo().addHistoryMessage(new SpannableString(mSendText.getText()));
                    mSendText.setText("");
                    return;
                }
            } catch (RuntimeException e) {
                setClientCommandError(mContext.getString(R.string.command_error_internal));
                return;
            }
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(mContext.getString(R.string.command_error_not_found));
            builder.append("  ");
            builder.append(mContext.getString(R.string.command_send_raw), new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    ((IRCConnection) mFragment.getConnectionInfo().getApiInstance()).sendCommandRaw(
                            text.substring(1), null, null);
                    mClientCommandErrorContainer.setVisibility(View.GONE);
                }
            });
            setClientCommandError(builder.getSpannable());
            return;
        }
        if (channel == null)
            return;
        mFragment.getConnectionInfo().addHistoryMessage(new SpannableString(mSendText.getText()));
        mSendText.setText("");
        mFragment.getConnectionInfo().getApiInstance().sendMessage(channel, text, null, null);
    }

    private void setClientCommandError(CharSequence message) {
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
                    dialog.setData(whoisInfo);
                    dialog.show();
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
