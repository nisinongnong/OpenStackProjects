package com.freerdp.freerdpcore.services;

/**
 * Created by T530 on 2017/4/25.
 */

public class LibFreeRDP {
    public static interface EventListener {
        void OnPreConnect(long instance);

        void OnConnectionSuccess(long instance);

        void OnConnectionFailure(long instance);

        void OnDisconnecting(long instance);

        void OnDisconnected(long instance);
    }

    public static interface UIEventListener {
        void OnSettingsChanged(int width, int height, int bpp);

        boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password);

        boolean OnGatewayAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder
                password);

        int OnVerifiyCertificate(String commonName, String subject,
                                 String issuer, String fingerprint, boolean mismatch);

        int OnVerifyChangedCertificate(String commonName, String subject,
                                       String issuer, String fingerprint, String oldSubject,
                                       String oldIssuer, String oldFingerprint);

        void OnCursorViewChanged(int type, int width, int height,int hot_x,int hot_y);

        void OnGraphicsUpdate(int x, int y, int width, int height);

        void OnGraphicsResize(int width, int height, int bpp);

        void OnRemoteClipboardChanged(String data);
    }
}
