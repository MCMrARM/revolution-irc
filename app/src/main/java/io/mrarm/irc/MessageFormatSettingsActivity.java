package io.mrarm.irc;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.util.FormattableEditText;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.TextFormatBar;

public class MessageFormatSettingsActivity extends AppCompatActivity {

    private MessageBuilder mMessageBuilder;

    private EditText mDateFormat;
    private TextInputLayout mDateFormatCtr;
    private FormattableEditText mMessageFormatNormal;
    private TextView mMessageFormatNormalExample;
    private FormattableEditText mMessageFormatAction;
    private TextView mMessageFormatActionExample;
    private FormattableEditText mMessageFormatEvent;
    private TextView mMessageFormatEventExample;
    private TextFormatBar mTextFormatBar;

    private MessageSenderInfo mTestSender;
    private MessageInfo mSampleMessage;
    private MessageInfo mSampleActionMessage;
    private MessageInfo mSampleEventMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_format_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMessageBuilder = new MessageBuilder(this);

        mTextFormatBar = (TextFormatBar) findViewById(R.id.format_bar);
        mTextFormatBar.setVisibility(View.GONE);
        mTextFormatBar.setOnChangeListener((TextFormatBar bar, FormattableEditText text) -> {
            if (text == mMessageFormatNormal)
                mMessageBuilder.setMessageFormat(text.getText());
            else if (text == mMessageFormatAction)
                mMessageBuilder.setActionMessageFormat(text.getText());
            else if (text == mMessageFormatEvent)
                mMessageBuilder.setEventMessageFormat(text.getText());
            refreshExamples();
        });
        mTextFormatBar.setExtraButton(R.drawable.ic_add_circle_outline,
                getString(R.string.message_format_add_chip), (View v) -> {
                    PopupMenu menu = new PopupMenu(v.getContext(), v, GravityCompat.END);
                    MenuInflater inflater = menu.getMenuInflater();
                    inflater.inflate(R.menu.menu_format_add_chip, menu.getMenu());
                    menu.setOnMenuItemClickListener((MenuItem item) -> {
                        int id = item.getItemId();
                        if (id == R.id.message_format_time)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_TIME, " ");
                        else if (id == R.id.message_format_sender)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_SENDER, " ");
                        else if (id == R.id.message_format_message)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_MESSAGE, " ");
                        return false;
                    });
                    menu.show();
                });

        View.OnFocusChangeListener focusListener = (View v, boolean hasFocus) -> {
            mTextFormatBar.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        };

        mDateFormat = (EditText) findViewById(R.id.date_format);
        mDateFormatCtr = (TextInputLayout) findViewById(R.id.date_format_ctr);
        mDateFormat.setText(mMessageBuilder.getMessageTimeFormat().toPattern());
        mDateFormat.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            try {
                mMessageBuilder.setMessageTimeFormat(s.toString());
                mDateFormatCtr.setError(null);
                mDateFormatCtr.setErrorEnabled(false);
            } catch (Exception e) {
                mDateFormatCtr.setError(getString(R.string.message_format_date_invalid));
                return;
            }
            refreshExamples();
        }));

        mMessageFormatNormal = (FormattableEditText) findViewById(R.id.message_format_normal);
        mMessageFormatNormal.setText(mMessageBuilder.getMessageFormat());
        mMessageFormatNormal.setFormatBar(mTextFormatBar);
        mMessageFormatNormal.setOnFocusChangeListener(focusListener);
        mMessageFormatNormal.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatNormalExample = (TextView) findViewById(R.id.message_format_normal_example);

        mMessageFormatAction = (FormattableEditText) findViewById(R.id.message_format_action);
        mMessageFormatAction.setText(mMessageBuilder.getActionMessageFormat());
        mMessageFormatAction.setFormatBar(mTextFormatBar);
        mMessageFormatAction.setOnFocusChangeListener(focusListener);
        mMessageFormatAction.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setActionMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatActionExample = (TextView) findViewById(R.id.message_format_action_example);

        mMessageFormatEvent = (FormattableEditText) findViewById(R.id.message_format_event);
        mMessageFormatEvent.setText(mMessageBuilder.getEventMessageFormat());
        mMessageFormatEvent.setFormatBar(mTextFormatBar);
        mMessageFormatEvent.setOnFocusChangeListener(focusListener);
        mMessageFormatEvent.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setEventMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatEventExample = (TextView) findViewById(R.id.message_format_event_example);

        refreshExamples();
    }

    private void insertChip(int type, String str) {
        FormattableEditText editText = mTextFormatBar.getEditText();
        int sStart = editText.getSelectionStart();
        int sEnd = editText.getSelectionEnd();
        editText.getText().replace(sStart, sEnd, str);
        editText.getText().setSpan(new MessageBuilder.MetaChipSpan(this, type), sStart, sStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        refreshExamples();
    }

    public static Date getSampleMessageTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.AM_PM, Calendar.PM);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    private void refreshExamples() {
        if (mTestSender == null) {
            mTestSender = new MessageSenderInfo(getString(R.string.message_example_sender), "", "", null, null);
            Date date = getSampleMessageTime();
            mSampleMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
            mSampleActionMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.ME);
            mSampleEventMessage = new MessageInfo(mTestSender, date, null, MessageInfo.MessageType.JOIN);
        }

        mMessageFormatNormalExample.setText(mMessageBuilder.buildMessage(mSampleMessage));
        mMessageFormatActionExample.setText(mMessageBuilder.buildMessage(mSampleActionMessage));
        mMessageFormatEventExample.setText(mMessageBuilder.buildMessage(mSampleEventMessage));
    }

    public void save() {
        MessageBuilder global = MessageBuilder.getInstance(this);
        global.setMessageTimeFormat(mMessageBuilder.getMessageTimeFormat().toPattern());
        global.setMessageFormat(mMessageBuilder.getMessageFormat());
        global.setActionMessageFormat(mMessageBuilder.getActionMessageFormat());
        global.setEventMessageFormat(mMessageBuilder.getEventMessageFormat());
        global.saveFormats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_only_done, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done || id == android.R.id.home) {
            if (id == R.id.action_done) {
                save();
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
