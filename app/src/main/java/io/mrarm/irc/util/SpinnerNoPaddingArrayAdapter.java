package io.mrarm.irc.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class SpinnerNoPaddingArrayAdapter<T> extends ArrayAdapter<T> {

    public SpinnerNoPaddingArrayAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View ret = super.getView(position, convertView, parent);
        ret.setPadding(0, ret.getPaddingTop(), 0, ret.getPaddingBottom());
        return ret;
    }

}
