package io.mrarm.irc;

import android.content.Context;

public class IRCColorUtils {

    public static int[] COLOR_IDS = new int[] {
            R.color.ircBlack,
            R.color.ircWhite,
            R.color.ircBlue,
            R.color.ircGreen,
            R.color.ircLightRed,
            R.color.ircBrown,
            R.color.ircPurple,
            R.color.ircOrange,
            R.color.ircYellow,
            R.color.ircLightGreen,
            R.color.ircCyan,
            R.color.ircLightCyan,
            R.color.ircLightBlue,
            R.color.ircPink,
            R.color.ircGray,
            R.color.ircLightGray
    };

    public static int[] NICK_COLORS = new int[] { 3, 4, 7, 8, 9, 10, 11, 12, 13 };

    public static int getColor(Context context, int colorId) {
        return context.getResources().getColor(COLOR_IDS[colorId]);
    }

    public static int getNickColor(Context context, String nick) {
        int sum = 0;
        for (int i = 0; i < nick.length(); i++)
            sum += nick.charAt(i);
        return getColor(context, NICK_COLORS[sum % NICK_COLORS.length]);
    }
}
