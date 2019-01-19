package io.mrarm.irc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.TextInputLayout;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.ServerCertificateManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.ExpandIconStateHelper;
import io.mrarm.irc.util.PEMParser;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.view.AutoRunCommandListEditText;
import io.mrarm.irc.view.StaticLabelTextInputLayout;
import io.mrarm.irc.view.ChipsEditText;

public class EditServerActivity extends ThemedActivity {

    private static String TAG = "EditServerActivity";

    public static final String RESULT_ACTION = "io.mrarm.irc.EDIT_SERVER_RESULT_ACTION";

    private static final int REQUEST_SASL_EXT_CERT = 100;

    public static String ARG_SERVER_UUID = "server_uuid";
    public static String ARG_COPY = "copy";
    public static String ARG_NAME = "server_name";
    public static String ARG_ADDRESS = "server_address";
    public static String ARG_PORT = "server_port";
    public static String ARG_AUTOJOIN_CHANNELS = "server_autojoin_channels";

    private ServerConfigData mEditServer;
    private EditText mServerName;
    private TextInputLayout mServerNameCtr;
    private EditText mServerAddress;
    private TextInputLayout mServerAddressCtr;
    private EditText mServerPort;
    private TextInputLayout mServerPortCtr;
    private CheckBox mServerSSL;
    private View mServerSSLCertsButton;
    private TextView mServerSSLCertsLbl;
    private Spinner mServerAuthMode;
    private EditText mServerAuthUser;
    private TextInputLayout mServerAuthUserCtr;
    private EditText mServerAuthPass;
    private View mServerAuthSASLExt;
    private TextView mServerAuthSASLExtFP;
    private View mServerAuthSASLExtImportButton;
    private String mOldServerAuthPass;
    private StaticLabelTextInputLayout mServerAuthPassCtr;
    private View mServerAuthPassMainCtr;
    private View mServerAuthPassReset;
    private ChipsEditText mServerNick;
    private EditText mServerUser;
    private EditText mServerRealname;
    private ChipsEditText mServerChannels;
    private CheckBox mServerRejoinChannels;
    private AutoRunCommandListEditText mServerCommands;
    private Spinner mServerEncoding;

    private View mServerUserExpandIcon;
    private View mServerUserExpandContent;

    private String[] mServerEncodingValues;

    private X509Certificate mServerCert = null;
    private byte[] mServerPrivKey = null;
    private String mServerPrivKeyType;

    public static Intent getLaunchIntent(Context context, ServerConfigData data, boolean copy) {
        Intent intent = new Intent(context, EditServerActivity.class);
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, data.uuid.toString());
        if (copy)
            args.putBoolean(ARG_COPY, true);
        intent.putExtras(args);
        return intent;
    }

    public static Intent getLaunchIntent(Context context, ServerConfigData data) {
        return getLaunchIntent(context, data, false);
    }

    private TextWatcher mResetPasswordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                mServerAuthPassReset.setVisibility(View.GONE);
                mServerAuthPassCtr.setPasswordVisibilityToggleEnabled(true);
            } else {
                mServerAuthPassReset.setVisibility(View.VISIBLE);
                mServerAuthPassCtr.setPasswordVisibilityToggleEnabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uuidString = getIntent().getStringExtra(ARG_SERVER_UUID);
        if (uuidString != null)
            mEditServer = ServerConfigManager.getInstance(this).findServer(UUID.fromString(uuidString));

        setContentView(R.layout.activity_edit_server);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mServerName = findViewById(R.id.server_name);
        mServerNameCtr = findViewById(R.id.server_name_ctr);
        mServerAddress = findViewById(R.id.server_address_name);
        mServerAddressCtr = findViewById(R.id.server_address_name_ctr);
        mServerPort = findViewById(R.id.server_address_port);
        mServerPortCtr = findViewById(R.id.server_address_port_ctr);
        mServerSSL = findViewById(R.id.server_ssl_checkbox);
        mServerSSLCertsButton = findViewById(R.id.server_ssl_certs);
        mServerSSLCertsLbl = findViewById(R.id.server_ssl_cert_lbl);
        mServerAuthMode = findViewById(R.id.server_auth_mode);
        mServerAuthUser = findViewById(R.id.server_username);
        mServerAuthUserCtr = findViewById(R.id.server_username_ctr);
        mServerAuthPass = findViewById(R.id.server_password);
        mServerAuthPassCtr = findViewById(R.id.server_password_ctr);
        mServerAuthPassMainCtr = findViewById(R.id.server_password_main_ctr);
        mServerAuthPassReset = findViewById(R.id.server_password_reset);
        mServerAuthSASLExt = findViewById(R.id.server_sasl_ext_main_ctr);
        mServerAuthSASLExtFP = findViewById(R.id.server_sasl_ext_fp);
        mServerAuthSASLExtImportButton = findViewById(R.id.server_sasl_ext_import);
        mServerNick = findViewById(R.id.server_nick);
        mServerUser = findViewById(R.id.server_user);
        mServerRealname = findViewById(R.id.server_realname);
        mServerChannels = findViewById(R.id.server_channels);
        mServerRejoinChannels = findViewById(R.id.server_rejoin_channels);
        mServerCommands = findViewById(R.id.server_commands);
        mServerEncoding = findViewById(R.id.server_encoding);

        mServerUserExpandIcon = findViewById(R.id.server_user_expand);
        mServerUserExpandContent = findViewById(R.id.server_user_expand_content);
        mServerUserExpandIcon.setOnClickListener((View view) -> {
            mServerUserExpandContent.setVisibility(mServerUserExpandContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ExpandIconStateHelper.animateSetExpanded(mServerUserExpandIcon, mServerUserExpandContent.getVisibility() == View.VISIBLE);
        });

        mServerName.addTextChangedListener(new SimpleTextWatcher((Editable s) -> mServerNameCtr.setErrorEnabled(false)));
        mServerAddress.addTextChangedListener(new SimpleTextWatcher((Editable s) -> mServerAddressCtr.setErrorEnabled(false)));
        mServerPort.addTextChangedListener(new SimpleTextWatcher((Editable s) -> mServerPortCtr.setErrorEnabled(false)));

        mServerSSLCertsButton.setOnClickListener((View v) -> {
            Intent intent = new Intent(EditServerActivity.this, CertificateManagerActivity.class);
            intent.putExtra(CertificateManagerActivity.ARG_SERVER_UUID, mEditServer.uuid.toString());
            startActivity(intent);
        });

        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this,
                R.layout.simple_spinner_item, android.R.id.text1,
                getResources().getTextArray(R.array.server_auth_modes));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerAuthMode.setAdapter(spinnerAdapter);
        mServerAuthMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mServerAuthUserCtr.setVisibility(View.GONE);
                    mServerAuthPassMainCtr.setVisibility(View.GONE);
                    mServerAuthSASLExt.setVisibility(View.GONE);
                } else if (position == 1) {
                    mServerAuthUserCtr.setVisibility(View.GONE);
                    mServerAuthPassMainCtr.setVisibility(View.VISIBLE);
                    mServerAuthSASLExt.setVisibility(View.GONE);
                } else if (position == 2) {
                    mServerAuthUserCtr.setVisibility(View.VISIBLE);
                    mServerAuthPassMainCtr.setVisibility(View.VISIBLE);
                    mServerAuthSASLExt.setVisibility(View.GONE);
                } else if (position == 3) {
                    mServerAuthUserCtr.setVisibility(View.GONE);
                    mServerAuthPassMainCtr.setVisibility(View.GONE);
                    mServerAuthSASLExt.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mServerAuthSASLExtImportButton.setOnClickListener((View v) -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_SASL_EXT_CERT);
        });

        mServerEncodingValues =  getResources().getStringArray(R.array.encodings_values);
        ArrayAdapter<CharSequence> encodingAdapter = new ArrayAdapter<>(this,
                R.layout.simple_spinner_item, android.R.id.text1,
                getResources().getTextArray(R.array.encodings_display));
        encodingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mServerEncoding.setAdapter(encodingAdapter);

        mServerAuthPassReset.setOnClickListener((View view) -> {
            mServerAuthPassCtr.setForceShowHint(false);
            mServerAuthPassReset.setVisibility(View.GONE);
            mServerAuthPassCtr.setPasswordVisibilityToggleEnabled(true);
            mServerAuthPass.removeTextChangedListener(mResetPasswordWatcher);
        });

        mServerPort.setText(String.valueOf(getDefaultPort(mServerSSL.isEnabled())));
        mServerSSL.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (String.valueOf(getDefaultPort(!isChecked)).equals(mServerPort.getText().toString()))
                mServerPort.setText(String.valueOf(getDefaultPort(isChecked)));
        });

        if (mEditServer != null) {
            mServerName.setText(mEditServer.name);
            mServerAddress.setText(mEditServer.address);
            mServerSSL.setChecked(mEditServer.ssl);
            mServerPort.setText(String.valueOf(mEditServer.port));
            mServerRejoinChannels.setChecked(mEditServer.rejoinChannels);

            if (mEditServer.authPass != null) {
                mServerAuthPassReset.setVisibility(View.VISIBLE);
                mServerAuthPassCtr.setForceShowHint(true);
                mServerAuthPass.setHint(R.string.server_password_unchanged);
                mServerAuthPass.addTextChangedListener(mResetPasswordWatcher);
                mServerAuthPassCtr.setPasswordVisibilityToggleEnabled(false);
                mServerAuthUser.setText(mEditServer.authUser);
                mOldServerAuthPass = mEditServer.authPass;
            }
            mServerCert = mEditServer.getAuthCert();
            if (mServerCert != null) {
                mServerAuthSASLExtFP.setText(getString(R.string.server_sasl_ext_fp,
                        getCertificateFingerprint(mServerCert)));
            }
            mServerPrivKey = mEditServer.authCertPrivKey;
            mServerPrivKeyType = mEditServer.authCertPrivKeyType;
            if (mEditServer.authMode != null) {
                switch (mEditServer.authMode) {
                    case ServerConfigData.AUTH_PASSWORD:
                        mServerAuthMode.setSelection(1);
                        break;
                    case ServerConfigData.AUTH_SASL:
                        mServerAuthMode.setSelection(2);
                        break;
                    case ServerConfigData.AUTH_SASL_EXTERNAL:
                        mServerAuthMode.setSelection(3);
                        break;
                    default:
                        mServerAuthMode.setSelection(0);
                        break;
                }
            }

            if (mEditServer.autojoinChannels != null)
                mServerChannels.setItems(mEditServer.autojoinChannels);

            if (mEditServer.nicks != null)
                mServerNick.setItems(mEditServer.nicks);
            if (mEditServer.user != null || mEditServer.realname != null) {
                mServerUser.setText(mEditServer.user);
                mServerRealname.setText(mEditServer.realname);
                mServerUserExpandContent.setVisibility(View.VISIBLE);
                ExpandIconStateHelper.setExpanded(mServerUserExpandIcon, true);
            }
            if (mEditServer.execCommandsConnected != null
                    && mEditServer.execCommandsConnected.size() > 0) {
                StringBuilder b = new StringBuilder();
                for (String cmd : mEditServer.execCommandsConnected) {
                    if (b.length() > 0)
                        b.append('\n');
                    b.append(cmd);
                }
                mServerCommands.setText(b.toString());
            }
            mServerEncoding.setSelection(0);
            for (int i = 0; i < mServerEncodingValues.length; i++) {
                if (mServerEncodingValues[i].equals(mEditServer.charset))
                    mServerEncoding.setSelection(i);
            }
            if (getIntent().getBooleanExtra(ARG_COPY, false)) {
                mEditServer = null;
                getSupportActionBar().setTitle(R.string.add_server);
                mServerSSLCertsButton.setVisibility(View.GONE);
            }
        } else {
            getSupportActionBar().setTitle(R.string.add_server);
            mServerSSLCertsButton.setVisibility(View.GONE);

            String prefillName = getIntent().getStringExtra(ARG_NAME);
            if (prefillName != null)
                mServerName.setText(prefillName);
            String prefillAddress = getIntent().getStringExtra(ARG_ADDRESS);
            if (prefillAddress != null)
                mServerAddress.setText(prefillAddress);
            int prefillPort = getIntent().getIntExtra(ARG_PORT, -1);
            if (prefillPort != -1)
                mServerPort.setText(String.valueOf(prefillPort));
            ArrayList<String> prefillAutojoinChannels =
                    getIntent().getStringArrayListExtra(ARG_AUTOJOIN_CHANNELS);
            if (prefillAutojoinChannels != null)
                mServerChannels.setItems(prefillAutojoinChannels);
        }
    }

    private boolean validate() {
        boolean succeeded = true;

        String newServerName = mServerName.getText().toString();
        if (mServerName.getText().length() == 0) {
            mServerNameCtr.setError(getString(R.string.server_error_name_empty));
            mServerName.requestFocus();
            succeeded = false;
        }
        for (ServerConfigData data : ServerConfigManager.getInstance(this).getServers()) {
            if (data != mEditServer && data.name.equals(newServerName)) {
                mServerNameCtr.setError(getString(R.string.server_error_name_collision));
                mServerName.requestFocus();
                succeeded = false;
            }
        }
        if (mServerAddress.getText().length() == 0) {
            mServerAddressCtr.setError(getString(R.string.server_error_invalid_address));
            if (succeeded)
                mServerAddress.requestFocus();
            succeeded = false;
        }
        try {
            Integer.parseInt(mServerPort.getText().toString());
        } catch (NumberFormatException e) {
            mServerPortCtr.setError(getString(R.string.server_error_invalid_port));
            if (succeeded)
                mServerPort.requestFocus();
            succeeded = false;
        }

        return succeeded;
    }

    private boolean save() {
        if (!validate())
            return false;
        boolean addConnection = false;
        if (mEditServer == null) {
            mEditServer = new ServerConfigData();
            mEditServer.uuid = UUID.randomUUID();
            mEditServer.authPass = mOldServerAuthPass;
        } else {
            ServerConnectionInfo conn = ServerConnectionManager.getInstance(this).getConnection(mEditServer.uuid);
            if (conn != null) {
                conn.disconnect();
                ServerConnectionManager.getInstance(this).removeConnection(conn);
                addConnection = true;
            }
        }
        mEditServer.name = mServerName.getText().toString();
        mEditServer.address = mServerAddress.getText().toString();
        mEditServer.port = Integer.parseInt(mServerPort.getText().toString());
        mEditServer.ssl = mServerSSL.isChecked();
        mEditServer.nicks = Arrays.asList(mServerNick.getItems());
        if (mEditServer.nicks.size() == 0)
            mEditServer.nicks = null;
        mEditServer.user = mServerUser.getText().length() > 0 ? mServerUser.getText().toString() : null;
        mEditServer.realname = mServerRealname.getText().length() > 0 ? mServerRealname.getText().toString() : null;
        int authModeInt = mServerAuthMode.getSelectedItemPosition();
        boolean authModePassword = false;
        mEditServer.authCertData = null;
        mEditServer.authCertPrivKey = null;
        mEditServer.authCertPrivKeyType = null;
        if (authModeInt == 1) {
            mEditServer.authMode = ServerConfigData.AUTH_PASSWORD;
            mEditServer.authUser = null;
            authModePassword = true;
        } else if (authModeInt == 2) {
            mEditServer.authMode = ServerConfigData.AUTH_SASL;
            mEditServer.authUser = mServerAuthUser.getText().toString();
            authModePassword = true;
        } else if (authModeInt == 3) {
            mEditServer.authMode = ServerConfigData.AUTH_SASL_EXTERNAL;
            mEditServer.authUser = null;
            mEditServer.authPass = null;
            try {
                mEditServer.authCertData = mServerCert.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }
            mEditServer.authCertPrivKey = mServerPrivKey;
            mEditServer.authCertPrivKeyType = mServerPrivKeyType;
        } else {
            mEditServer.authMode = null;
            mEditServer.authUser = null;
            mEditServer.authPass = null;
        }
        if (mServerAuthPassReset.getVisibility() == View.GONE && authModePassword)
            mEditServer.authPass = mServerAuthPass.getText().toString();
        mEditServer.autojoinChannels = Arrays.asList(mServerChannels.getItems());
        mEditServer.rejoinChannels = mServerRejoinChannels.isChecked();
        mEditServer.execCommandsConnected = mServerCommands.getText().length() > 0
                ? Arrays.asList(mServerCommands.getTextWithPasswords().split("\n")) : null;
        mEditServer.charset = mServerEncodingValues[mServerEncoding.getSelectedItemPosition()];
        try {
            ServerConfigManager.getInstance(this).saveServer(mEditServer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save server info");
            e.printStackTrace();

            Toast.makeText(this, R.string.server_save_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (addConnection) {
            ServerConnectionManager.getInstance(this).tryCreateConnection(mEditServer, this);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEditServer != null) {
            List<String> aliases = ServerCertificateManager.get(this, mEditServer.uuid).getCertificateAliases();
            int count = aliases != null ? aliases.size() : 0;
            mServerSSLCertsLbl.setText(getResources().getQuantityString(R.plurals.server_manage_custom_certs_text, count, count));
        }
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
                mServerNick.clearFocus();
                mServerChannels.clearFocus();
                if (!save())
                    return true;

                Intent result = new Intent(RESULT_ACTION);
                result.putExtra(ARG_SERVER_UUID, mEditServer.uuid.toString());
                setResult(Activity.RESULT_OK, result);
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SASL_EXT_CERT && data != null && data.getData() != null) {
            Uri uri = data.getData();
            X509Certificate cert = null;
            PrivateKey privKey = null;
            int errorResId = -1;
            try {
                ParcelFileDescriptor desc = getContentResolver().openFileDescriptor(uri, "r");
                if (desc == null)
                    throw new IOException();
                BufferedReader reader = new BufferedReader(
                        new FileReader(desc.getFileDescriptor()));
                List<Object> objects = PEMParser.parse(reader);
                for (Object o : objects) {
                    if (o instanceof X509Certificate) {
                        if (cert != null) {
                            errorResId = R.string.error_cert_already_present;
                            break;
                        }
                        cert = (X509Certificate) o;
                    }
                    if (o instanceof PrivateKey) {
                        if (privKey != null) {
                            errorResId = R.string.error_privkey_already_present;
                            break;
                        }
                        privKey = (PrivateKey) o;
                    }
                }
                if (privKey == null || cert == null)
                    errorResId = R.string.error_cert_or_privkey_missing;
            } catch (IOException e) {
                errorResId = R.string.error_file_open;
                e.printStackTrace();
            }
            if (errorResId == -1) {
                mServerCert = cert;
                mServerPrivKey = privKey.getEncoded();
                mServerPrivKeyType = privKey.getAlgorithm();
                mServerAuthSASLExtFP.setText(getString(R.string.server_sasl_ext_fp,
                        getCertificateFingerprint(mServerCert)));
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.error_generic)
                        .setMessage(errorResId)
                        .show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static int getDefaultPort(boolean sslEnabled) {
        return sslEnabled ? 6697 : 6667;
    }


    private static String getCertificateFingerprint(X509Certificate cert) {
        try {
            StringBuilder builder = new StringBuilder();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(cert.getEncoded());
            for (byte b : bytes)
                builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
