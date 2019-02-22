package io.mrarm.irc.config;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import io.mrarm.irc.R;
import io.mrarm.irc.util.SimpleWildcardPattern;

public class ServerConfigData {

    private static final String AUTH_LEGACY_PASSWORD = "password";
    public static final String AUTH_SASL = "sasl";
    public static final String AUTH_SASL_EXTERNAL = "sasl_external";

    public String name;
    public UUID uuid;

    public String address;
    public int port;
    public boolean ssl;
    public String charset;
    public String pass;
    public String authMode;
    public String authUser;
    public String authPass;
    public byte[] authCertData;
    public byte[] authCertPrivKey;
    public String authCertPrivKeyType;

    public List<String> nicks;
    public String user;
    public String realname;

    public List<String> autojoinChannels;
    public boolean rejoinChannels = true;
    public List<String> execCommandsConnected;

    public List<IgnoreEntry> ignoreList;

    public long storageLimit;

    public void migrateLegacyProperties() {
        if (authMode != null && authMode.equals(AUTH_LEGACY_PASSWORD)) {
            pass = authPass;
            authPass = null;
            authMode = null;
        }
    }

    public X509Certificate getAuthCert() {
        if (authCertData == null)
            return null;
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(authCertData));
        } catch (CertificateException e) {
            Log.e("ServerConfigData", "Failed to load cert data");
            e.printStackTrace();
        }
        return null;
    }

    public PrivateKey getAuthPrivateKey() {
        if (authCertPrivKey == null)
            return null;
        try {
            KeyFactory factory = KeyFactory.getInstance(authCertPrivKeyType);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(authCertPrivKey);
            return factory.generatePrivate(keySpec);
        } catch (GeneralSecurityException e) {
            Log.w("ServerConfigData", "Failed to load private key");
            e.printStackTrace();
        }
        return null;
    }

    public static class IgnoreEntry {

        public String nick;
        public String user;
        public String host;
        public String comment;
        public transient Pattern nickRegex;
        public transient Pattern userRegex;
        public transient Pattern hostRegex;

        public boolean matchDirectMessages = true;
        public boolean matchDirectNotices = true;
        public boolean matchChannelMessages = true;
        public boolean matchChannelNotices = true;

        public void updateRegexes() {
            nickRegex = SimpleWildcardPattern.compile(nick);
            userRegex = SimpleWildcardPattern.compile(user);
            hostRegex = SimpleWildcardPattern.compile(host);
        }

    }

}
