package io.mrarm.irc.config;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.mrarm.irc.R;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.SettableFuture;
import io.mrarm.irc.util.WarningDisplayContext;

public class ServerSSLHelper {

    private static final String TAG = "ServerSSLHelper";

    private static final Map<String, WeakReference<ServerSSLHelper>> mInstances = new HashMap<>();

    public static ServerSSLHelper get(File file) {
        synchronized (mInstances) {
            WeakReference<ServerSSLHelper> instance = mInstances.get(file.getAbsolutePath());
            if (instance != null) {
                ServerSSLHelper helper = instance.get();
                if (helper != null)
                    return helper;
            }
            ServerSSLHelper ret = new ServerSSLHelper(file);
            mInstances.put(file.getAbsolutePath(), new WeakReference<>(ret));
            return ret;
        }
    }

    public static ServerSSLHelper get(Context context, UUID serverUUID) {
        return get(ServerConfigManager.getInstance(context).getServerSSLCertsFile(serverUUID));
    }

    private File mKeyStoreFile;
    private KeyStore mKeyStore;
    private X509TrustManager mKeyStoreTrustManager;
    private List<X509Certificate> mTempTrustedCertificates;

    private ServerSSLHelper(File keyStoreFile) {
        mKeyStoreFile = keyStoreFile;
        if (keyStoreFile != null && keyStoreFile.exists()) {
            try {
                loadKeyStore(new FileInputStream(mKeyStoreFile));
            } catch (Exception e) {
                Log.w("ServerSSLHelper", "Failed to load keystore");
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

    private Future<Boolean> askUser(X509Certificate certificate, int stringId, Object... stringArgs) {
        SettableFuture<Boolean> ret = new SettableFuture<>();

        final Activity activity = WarningDisplayContext.getActivity();
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.certificate_error);
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.bad_certificate_layout, null, false);
            ((TextView) view.findViewById(R.id.error_certificate)).setText(buildCertOverviewString(certificate));
            ((TextView) view.findViewById(R.id.error_header)).setText(String.format(activity.getString(stringId), stringArgs));
            builder.setView(view);
            builder.setPositiveButton(R.string.action_cancel, (DialogInterface dialog, int which) -> {
                ret.set(false);
            });
            builder.setNegativeButton(R.string.certificate_error_ignore, (DialogInterface dialog, int which) -> {
                boolean remember = (((CheckBox) view.findViewById(R.id.error_remember)).isChecked());
                addCertificateException(certificate, !remember);
                ret.set(true);
            });
            builder.show();
        });

        return ret;
    }

    public void addCertificateException(X509Certificate certificate, boolean temporary) {
        synchronized (this) {
            if (temporary) {
                if (mTempTrustedCertificates == null)
                    mTempTrustedCertificates = new ArrayList<>();
                mTempTrustedCertificates.add(certificate);
                return;
            }
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

    public TrustManager createTrustManager() {
        final X509TrustManager defaultTrustManager = getKeyStoreTrustManager(null);
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                throw new CertificateException("Not supported");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    defaultTrustManager.checkServerTrusted(chain, authType);
                } catch (Exception e) {
                    synchronized (ServerSSLHelper.this) {
                        try {
                            if (mKeyStoreTrustManager == null && mKeyStore != null)
                                mKeyStoreTrustManager = getKeyStoreTrustManager(mKeyStore);
                            mKeyStoreTrustManager.checkServerTrusted(chain, authType);
                        } catch (Exception e2) {
                            if (mTempTrustedCertificates != null && mTempTrustedCertificates.contains(chain[0])) {
                                Log.i(TAG, "A temporarily trusted certificate is being used - trusting the server");
                                return;
                            }
                            Log.i(TAG, "Unrecognized certificate");
                            try {
                                X509Certificate cert = chain[0];
                                if (!askUser(cert, R.string.certificate_bad_cert).get())
                                    throw new CertificateException("User rejected the certificate");
                            } catch (InterruptedException | ExecutionException e3) {
                                throw new CertificateException("Asking user about the certificate failed");
                            }
                        }
                    }
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return defaultTrustManager.getAcceptedIssuers();
            }
        };
    }

    public HostnameVerifier createHostnameVerifier() {
        final HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        return (String hostname, SSLSession session) -> {
            if (hostnameVerifier.verify(hostname, session))
                return true;
            X509Certificate cert;
            try {
                cert = (X509Certificate) session.getPeerCertificates()[0];
            } catch (SSLPeerUnverifiedException e) {
                Log.e(TAG, "Error while trying to get certificate info");
                return false;
            }
            try {
                synchronized (this) {
                    if (mTempTrustedCertificates != null && mTempTrustedCertificates.contains(cert)) {
                        Log.i(TAG, "Accepting hostname as a temporarily trusted certificate is being used");
                        return true;
                    }
                    if (mKeyStore != null && mKeyStore.getCertificateAlias(cert) != null) {
                        Log.i(TAG, "Accepting hostname as a custom cert is being used");
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to find the certificate in the custom key store");
            }
            Log.i(TAG, "Failed to verify hostname, asking user");
            try {
                return askUser(cert, R.string.certificate_bad_hostname, buildCertAppliesToString(cert), hostname).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Error while trying to show a dialog");
                return false;
            }
        };
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

    private static String buildCertAppliesToString(X509Certificate cert) {
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

    public SocketFactory createSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{createTrustManager()}, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create a SSL socket factory");
        }
    }

    private static X509TrustManager getKeyStoreTrustManager(KeyStore keyStore) {
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
