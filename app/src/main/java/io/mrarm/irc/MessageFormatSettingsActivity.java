package io.mrarm.irc;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.StubTextWatcher;

public class MessageFormatSettingsActivity extends AppCompatActivity {

    private EditText mDateFormat;
    private TextInputLayout mDateFormatCtr;
    private EditText mMessageFormatNormal;
    private TextView mMessageFormatNormalExample;
    private EditText mMessageFormatAction;
    private TextView mMessageFormatActionExample;
    private EditText mMessageFormatEvent;
    private TextView mMessageFormatEventExample;

    private MessageSenderInfo mTestSender;
    private MessageInfo mSampleMessage;
    private MessageInfo mSampleActionMessage;
    private MessageInfo mSampleEventMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_format_settings);

        MessageBuilder builder = MessageBuilder.getInstance(this);

        mDateFormat = (EditText) findViewById(R.id.date_format);
        mDateFormatCtr = (TextInputLayout) findViewById(R.id.date_format_ctr);
        mDateFormat.setText(builder.getMessageTimeFormat().toPattern());
        mDateFormat.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            try {
                builder.setMessageTimeFormat(s.toString());
                mDateFormatCtr.setError(null);
                mDateFormatCtr.setErrorEnabled(false);
            } catch (Exception e) {
                mDateFormatCtr.setError(getString(R.string.message_format_date_invalid));
                return;
            }
            refreshExamples();
        }));

        mMessageFormatNormal = (EditText) findViewById(R.id.message_format_normal);
        mMessageFormatNormal.setText(builder.getMessageFormat());
        mMessageFormatNormal.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            builder.setMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatNormalExample = (TextView) findViewById(R.id.message_format_normal_example);

        mMessageFormatAction = (EditText) findViewById(R.id.message_format_action);
        mMessageFormatAction.setText(builder.getActionMessageFormat());
        mMessageFormatAction.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            builder.setActionMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatActionExample = (TextView) findViewById(R.id.message_format_action_example);

        mMessageFormatEvent = (EditText) findViewById(R.id.message_format_event);
        mMessageFormatEvent.setText(builder.getEventMessageFormat());
        mMessageFormatEvent.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            builder.setEventMessageFormat(s);
            refreshExamples();
        }));

        mMessageFormatEventExample = (TextView) findViewById(R.id.message_format_event_example);

        refreshExamples();
    }

    private void refreshExamples() {
        if (mTestSender == null) {
            mTestSender = new MessageSenderInfo(getString(R.string.message_example_sender), "", "", null, null);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.AM_PM, Calendar.PM);
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date date = calendar.getTime();
            mSampleMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
            mSampleActionMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.ME);
            mSampleEventMessage = new MessageInfo(mTestSender, date, null, MessageInfo.MessageType.JOIN);
        }

        MessageBuilder builder = MessageBuilder.getInstance(this);
        mMessageFormatNormalExample.setText(builder.buildMessage(mSampleMessage));
        mMessageFormatActionExample.setText(builder.buildMessage(mSampleActionMessage));
        mMessageFormatEventExample.setText(builder.buildMessage(mSampleEventMessage));
    }

}
