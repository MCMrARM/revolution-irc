package io.mrarm.irc;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.M)
public class IRCChooserTargetService extends ChooserTargetService {

    private static final long SET_TIMEOUT = 15L * 60L * 1000L; // 15 minutes

    private static UUID sServer;
    private static String sChannel;
    private static long sSetTime;

    public static void setChannel(UUID server, String channel) {
        if (channel == null)
            return;
        sServer = server;
        sChannel = channel;
        sSetTime = System.currentTimeMillis();
    }

    public static void unsetChannel(UUID server, String channel) {
        if (server == sServer && channel != null && channel.equals(sChannel)) {
            sServer = null;
            sChannel = channel;
        }
    }

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetComponentName,
                                                   IntentFilter intentFilter) {
        if (sServer != null && sChannel != null) {
            if (System.currentTimeMillis() - sSetTime >= SET_TIMEOUT) {
                sServer = null;
                sChannel = null;
                return null;
            }
            ComponentName componentName = new ComponentName(getPackageName(),
                    MainActivity.class.getCanonicalName());

            List<ChooserTarget> targets = new ArrayList<>();
            Bundle extras = new Bundle();
            extras.putString(MainActivity.ARG_SERVER_UUID, sServer.toString());
            extras.putString(MainActivity.ARG_CHANNEL_NAME, sChannel);
            targets.add(new ChooserTarget(sChannel,
                    Icon.createWithResource(this, R.drawable.ic_direct_share),
                    1.f, componentName, extras));
            return targets;
        }
        return null;
    }

}
