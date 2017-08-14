package io.mrarm.irc.chat;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.dialog.UserBottomSheetDialog;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.ImageViewTintUtils;
import io.mrarm.irc.util.SimpleTextVariableList;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.view.ChatAutoCompleteEditText;
import io.mrarm.irc.view.ProgressBar;
import io.mrarm.irc.view.TextFormatBar;

public class ChatFragmentSendMessageHelper {

    private Context mContext;
    private ChatFragment mFragment;
    private ChatAutoCompleteEditText mSendText;
    private ChatSuggestionsAdapter mChannelMembersListAdapter;
    private View mFormatBarDivider;
    private TextFormatBar mFormatBar;
    private ImageView mSendIcon;
    private ProgressBar mSendProgressBar;
    private ImageView mTabIcon;
    private View mCommandErrorContainer;
    private TextView mCommandErrorText;

    public ChatFragmentSendMessageHelper(ChatFragment chatFragment, View rootView) {
        mContext = rootView.getContext();
        mFragment = chatFragment;
        ServerConnectionInfo connectionInfo = mFragment.getConnectionInfo();

        mFormatBar = rootView.findViewById(R.id.format_bar);
        mFormatBarDivider = rootView.findViewById(R.id.format_bar_divider);
        mSendText = rootView.findViewById(R.id.send_text);
        mSendIcon = rootView.findViewById(R.id.send_button);
        mSendProgressBar = rootView.findViewById(R.id.send_progress);
        mTabIcon = rootView.findViewById(R.id.tab_button);

        mSendText.setFormatBar(mFormatBar);
        mSendText.setCustomSelectionActionModeCallback(new FormatItemActionMode());

        mFormatBar.setExtraButton(R.drawable.ic_close, mContext.getString(R.string.action_close), (View v) -> {
            setFormatBarVisible(false);
        });

        RecyclerView suggestionsRecyclerView = rootView.findViewById(R.id.suggestions_list);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mChannelMembersListAdapter = new ChatSuggestionsAdapter(connectionInfo, null);
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

        mSendText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            int accentColor = mContext.getResources().getColor(R.color.colorAccent);
            if (s.length() > 0)
                ImageViewTintUtils.setTint(mSendIcon, accentColor);
            else
                ImageViewTintUtils.setTint(mSendIcon, 0x54000000);
            mCommandErrorContainer.setVisibility(View.GONE); // hide the error
        }));
        mSendText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND)
                sendMessage();
            return false;
        });
        mSendIcon.setOnClickListener((View view) -> {
            sendMessage();
        });

        mTabIcon.setOnClickListener((View v) -> {
            mSendText.requestTabComplete();
        });

        mCommandErrorContainer = rootView.findViewById(R.id.command_error_card);
        mCommandErrorText = rootView.findViewById(R.id.command_error_text);
        mCommandErrorText.setMovementMethod(new LinkMovementMethod());
        rootView.findViewById(R.id.command_error_close).setOnClickListener((View v) -> mCommandErrorContainer.setVisibility(View.GONE));
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

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersListAdapter.setMembers(members);
    }

    public void sendMessage() {
        String text = IRCColorUtils.convertSpannableToIRCString(mContext, mSendText.getText());
        if (text.length() == 0 || mSendProgressBar.getVisibility() == View.VISIBLE)
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
                    boolean doNotClear = false;
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        if (setupCommandResultHandler(conn, result.text.split(" "))) {
                            setSendingStatus(true);
                            doNotClear = true;
                        }
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        conn.sendMessage(result.channel, result.text, null, null);
                    } else {
                        throw new RuntimeException();
                    }
                    if (!doNotClear)
                        mSendText.setText("");
                    return;
                }
            } catch (RuntimeException e) {
                setCommandError(mContext.getString(R.string.command_error_internal));
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
                    mCommandErrorContainer.setVisibility(View.GONE);
                }
            });
            setCommandError(builder.getSpannable());
            return;
        }
        mSendText.setText("");
        mFragment.getConnectionInfo().getApiInstance().sendMessage(channel, text, null, null);
    }

    private boolean setupCommandResultHandler(IRCConnection connection, String[] command) {
        if (command.length == 0)
            return false;
        CommandHandlerList l = connection.getServerConnectionData().getCommandHandlerList();
        if (command[0].equalsIgnoreCase("WHOIS")) {
            l.getHandler(WhoisCommandHandler.class).onRequested(command.length > 1 ? command[1] : null, (WhoisInfo whoisInfo) -> {
                mFragment.getActivity().runOnUiThread(() -> {
                    notifyCommandSuceeded();
                    UserBottomSheetDialog dialog = new UserBottomSheetDialog(mContext);
                    dialog.setData(whoisInfo);
                    dialog.show();
                });
            }, (String n, int i, String m) -> {
                notifyCommandFailed((n != null ? (n + ": ") : "") + m);
            });
            return true;
        } else if (command[0].equalsIgnoreCase("NICK")) {
            l.getHandler(NickCommandHandler.class).onRequested(command.length > 1 ? command[1] : null, (String nick) -> {
                notifyCommandSuceeded();
            }, (String n, int i, String m) -> {
                notifyCommandFailed((n != null ? (n + ": ") : "") + m);
            });
            return true;
        }
        return false;
    }

    private void setCommandError(CharSequence message) {
        mCommandErrorText.setText(message);
        mCommandErrorContainer.setVisibility(View.VISIBLE);
        mSendText.dismissDropDown();
    }

    private void setSendingStatus(boolean sending) {
        mSendProgressBar.setVisibility(sending ? View.VISIBLE : View.GONE);
        if (sending) {
            mSendText.setFilters(new InputFilter[]{
                    (CharSequence src, int start, int end, Spanned dst, int dstart, int dend)
                            -> dst.subSequence(dstart, dend)
            });
            mSendIcon.setVisibility(View.INVISIBLE);
        } else {
            mSendText.setFilters(new InputFilter[]{});
            mSendIcon.setVisibility(View.VISIBLE);
        }
    }

    private void notifyCommandSuceeded() {
        if (mFragment.getActivity() == null)
            return;
        mFragment.getActivity().runOnUiThread(() -> {
            setSendingStatus(false);
            mSendText.setText("");
        });
    }

    private void notifyCommandFailed(CharSequence message) {
        if (mFragment.getActivity() == null)
            return;
        mFragment.getActivity().runOnUiThread(() -> {
            setSendingStatus(false);
            setCommandError(message);
        });
    }


    public boolean hasSendMessageTextSelection() {
        return (mSendText != null && mSendText.getSelectionEnd() - mSendText.getSelectionStart() > 0);
    }


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

}
