package io.mrarm.irc.config;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.mrarm.irc.util.ColoredTextBuilder;

public class ServerCertificateManager {

    private static final String TAG = "CertificateManager";

    private static final Map<String, WeakReference<ServerCertificateManager>> mInstances = new HashMap<>();

    public static ServerCertificateManager get(File file) {
        synchronized (mInstances) {
            WeakReference<ServerCertificateManager> instance = mInstances.get(file.getAbsolutePath());
            if (instance != null) {
                ServerCertificateManager helper = instance.get();
                if (helper != null)
                    return helper;
            }
            ServerCertificateManager ret = new ServerCertificateManager(file);
            mInstances.put(file.getAbsolutePath(), new WeakReference<>(ret));
            return ret;
        }
    }

    public static ServerCertificateManager get(Context context, UUID serverUUID) {
        return get(ServerConfigManager.getInstance(context).getServerSSLCertsFile(serverUUID));
    }

    private File mKeyStoreFile;
    private KeyStore mKeyStore;
    private X509TrustManager mKeyStoreTrustManager;

    private ServerCertificateManager(File keyStoreFile) {
        mKeyStoreFile = keyStoreFile;
        if (keyStoreFile != null && keyStoreFile.exists()) {
            try {
                loadKeyStore(new FileInputStream(mKeyStoreFile));
            } catch (Exception e) {
                Log.w(TAG, "Failed to load keystore");
                mKeyStore = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        synchronized (mInstances) {
            String path = mKeyStoreFile.getAbsolutePath();
            if (mInstances.containsKey(path)) {
                if (mInstances.get(path).get() == this)
                    mInstances.remove(path);
            }
        }
        super.finalize();
    }

    public void loadKeyStore(InputStream stream) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        synchronized (this) {
            mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mKeyStore.load(stream, null);
        }
    }

    private void createKeyStoreIfNull() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException {
        synchronized (this) {
            if (mKeyStore == null) {
                mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                mKeyStore.load(null, null);
            }
        }
    }

    public void saveKeyStore(OutputStream stream) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        synchronized (this) {
            createKeyStoreIfNull();
            mKeyStore.store(stream, null);
        }
    }

    public void saveKeyStore() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException {
        saveKeyStore(new FileOutputStream(mKeyStoreFile));
    }

    public void addCertificateException(X509Certificate certificate) {
        synchronized (this) {
            try {
                createKeyStoreIfNull();
                mKeyStore.setCertificateEntry("cert-" + UUID.randomUUID(), certificate);
                if (mKeyStoreFile != null)
                    saveKeyStore();
            } catch (Exception e) {
                Log.e(TAG, "Failed to add certificate exception");
                e.printStackTrace();
            }
        }
    }

    public void removeCertificate(String alias) {
        synchronized (this) {
            if (mKeyStore == null)
                return;
            try {
                mKeyStore.deleteEntry(alias);
                if (mKeyStoreFile != null)
                    saveKeyStore();
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove certificate");
                e.printStackTrace();
            }
        }
    }

    public List<String> getCertificateAliases() {
        synchronized (this) {
            if (mKeyStore == null)
                return null;
            try {
                return Collections.list(mKeyStore.aliases());
            } catch (KeyStoreException e) {
                return null;
            }
        }
    }

    public X509Certificate getCertificate(String alias) {
        synchronized (this) {
            try {
                return (X509Certificate) mKeyStore.getCertificate(alias);
            } catch (KeyStoreException e) {
                return null;
            }
        }
    }

    public boolean hasCertificate(Certificate certificate) throws KeyStoreException {
        synchronized (this) {
            return mKeyStore.getCertificateAlias(certificate) != null;
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        synchronized (this) {
            if (mKeyStoreTrustManager == null && mKeyStore != null)
                mKeyStoreTrustManager = createKeyStoreTrustManager(mKeyStore);
            if (mKeyStoreTrustManager == null)
                    throw new CertificateException("Key store is null");
            mKeyStoreTrustManager.checkServerTrusted(chain, authType);
        }
    }

    public static SpannableString buildCertOverviewString(X509Certificate cert) {
        String sha1Fingerprint;
        try {
            StringBuilder builder = new StringBuilder();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(cert.getEncoded());
            for (byte b : bytes)
                builder.append(String.format("%02x ", b));
            sha1Fingerprint = builder.toString();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        ColoredTextBuilder builder = new ColoredTextBuilder();
        builder.append("Subject: ", new StyleSpan(Typeface.BOLD));
        builder.append(cert.getSubjectX500Principal().getName().replace(",", ",\u200B"));
        builder.append("\nApplies to: ", new StyleSpan(Typeface.BOLD));
        builder.append(buildCertAppliesToString(cert));
        builder.append("\nIssuer: ", new StyleSpan(Typeface.BOLD));
        builder.append(cert.getIssuerDN().toString().replace(",", ",\u200B"));
        builder.append("\nSHA1 fingerprint:\n", new StyleSpan(Typeface.BOLD));
        builder.append(sha1Fingerprint);
        return SpannableString.valueOf(builder.getSpannable());
    }

    public static String buildCertAppliesToString(X509Certificate cert) {
        List<String> elements = new ArrayList<>();
        try {
            Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> altName : altNames) {
                    Integer altNameType = (Integer) altName.get(0);
                    if (altNameType != 2 && altNameType != 7) // dns or ip
                        continue;
                    elements.add((String) altName.get(1));
                }
            }
        } catch (CertificateParsingException ignored) {
        }

        if (elements.size() == 0)
            return "none";
        return TextUtils.join(",", elements.toArray());
    }

    public static X509TrustManager createKeyStoreTrustManager(KeyStore keyStore) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keyStore);
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager)
                    return (X509TrustManager) manager;
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
