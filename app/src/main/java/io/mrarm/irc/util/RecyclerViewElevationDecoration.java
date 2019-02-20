package io.mrarm.irc.util;

import android.content.Context;

public class RecyclerViewElevationDecoration extends RecyclerViewBaseElevationDecoration {

    private final ItemElevationCallback mCallback;

    public RecyclerViewElevationDecoration(Context context, ItemElevationCallback callback) {
        super(context);
        mCallback = callback;
    }

    @Override
    public boolean isItemElevated(int index) {
        return mCallback.isItemElevated(index);
    }

    public interface ItemElevationCallback {

        boolean isItemElevated(int index);

    }

}
