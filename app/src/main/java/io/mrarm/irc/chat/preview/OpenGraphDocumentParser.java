package io.mrarm.irc.chat.preview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OpenGraphDocumentParser {

    private static final String KEY_TITLE = "og:title";
    private static final String KEY_TYPE = "og:type";
    private static final String KEY_IMAGE = "og:image";
    private static final String KEY_URL = "og:url";

    private static final String KEY_DESCRIPTION = "og:description";

    private static final String KEY_FALLBACK_DESCRIPTION = "description";

    private final Map<String, String> mMetaTags = new HashMap<>();
    private String mFallbackTitle;

    public Map<String, String> getMetaValues() {
        return mMetaTags;
    }

    public String getMetaValue(String key) {
        return mMetaTags.get(key);
    }

    public String getTitle() {
        String title = getMetaValue(KEY_TITLE);
        if (title == null)
            return mFallbackTitle;
        return title;
    }

    public String getDescription() {
        String value = getMetaValue(KEY_DESCRIPTION);
        if (value == null)
            return getMetaValue(KEY_FALLBACK_DESCRIPTION);
        return value;
    }

    public String getImage() {
        return getMetaValue(KEY_IMAGE);
    }

    public void parse(InputStream inputStream, String encoding, String url) throws IOException {
        Document doc = Jsoup.parse(inputStream, encoding, url);
        mFallbackTitle = doc.head().getElementsByTag("title").text();
        for (Element element : doc.head().getElementsByTag("meta")) {
            String prop = element.attr("property");
            if (prop.isEmpty())
                prop = element.attr("name");
            String content = element.attr("content");

            mMetaTags.put(prop, content);
        }
    }

}
