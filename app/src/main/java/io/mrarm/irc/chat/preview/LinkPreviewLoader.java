package io.mrarm.irc.chat.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.chat.preview.cache.LinkPreviewCacheManager;
import io.mrarm.irc.chat.preview.cache.LinkPreviewInfo;

public class LinkPreviewLoader implements Runnable {

    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36";

    private final URL mURL;
    private final BitmapFactory.Options mBitmapOptions;
    private final LinkPreviewCacheManager mCacheManager;
    private List<LoadCallback> mLoadCallbacks = new ArrayList<>();
    private URLConnection mConnection;
    private Thread mThread;
    private boolean mHasResult = false;
    private LinkPreviewInfo mResult;
    int mRefCount = 0; // used by LinkPreviewLoadManager

    public LinkPreviewLoader(URL url, BitmapFactory.Options options,
                             LinkPreviewCacheManager cacheManager) {
        mURL = url;
        mBitmapOptions = options;
        mCacheManager = cacheManager;
    }

    public URL getURL() {
        return mURL;
    }

    public synchronized void addLoadCallback(LoadCallback callback) {
        if (mHasResult) {
            callback.onLinkPreviewLoaded(mResult);
            return;
        }
        mLoadCallbacks.add(callback);
    }

    public synchronized void removeLoadCallback(LoadCallback callback) {
        if (mLoadCallbacks != null)
            mLoadCallbacks.remove(callback);
    }

    private void setResult(LinkPreviewInfo result) {
        List<LoadCallback> callbacks;
        synchronized (this) {
            mHasResult = true;
            mResult = result;
            callbacks = mLoadCallbacks;
            mLoadCallbacks = null;
        }
        for (LoadCallback callback : callbacks)
            callback.onLinkPreviewLoaded(result);
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
            ret = LinkPreviewInfo.fromWebsite(mURL.toString(), title, desc, imageUrl, null);
        } else {
            ret = null;
        }
        return ret;
    }

    private Bitmap decodeImage(InputStream stream) throws IOException {
        return BitmapFactory.decodeStream(stream, null, mBitmapOptions);
    }

    private Bitmap loadImageFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
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
        LinkPreviewInfo result;
        result = mCacheManager.getDatabase().linkPreviewDao().findPreviewFor(mURL.toString());
        boolean updateLastUsed = false;
        if (result != null) {
            Log.d("LinkPreviewLoader", "Got link preview from cache: " + mURL);
            result.updateLastUsed();
            updateLastUsed = true;
        } else {
            Log.d("LinkPreviewLoader", "Loading link preview from network: " + mURL);
            try {
                result = doLoad();
            } catch (IOException e) {
                Log.d("LinkPreviewLoader", "Failed to load preview for link: " + mURL);
                e.printStackTrace();
            }
            mCacheManager.getDatabase().linkPreviewDao().insertPreview(result);
        }
        mCacheManager.deleteLeastRecentlyUsedPreviews();
        if (result != null && result.getImageUrl() != null && !result.getImageUrl().isEmpty() &&
                result.getImage() == null) {
            try {
                String url = result.getImageUrl();
                Bitmap bmp = mCacheManager.getImageCache().getImageFromCache(url);
                if (bmp != null) {
                    Log.d("LinkPreviewLoader", "Got image from cache: " + url);
                    result.setImage(bmp);
                } else {
                    Log.d("LinkPreviewLoader", "Loading image from network: " + url);
                    bmp = loadImageFromUrl(url);
                    result.setImage(bmp);
                    if (bmp != null)
                        mCacheManager.getImageCache().storeImageInCache(url, bmp);
                }
            } catch (IOException ignored) {
                Log.d("LinkPreviewLoader", "Failed to load image for link: " + mURL);
            }
        }
        setResult(result);
        synchronized (this) {
            if (updateLastUsed) {
                mCacheManager.getDatabase().linkPreviewDao().updateLastUsed(mURL.toString(),
                        result.getLastUsed());
            }
            mConnection = null;
            mThread = null;
        }
    }

    public synchronized void cancel() {
        if (mConnection instanceof HttpURLConnection)
            ((HttpURLConnection) mConnection).disconnect();
        if (mThread != null)
            mThread.interrupt();
    }


    public interface LoadCallback {

        void onLinkPreviewLoaded(LinkPreviewInfo previewInfo);

    }

}
