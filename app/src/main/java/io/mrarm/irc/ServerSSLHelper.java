package io.mrarm.irc;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.List;
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

import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.SettableFuture;
import io.mrarm.irc.util.WarningDisplayContext;

public class ServerSSLHelper {

    private static final String TAG = "ServerSSLHelper";

    public static final String CONFIG_NAME = "SSLCustomCerts";

    private File mKeyStoreFile;
    private KeyStore mKeyStore;
    private List<X509Certificate> mTempTrustedCertificates;

    public ServerSSLHelper(File keyStoreFile) {
        mKeyStoreFile = keyStoreFile;
        if (keyStoreFile != null && keyStoreFile.exists())
            loadKeyStore();
    }

    private void loadKeyStore() {
        try {
            mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mKeyStore.load(new FileInputStream(mKeyStoreFile), null);
        } catch (Exception e) {
            Log.w("ServerSSLHelper", "Failed to load keystore");
            mKeyStore = null;
        }
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
        if (temporary) {
            if (mTempTrustedCertificates == null)
                mTempTrustedCertificates = new ArrayList<>();
            mTempTrustedCertificates.add(certificate);
            return;
        }
        try {
            if (mKeyStore == null) {
                mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                mKeyStore.load(null, null);
            }
            mKeyStore.setCertificateEntry("cert-" + UUID.randomUUID(), certificate);
            if (mKeyStoreFile != null)
                mKeyStore.store(new FileOutputStream(mKeyStoreFile), null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add certificate exception");
            e.printStackTrace();
        }
    }

    public TrustManager createTrustManager() {
        final X509TrustManager defaultTrustManager = getKeyStoreTrustManager(null);
        final X509TrustManager myTrustManager = mKeyStore != null ?
                getKeyStoreTrustManager(mKeyStore) : null;
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
                    try {
                        myTrustManager.checkServerTrusted(chain, authType);
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
                if (mTempTrustedCertificates != null && mTempTrustedCertificates.contains(cert)) {
                    Log.i(TAG, "Accepting hostname as a temporarily trusted certificate is being used");
                    return true;
                }
                if (mKeyStore != null && mKeyStore.getCertificateAlias(cert) != null) {
                    Log.i(TAG, "Accepting hostname as a custom cert is being used");
                    return true;
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

    private static SpannableString buildCertOverviewString(X509Certificate cert) {
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
        builder.append(buildCertAppliesToString(cert));
        builder.append("\nIssuer:  ", new StyleSpan(Typeface.BOLD));
        builder.append(cert.getIssuerDN().toString());
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
            sslContext.init(null, new TrustManager[]{ createTrustManager() }, null);
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
