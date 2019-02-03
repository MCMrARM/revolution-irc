package io.mrarm.irc.chat.preview;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.mrarm.irc.chat.preview.cache.LinkPreviewCacheManager;

public class LinkPreviewLoadManager {

    private static LinkPreviewLoadManager sInstance;

    public static synchronized LinkPreviewLoadManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new LinkPreviewLoadManager(context.getApplicationContext());
        return sInstance;
    }

    private final Context mContext;
    private final LinkPreviewCacheManager mCache;
    private final Map<URL, LinkPreviewLoader> mTasks = new HashMap<>();

    public LinkPreviewLoadManager(Context context) {
        mContext = context;
        mCache = new LinkPreviewCacheManager(mContext);
    }

    public LinkPreviewCacheManager getCache() {
        return mCache;
    }

    public synchronized LoadHandle load(URL url) {
        LinkPreviewLoader task = mTasks.get(url);
        if (task == null) {
            LinkPreviewLoader newTask = new LinkPreviewLoader(url, getCache());
            task = newTask;
            task.addLoadCallback((i) -> {
                synchronized (this) {
                    mTasks.remove(newTask.getURL());
                }
            });
            task.mRefCount++;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(task);
            mTasks.put(task.getURL(), task);
        } else {
            synchronized (task) {
                task.mRefCount++;
            }
        }
        return new LoadHandle(task);
    }


    public class LoadHandle {

        private LinkPreviewLoader mLoader;
        private List<LinkPreviewLoader.LoadCallback> mCallbacks = new ArrayList<>();

        private LoadHandle(@NonNull LinkPreviewLoader loader) {
            mLoader = loader;
        }

        public LoadHandle addLoadCallback(LinkPreviewLoader.LoadCallback cb) {
            mLoader.addLoadCallback(cb);
            mCallbacks.add(cb);
            return this;
        }

        public void cancel() {
            for (LinkPreviewLoader.LoadCallback cb : mCallbacks)
                mLoader.removeLoadCallback(cb);
            mCallbacks.clear();
            synchronized (LinkPreviewLoadManager.this) {
                synchronized (mLoader) {
                    if (--mLoader.mRefCount <= 0) {
                        mTasks.remove(mLoader.getURL());
                    }
                }
            }
            mLoader = null;
        }

    }

}
