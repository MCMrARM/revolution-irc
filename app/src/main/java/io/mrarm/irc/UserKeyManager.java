package io.mrarm.irc;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

public class UserKeyManager extends X509ExtendedKeyManager {

    private static final String MY_KEY_ALIAS = "main";

    private X509Certificate mCertificate;
    private PrivateKey mPrivateKey;

    public UserKeyManager(X509Certificate cert, PrivateKey privateKey) {
        mCertificate = cert;
        mPrivateKey = privateKey;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        if (keyType.equals(mPrivateKey.getAlgorithm()))
            return new String[] { MY_KEY_ALIAS };
        return new String[0];
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        for (String kt : keyType) {
            if (mPrivateKey.getAlgorithm().equals(kt))
                return MY_KEY_ALIAS;
        }
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return new String[0];
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (alias.equals(MY_KEY_ALIAS))
            return new X509Certificate[] { mCertificate };
        return new X509Certificate[0];
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (alias.equals(MY_KEY_ALIAS))
            return mPrivateKey;
        return null;
    }

}
