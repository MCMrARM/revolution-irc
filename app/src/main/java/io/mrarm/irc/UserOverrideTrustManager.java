package io.mrarm.irc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import io.mrarm.irc.config.ServerCertificateManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.SettableFuture;
import io.mrarm.irc.util.WarningHelper;

public class UserOverrideTrustManager implements X509TrustManager, HostnameVerifier {

    private static final String TAG = "CertificateManager";

    private static final X509TrustManager sDefaultTrustManager = ServerCertificateManager.createKeyStoreTrustManager(null);

    private final Context mContext;
    private final UUID mServerUUID;
    private final ServerCertificateManager mManager;
    private List<X509Certificate> mTempTrustedCertificates;

    public UserOverrideTrustManager(Context context, UUID serverUUID) {
        mContext = context;
        mServerUUID = serverUUID;
        mManager = ServerCertificateManager.get(context, serverUUID);
    }

    private String getServerName() {
        ServerConfigData server = ServerConfigManager.getInstance(mContext).findServer(mServerUUID);
        if (server != null)
            return server.name;
        return null;
    }

    private Future<Boolean> askUser(X509Certificate certificate, int stringId, Object... stringArgs) {
        SSLCertWarning warning = new SSLCertWarning(certificate, stringId, stringArgs);
        WarningHelper.showWarning(warning);
        return warning.mReturnValue;
    }

    public void addCertificateException(X509Certificate certificate, boolean temporary) {
        if (temporary) {
            synchronized (this) {
                if (mTempTrustedCertificates == null)
                    mTempTrustedCertificates = new ArrayList<>();
                mTempTrustedCertificates.add(certificate);
            }
        } else {
            mManager.addCertificateException(certificate);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        throw new CertificateException("Not supported");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            sDefaultTrustManager.checkServerTrusted(chain, authType);
        } catch (Exception e) {
            try {
                mManager.checkServerTrusted(chain, authType);
            } catch (Exception e2) {
                synchronized (UserOverrideTrustManager.this) {
                    if (mTempTrustedCertificates != null && mTempTrustedCertificates.contains(chain[0])) {
                        Log.i(TAG, "A temporarily trusted certificate is being used - trusting the server");
                        return;
                    }
                }
                Log.i(TAG, "Unrecognized certificate");
                try {
                    X509Certificate cert = chain[0];
                    if (!askUser(cert, R.string.certificate_bad_cert).get())
                        throw new UserRejectedCertificateException();
                } catch (InterruptedException | ExecutionException e3) {
                    throw new CertificateException("Asking user about the certificate failed");
                }
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return sDefaultTrustManager.getAcceptedIssuers();
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        if (HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session))
            return true;
        X509Certificate cert;
        try {
            cert = (X509Certificate) session.getPeerCertificates()[0];
        } catch (SSLPeerUnverifiedException e) {
            Log.e(TAG, "Error while trying to get certificate info");
            return false;
        }
        try {
            synchronized (UserOverrideTrustManager.this) {
                if (mTempTrustedCertificates != null && mTempTrustedCertificates.contains(cert)) {
                    Log.i(TAG, "Accepting hostname as a temporarily trusted certificate is being used");
                    return true;
                }
            }
            if (mManager.hasCertificate(cert)) {
                Log.i(TAG, "Accepting hostname as a custom cert is being used");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to find the certificate in the custom key store");
        }
        Log.i(TAG, "Failed to verify hostname, asking user");
        try {
            return askUser(cert, R.string.certificate_bad_hostname, ServerCertificateManager.buildCertAppliesToString(cert), hostname).get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error while trying to show a dialog");
            return false;
        }
    }

    private class SSLCertWarning extends WarningHelper.Warning {

        private X509Certificate mCertificate;
        private SettableFuture<Boolean> mReturnValue;
        private int mStringId;
        private Object[] mStringArgs;
        private WeakReference<AlertDialog> mLastDialog;

        public SSLCertWarning(X509Certificate certificate, int stringId, Object[] stringArgs) {
            mCertificate = certificate;
            mStringId = stringId;
            mStringArgs = stringArgs;
            mReturnValue = new SettableFuture<>();
        }

        @Override
        protected void buildNotification(Context context, NotificationCompat.Builder notification, int notificationId) {
            super.buildNotification(context, notification, notificationId);
            notification.setContentText(context.getString(R.string.certificate_error, getServerName()));
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra(MainActivity.ARG_SERVER_UUID, mServerUUID.toString());
            notification.setContentIntent(PendingIntent.getActivity(context, notificationId, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        @Override
        public void showDialog(Activity activity) {
            dismissDialog(null);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.certificate_error, getServerName()));
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.bad_certificate_layout, null, false);
            ((TextView) view.findViewById(R.id.error_certificate)).setText(ServerCertificateManager.buildCertOverviewString(mCertificate));
            ((TextView) view.findViewById(R.id.error_header)).setText(String.format(activity.getString(mStringId), mStringArgs));
            builder.setView(view);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.action_cancel, (DialogInterface dialog, int which) -> {
                mReturnValue.set(false);
                dismiss();
            });
            builder.setNegativeButton(R.string.certificate_error_ignore, (DialogInterface dialog, int which) -> {
                if (mReturnValue.isDone())
                    return;
                boolean remember = (((CheckBox) view.findViewById(R.id.error_remember)).isChecked());
                addCertificateException(mCertificate, !remember);
                mReturnValue.set(true);
                dismiss();
            });
            builder.setOnDismissListener((DialogInterface di) -> {
                if (mLastDialog != null && mLastDialog.get() == di)
                    mLastDialog = null;
            });
            mLastDialog = new WeakReference<>(builder.show());
        }

        @Override
        public void dismissDialog(Activity activity) {
            if (mLastDialog != null) {
                AlertDialog dialog = mLastDialog.get();
                if (dialog != null)
                    dialog.dismiss();
                mLastDialog = null;
            }
        }
    }

    public static class UserRejectedCertificateException extends CertificateException {

        public UserRejectedCertificateException() {
            super("User rejected the certificate");
        }

    }

}
