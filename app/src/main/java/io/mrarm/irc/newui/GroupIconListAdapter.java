package io.mrarm.irc.newui;

import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;

import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.ViewHolderType;
import io.mrarm.irc.BR;
import io.mrarm.irc.R;
import io.mrarm.irc.newui.group.MasterGroup;

public class GroupIconListAdapter extends DataAdapter {

    private IconClickListener mClickListener;

    public GroupIconListAdapter(ObservableList<MasterGroup> masterGroups) {
        ListData<MasterGroup> data = new ListData<>(masterGroups, ITEM_TYPE);
        data.setContext(this);
        setSource(data);
    }

    public void setClickListener(IconClickListener listener) {
        mClickListener = listener;
    }

    private static ViewHolderType<MasterGroup> ITEM_TYPE =
            ViewHolderType.<MasterGroup>fromDataBinding(R.layout.group_icon_list_item)
                    .setValueVarId(BR.group)
                    .<GroupIconListAdapter, ViewDataBinding>onBind((h, b, item, context) -> {
                        h.itemView.setOnClickListener((v) -> {
                            int i = context.getSource().getElementPath(h.getAdapterPosition())
                                    .getIndexInParent(-1);
                            context.mClickListener.onIconClicked(i);
                        });
                    })
                    .build();


    public interface IconClickListener {

        void onIconClicked(int index);

    }

}
