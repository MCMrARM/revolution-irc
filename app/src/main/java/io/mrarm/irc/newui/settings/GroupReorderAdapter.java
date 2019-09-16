package io.mrarm.irc.newui.settings;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;

import io.mrarm.dataadapter.DataMerger;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.SimpleViewHolder;
import io.mrarm.dataadapter.SingleItemData;
import io.mrarm.dataadapter.ViewHolderType;
import io.mrarm.irc.R;
import io.mrarm.irc.BR;
import io.mrarm.irc.newui.group.Group;
import io.mrarm.irc.newui.group.MasterGroup;
import io.mrarm.irc.util.GroupItemTouchHelperCallback;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;
import io.mrarm.irc.util.ReorderableDataAdapter;

public class GroupReorderAdapter extends ReorderableDataAdapter
        implements RecyclerViewElevationDecoration.ItemElevationCallback,
        GroupItemTouchHelperCallback.AdapterInterface {

    private RecyclerViewElevationDecoration mDecoration;

    public GroupReorderAdapter(Context context, ObservableList<MasterGroup> groups) {
        setSource(new DataMerger(groups, (g) -> {
            DataMerger ret = new DataMerger();
            ret.add(new ListData<>(g.getGroups(), ITEM_TYPE));
            ret.add(new SingleItemData<>(null, SPACE_TYPE));
            return ret;
        }));
        mDecoration = new RecyclerViewElevationDecoration(context, this);
    }

    @Override
    public int findGroupIndexAt(int index) {
        return getSource().getElementPath(index).getIndexInParent(1);
    }

    @Override
    public int findGroupItemIndexAt(int index) {
        ViewHolderType type = getSource().getHolderTypeFor(index);
        if (type == ITEM_TYPE)
            return getSource().getElementPath(index).getIndexInParent(-1);
        else if (type == SPACE_TYPE)
            return -1;
        return 0;
    }

    @Override
    public boolean isItemElevated(int index) {
        return getSource().getHolderTypeFor(index) == ITEM_TYPE;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(mDecoration);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.removeItemDecoration(mDecoration);
    }


    public static final ViewHolderType<Group> ITEM_TYPE =
            ViewHolderType.<Group>fromDataBinding(R.layout.newui_group_reorder_entry)
                    .setValueVarId(BR.group)
                    .build();

    public static final ViewHolderType<Void> SPACE_TYPE =
            ViewHolderType.from((c, v) -> new EmptySpaceItem(c));


    private static class EmptySpaceItem extends SimpleViewHolder<Void> {

        private static View createView(Context context) {
            View v = new View(context);
            int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                    context.getResources().getDisplayMetrics());
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, p));
            return v;
        }

        public EmptySpaceItem(Context context) {
            super(createView(context));
        }

        @Override
        public void bind(Void n) {
        }

    }

}
