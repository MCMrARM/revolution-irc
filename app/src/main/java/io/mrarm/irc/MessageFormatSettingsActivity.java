package io.mrarm.irc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
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
    private View mDateFormatPresetButton;
    private FormattableEditText mMessageFormatNormal;
    private View mMessageFormatPresetButton;
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
                mMessageBuilder.setMessageFormat(prepareFormat(text.getText()));
            else if (text == mMessageFormatAction)
                mMessageBuilder.setActionMessageFormat(prepareFormat(text.getText()));
            else if (text == mMessageFormatEvent)
                mMessageBuilder.setEventMessageFormat(prepareFormat(text.getText()));
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
        mDateFormatPresetButton = findViewById(R.id.date_format_preset);
        mDateFormatPresetButton.setOnClickListener((View v) -> {
            PopupMenu menu = new PopupMenu(v.getContext(), mDateFormat, GravityCompat.START);
            String[] presets = getResources().getStringArray(R.array.time_format_presets);
            String[] presetsText = getResources().getStringArray(R.array.time_format_presets_desc);

            TypedArray ta = getTheme().obtainStyledAttributes(R.style.AppTheme,
                    new int[] { android.R.attr.textColorSecondary });
            int secondaryColor = ta.getColor(0, Color.BLACK);
            ta.recycle();

            for (int i = 0; i < presets.length; i++) {
                SpannableString s = new SpannableString(presets[i] + " " + presetsText[i]);
                s.setSpan(new ForegroundColorSpan(secondaryColor), presets[i].length() + 1, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                menu.getMenu().add(s).setTitleCondensed(presets[i]);
            }
            menu.setOnMenuItemClickListener((MenuItem item) -> {
                mDateFormat.setText(item.getTitleCondensed());
                return false;
            });
            menu.show();
        });

        mMessageFormatNormal = (FormattableEditText) findViewById(R.id.message_format_normal);
        mMessageFormatNormal.setText(mMessageBuilder.getMessageFormat());
        mMessageFormatNormal.setFormatBar(mTextFormatBar);
        mMessageFormatNormal.setOnFocusChangeListener(focusListener);
        mMessageFormatNormal.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setMessageFormat(prepareFormat((Spannable) s));
            refreshExamples();
        }));

        mMessageFormatPresetButton = findViewById(R.id.message_format_preset);
        mMessageFormatPresetButton.setOnClickListener((View v) -> {
            PopupMenu menu = new PopupMenu(v.getContext(), mMessageFormatNormal, GravityCompat.START);
            for (int i = 0; i < 2; i++)
                menu.getMenu().add(Menu.NONE, i, Menu.NONE, buildPresetMessageFormat(MessageFormatSettingsActivity.this, i));
            menu.setOnMenuItemClickListener((MenuItem item) -> {
                mMessageFormatNormal.setText(buildPresetMessageFormat(MessageFormatSettingsActivity.this, item.getItemId()));
                return false;
            });
            menu.show();
        });

        mMessageFormatNormalExample = (TextView) findViewById(R.id.message_format_normal_example);

        mMessageFormatAction = (FormattableEditText) findViewById(R.id.message_format_action);
        mMessageFormatAction.setText(mMessageBuilder.getActionMessageFormat());
        mMessageFormatAction.setFormatBar(mTextFormatBar);
        mMessageFormatAction.setOnFocusChangeListener(focusListener);
        mMessageFormatAction.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setActionMessageFormat(prepareFormat((Spannable) s));
            refreshExamples();
        }));

        mMessageFormatActionExample = (TextView) findViewById(R.id.message_format_action_example);

        mMessageFormatEvent = (FormattableEditText) findViewById(R.id.message_format_event);
        mMessageFormatEvent.setText(mMessageBuilder.getEventMessageFormat());
        mMessageFormatEvent.setFormatBar(mTextFormatBar);
        mMessageFormatEvent.setOnFocusChangeListener(focusListener);
        mMessageFormatEvent.addTextChangedListener(new SimpleTextWatcher((CharSequence s, int start, int before, int count) -> {
            mMessageBuilder.setEventMessageFormat(prepareFormat((Spannable) s));
            refreshExamples();
        }));

        mMessageFormatEventExample = (TextView) findViewById(R.id.message_format_event_example);

        refreshExamples();
    }

    private CharSequence prepareFormat(Spannable s) {
        SpannableString ret = new SpannableString(s.toString());
        for (Object span : s.getSpans(0, s.length(), CharacterStyle.class)) {
            if ((ret.getSpanFlags(span) & Spannable.SPAN_COMPOSING) != 0)
                continue;
            ret.setSpan(span, s.getSpanStart(span), s.getSpanEnd(span), s.getSpanFlags(span) & Spanned.SPAN_PRIORITY);
        }
        return ret;
    }

    public static SpannableString buildPresetMessageFormat(Context context, int preset) {
        if (preset == 0)
            return MessageBuilder.buildDefaultMessageFormat(context);
        if (preset == 1) {
            SpannableString spannable = new SpannableString("  < >  ");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 2, 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), 3, 4, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE), 6, 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            return spannable;
        }
        return null;
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
