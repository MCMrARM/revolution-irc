package io.mrarm.irc.chat.preview;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import androidx.core.util.PatternsCompat;

public class MessageLinkExtractor {

    public static String[] extractLinks(String text) {
        if (text == null)
            return null;
        List<String> links = null;
        @SuppressLint("RestrictedApi") Matcher m = PatternsCompat.AUTOLINK_WEB_URL.matcher(text);
        while (m.find()) {
            if (links == null)
                links = new ArrayList<>();
            String url = m.group();
            links.add(url);
        }
        if (links == null)
            return null;
        return links.toArray(new String[0]);
    }

}
