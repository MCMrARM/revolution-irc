package io.mrarm.irc.chat.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import io.mrarm.irc.BuildConfig;

public class LinkPreviewLoader implements Runnable {

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36";

    private URL mURL;
    private BitmapFactory.Options mBitmapOptions;
    private LoadCallback mLoadCallback;
    private URLConnection mConnection;
    private Thread mThread;

    public LinkPreviewLoader(URL url) {
        mURL = url;
    }

    public void setBitmapOptions(BitmapFactory.Options options) {
        mBitmapOptions = options;
    }

    public void setLoadCallback(LoadCallback callback) {
        mLoadCallback = callback;
    }

    private LinkPreviewInfo doLoad() throws IOException {
        URLConnection connection = mURL.openConnection();
        synchronized (this) {
            mConnection = connection;
        }
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoInput(true);
        connection.connect();
        String contentType = connection.getHeaderField("Content-Type");
        LinkPreviewInfo ret;
        if (contentType != null)
            contentType = contentType.toLowerCase();
        if (contentType == null) {
            ret = null;
        } else if (contentType.startsWith("image/")) {
            Bitmap image = decodeImage(connection.getInputStream());
            connection.getInputStream().close();
            ret = LinkPreviewInfo.fromImage(mURL.toString(), image);
        } else if (contentType.startsWith("text/html")) {
            OpenGraphDocumentParser p = new OpenGraphDocumentParser();
            p.parse(connection.getInputStream(), connection.getContentEncoding(), mURL.toString());
            String title = p.getTitle();
            String imageUrl = p.getImage();
            String desc = p.getDescription();
            Bitmap image = null;
            try {
                if (imageUrl != null)
                    image = loadImageFromUrl(imageUrl);
            } catch (IOException ignored) {
                Log.d("LinkPreviewLoader", "Failed to load image for link: " + mURL);
            }
            ret = LinkPreviewInfo.fromWebsite(mURL.toString(), title, desc, imageUrl, image);
        } else {
            ret = null;
        }
        return ret;
    }

    private Bitmap decodeImage(InputStream stream) throws IOException {
        return BitmapFactory.decodeStream(stream, null, mBitmapOptions);
    }

    private Bitmap loadImageFromUrl(String url) throws IOException {
        URLConnection connection = mURL.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoInput(true);
        connection.connect();

        Bitmap image = decodeImage(connection.getInputStream());
        connection.getInputStream().close();
        return image;
    }

    @Override
    public void run() {
        synchronized (this) {
            mThread = Thread.currentThread();
        }
        LinkPreviewInfo result = null;
        try {
            result = doLoad();
        } catch (IOException e) {
            Log.d("LinkPreviewLoader", "Failed to load preview for link: " + mURL);
            e.printStackTrace();
        }
        if (mLoadCallback != null)
            mLoadCallback.onLinkPreviewLoaded(result);
        synchronized (this) {
            mConnection = null;
            mThread = null;
        }
    }

    public void cancel() {
        synchronized (this) {
            if (mConnection instanceof HttpURLConnection)
                ((HttpURLConnection) mConnection).disconnect();
            if (mThread != null)
                mThread.interrupt();
        }
    }


    public interface LoadCallback {

        void onLinkPreviewLoaded(LinkPreviewInfo previewInfo);

    }

}
