package io.mrarm.irc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.view.GravityCompat;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.PopupMenu;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.FormattableEditText;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.view.TextFormatBar;

public class MessageFormatSettingsActivity extends ThemedActivity {

    private MessageBuilder mMessageBuilder;

    private EditText mDateFormat;
    private TextInputLayout mDateFormatCtr;
    private View mDateFormatPresetButton;
    private CheckBox mDateFixedWidth;
    private FormattableEditText mMessageFormatNormal;
    private TextView mMessageFormatNormalExample;
    private FormattableEditText mMessageFormatNormalMention;
    private TextView mMessageFormatNormalMentionExample;
    private FormattableEditText mMessageFormatAction;
    private TextView mMessageFormatActionExample;
    private FormattableEditText mMessageFormatActionMention;
    private TextView mMessageFormatActionMentionExample;
    private FormattableEditText mMessageFormatNotice;
    private TextView mMessageFormatNoticeExample;
    private FormattableEditText mMessageFormatEvent;
    private TextView mMessageFormatEventExample;
    private CheckBox mMessageFormatEventHostname;
    private TextFormatBar mTextFormatBar;

    private MessageSenderInfo mTestSender;
    private MessageInfo mSampleMessage;
    private MessageInfo mSampleActionMessage;
    private MessageInfo mSampleNoticeMessage;
    private MessageInfo mSampleEventMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_format_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMessageBuilder = new MessageBuilder(this);

        mTextFormatBar = findViewById(R.id.format_bar);
        mTextFormatBar.setVisibility(View.GONE);
        mTextFormatBar.setOnChangeListener((TextFormatBar bar, EditText text) -> {
            ((SimpleTextWatcher.OnTextChangedListener) text.getTag()).afterTextChanged(text.getText());
        });
        mTextFormatBar.setExtraButton(R.drawable.ic_add_circle_outline,
                getString(R.string.message_format_add_chip), (View v) -> {
                    PopupMenu menu = new PopupMenu(v.getContext(), v, GravityCompat.END);
                    MenuInflater inflater = menu.getMenuInflater();
                    inflater.inflate(R.menu.menu_format_add_chip, menu.getMenu());
                    menu.setOnMenuItemClickListener((MenuItem item) -> {
                        int id = item.getItemId();
                        if (id == R.id.message_format_time)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_TIME);
                        else if (id == R.id.message_format_sender)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_SENDER);
                        else if (id == R.id.message_format_message)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_MESSAGE);
                        else if (id == R.id.message_format_wrap_anchor)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR);
                        else if (id == R.id.message_format_sender_prefix)
                            insertChip(MessageBuilder.MetaChipSpan.TYPE_SENDER_PREFIX);
                        return false;
                    });
                    menu.show();
                });


        mDateFormat = findViewById(R.id.date_format);
        mDateFormatCtr = findViewById(R.id.date_format_ctr);
        mDateFormat.setText(mMessageBuilder.getMessageTimeFormat().toPattern());
        mDateFormat.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
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

            int secondaryColor = StyledAttributesHelper.getColor(this, android.R.attr.textColorSecondary, Color.BLACK);

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

        mDateFixedWidth = findViewById(R.id.date_fixed_width);
        mDateFixedWidth.setChecked(mMessageBuilder.isMessageTimeFixedWidth());
        mDateFixedWidth.setOnCheckedChangeListener((CompoundButton btn, boolean checked) -> {
            mMessageBuilder.setMessageTimeFixedWidth(checked);
            refreshExamples();
        });

        mMessageFormatNormal = findViewById(R.id.message_format_normal);
        mMessageFormatNormalExample = findViewById(R.id.message_format_normal_example);
        mMessageFormatNormal.setText(mMessageBuilder.getMessageFormat());
        setupFormatEntry(mMessageFormatNormal, R.id.message_format_normal_preset, (Editable s) -> {
            mMessageBuilder.setMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildPresetMessageFormat(this, 0, false, false),
                buildPresetMessageFormat(this, 0, false, true),
                buildPresetMessageFormat(this, 1, false, true)
        });

        mMessageFormatNormalMention = findViewById(R.id.message_format_normal_mention);
        mMessageFormatNormalMentionExample = findViewById(R.id.message_format_normal_mention_example);
        mMessageFormatNormalMention.setText(mMessageBuilder.getMentionMessageFormat());
        setupFormatEntry(mMessageFormatNormalMention, R.id.message_format_normal_mention_preset, (Editable s) -> {
            mMessageBuilder.setMentionMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildPresetMessageFormat(this, 0, true, false),
                buildPresetMessageFormat(this, 0, true, true),
                buildPresetMessageFormat(this, 1, true, true)
        });

        mMessageFormatAction = findViewById(R.id.message_format_action);
        mMessageFormatActionExample = findViewById(R.id.message_format_action_example);
        mMessageFormatAction.setText(mMessageBuilder.getActionMessageFormat());
        setupFormatEntry(mMessageFormatAction, R.id.message_format_action_preset, (Editable s) -> {
            mMessageBuilder.setActionMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildActionPresetMessageFormat(this, 0, false),
                buildActionPresetMessageFormat(this, 1, false)
        });

        mMessageFormatActionMention = findViewById(R.id.message_format_action_mention);
        mMessageFormatActionMentionExample = findViewById(R.id.message_format_action_mention_example);
        mMessageFormatActionMention.setText(mMessageBuilder.getActionMentionMessageFormat());
        setupFormatEntry(mMessageFormatActionMention, R.id.message_format_action_mention_preset, (Editable s) -> {
            mMessageBuilder.setActionMentionMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildActionPresetMessageFormat(this, 0, true),
                buildActionPresetMessageFormat(this, 1, true)
        });

        mMessageFormatNotice = findViewById(R.id.message_format_notice);
        mMessageFormatNoticeExample = findViewById(R.id.message_format_notice_example);
        mMessageFormatNotice.setText(mMessageBuilder.getNoticeMessageFormat());
        setupFormatEntry(mMessageFormatNotice, R.id.message_format_notice_preset, (Editable s) -> {
            mMessageBuilder.setNoticeMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildNoticePresetMessageFormat(this, 0),
                buildNoticePresetMessageFormat(this, 1)
        });

        mMessageFormatEvent = findViewById(R.id.message_format_event);
        mMessageFormatEventExample = findViewById(R.id.message_format_event_example);
        mMessageFormatEventHostname = findViewById(R.id.message_format_event_hostname);
        mMessageFormatEvent.setText(mMessageBuilder.getEventMessageFormat());
        mMessageFormatEventHostname.setChecked(mMessageBuilder.getEventMessageShowHostname());
        setupFormatEntry(mMessageFormatEvent, R.id.message_format_event_preset, (Editable s) -> {
            mMessageBuilder.setEventMessageFormat(prepareFormat(s));
            refreshExamples();
        }, new CharSequence[] {
                buildEventPresetMessageFormat(this, 0),
                buildEventPresetMessageFormat(this, 1)
        });
        mMessageFormatEventHostname.setOnCheckedChangeListener((CompoundButton b, boolean ch) -> {
            mMessageBuilder.setEventMessageShowHostname(ch);
            refreshExamples();
        });

        refreshExamples();
    }

    private void refreshExamples() {
        if (mTestSender == null) {
            mTestSender = new MessageSenderInfo(getString(R.string.message_example_sender), "user", "test/host", null, null);
            Date date = getSampleMessageTime();
            mSampleMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
            mSampleActionMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.ME);
            mSampleNoticeMessage = new MessageInfo(mTestSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.NOTICE);
            mSampleEventMessage = new MessageInfo(mTestSender, date, null, MessageInfo.MessageType.JOIN);
        }

        mMessageFormatNormalExample.setText(mMessageBuilder.buildMessage(mSampleMessage));
        mMessageFormatNormalMentionExample.setText(mMessageBuilder.buildMessageWithMention(mSampleMessage));
        mMessageFormatActionExample.setText(mMessageBuilder.buildMessage(mSampleActionMessage));
        mMessageFormatActionMentionExample.setText(mMessageBuilder.buildMessageWithMention(mSampleActionMessage));
        mMessageFormatNoticeExample.setText(mMessageBuilder.buildMessage(mSampleNoticeMessage));
        mMessageFormatEventExample.setText(mMessageBuilder.buildMessage(mSampleEventMessage));
    }

    public void save() {
        MessageBuilder global = MessageBuilder.getInstance(this);
        global.setMessageTimeFormat(mMessageBuilder.getMessageTimeFormat().toPattern());
        global.setMessageTimeFixedWidth(mMessageBuilder.isMessageTimeFixedWidth());
        global.setMessageFormat(mMessageBuilder.getMessageFormat());
        global.setMentionMessageFormat(mMessageBuilder.getMentionMessageFormat());
        global.setActionMessageFormat(mMessageBuilder.getActionMessageFormat());
        global.setActionMentionMessageFormat(mMessageBuilder.getActionMentionMessageFormat());
        global.setNoticeMessageFormat(mMessageBuilder.getNoticeMessageFormat());
        global.setEventMessageFormat(mMessageBuilder.getEventMessageFormat());
        global.setEventMessageShowHostname(mMessageBuilder.getEventMessageShowHostname());
        global.saveFormats();
    }

    private void setupFormatEntry(FormattableEditText editText, int presetButtonId, SimpleTextWatcher.OnTextChangedListener textListener, CharSequence[] presets) {
        editText.setFormatBar(mTextFormatBar);
        editText.setOnFocusChangeListener(mEditTextFocusListener);
        editText.addTextChangedListener(new SimpleTextWatcher(textListener));
        editText.setTag(textListener);

        findViewById(presetButtonId).setOnClickListener((View v) -> {
            selectPreset(editText, presets);
        });
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

    private void selectPreset(EditText editText, CharSequence[] presets) {
        ListPopupWindow menu = new ListPopupWindow(editText.getContext());
        menu.setAnchorView(editText);
        menu.setAdapter(new ArrayAdapter<>(this,
                R.layout.activity_message_format_settings_preset, presets));
        menu.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            editText.setText(presets[position]);
            menu.dismiss();
        });
        menu.show();
    }

    public static SpannableString buildPresetMessageFormat(Context context, int preset, boolean mention, boolean prefix) {
        if (preset == 0) {
            SpannableString spannable = new SpannableString("\0 |" + (prefix ? "\0" : "") + "\0: \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 3, prefix ? 6 : 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (prefix)
                spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER_PREFIX), 3, 4, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), prefix ? 4 : 3, prefix ? 5 : 4, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE), prefix ? 7 : 6, prefix ? 8 : 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (mention) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), prefix ? 4 : 3, prefix ? 5 : 4, MessageBuilder.FORMAT_SPAN_FLAGS);
                spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), prefix ? 7 : 6, prefix ? 8 : 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            }
            return spannable;
        }
        if (preset == 1) {
            SpannableString spannable = new SpannableString("\0 |<" + (prefix ? "\0" : "") + "\0> \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 3, 4, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 4, prefix ? 6 : 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), prefix ? 6 : 5, prefix ? 7 : 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (prefix)
                spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER_PREFIX), 4, 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), prefix ? 5 : 4, prefix ? 6 : 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE), prefix ? 8 : 7, prefix ? 9 : 8, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (mention) {
                spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), prefix ? 8 : 7, prefix ? 9 : 8, MessageBuilder.FORMAT_SPAN_FLAGS);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 4, prefix ? 6 : 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            }
            return spannable;
        }
        return null;
    }

    public static SpannableString buildActionPresetMessageFormat(Context context, int preset, boolean mention) {
        if (preset == 0 || preset == 1) {
            SpannableString spannable = new SpannableString("\0 |* \0 \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (preset == 0)
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), 3, 8, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), 5, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE),  7, 8, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (mention) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 5, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
                spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER),  3, 8, MessageBuilder.FORMAT_SPAN_FLAGS);
            } else {
                spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_STATUS), 3, 4, MessageBuilder.FORMAT_SPAN_FLAGS);
                spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 5, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            }
            return spannable;
        }
        return null;
    }

    public static SpannableString buildNoticePresetMessageFormat(Context context, int preset) {
        if (preset == 0) {
            SpannableString spannable = new SpannableString("\0 |\0: \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 3, 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), 3, 4, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE), 6, 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 3, 7, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            return spannable;
        }
        if (preset == 1) {
            SpannableString spannable = new SpannableString("\0 |-\0- \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_SENDER), 3, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_SENDER), 4, 5, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE),  7, 8, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            return spannable;
        }
        return null;
    }

    public static SpannableString buildEventPresetMessageFormat(Context context, int preset) {
        if (preset == 0 || preset == 1) {
            SpannableString spannable = new SpannableString("\0 |* \0");
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_TIMESTAMP), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            if (preset == 0)
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), 3, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaForegroundColorSpan(context, MessageBuilder.MetaForegroundColorSpan.COLOR_STATUS), 3, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_TIME), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_MESSAGE),  5, 6, MessageBuilder.FORMAT_SPAN_FLAGS);
            spannable.setSpan(new MessageBuilder.MetaChipSpan(context, MessageBuilder.MetaChipSpan.TYPE_WRAP_ANCHOR), 2, 3, MessageBuilder.FORMAT_SPAN_FLAGS);
            return spannable;
        }
        return null;
    }

    private void insertChip(int type) {
        EditText editText = mTextFormatBar.getEditText();
        int sStart = editText.getSelectionStart();
        int sEnd = editText.getSelectionEnd();
        SpannableString ss = new SpannableString("\0");
        ss.setSpan(new MessageBuilder.MetaChipSpan(this, type), 0, 1, MessageBuilder.FORMAT_SPAN_FLAGS);
        editText.getText().replace(sStart, sEnd, ss);
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


    private final View.OnFocusChangeListener mEditTextFocusListener = (View v, boolean hasFocus) -> {
        mTextFormatBar.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
    };

}
