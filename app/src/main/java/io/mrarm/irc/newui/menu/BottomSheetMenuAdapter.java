package io.mrarm.irc.newui.menu;

import android.view.MenuItem;

import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;

import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.ViewHolderType;
import io.mrarm.irc.BR;
import io.mrarm.irc.R;

class BottomSheetMenuAdapter extends DataAdapter {

    private ObservableList<MenuItem> mItems;

    BottomSheetMenuAdapter(BottomSheetMenu menu, ObservableList<MenuItem> items) {
        mItems = items;

        ListData<MenuItem> data = new ListData<>(items, ITEM_TYPE);
        data.setContext(menu);
        setSource(data);
    }


    public static final ViewHolderType<MenuItem> ITEM_TYPE =
            ViewHolderType.<MenuItem>fromDataBinding(R.layout.newui_bottom_sheet_menu_item)
                    .setValueVarId(BR.item)
                    .<BottomSheetMenu, ViewDataBinding>onBind((h, b, item, ctx) -> {
                        h.itemView.setOnClickListener((v) -> {
                            if (ctx.performIdentifierAction(item.getItemId(), 0))
                                ctx.close();
                        });
                    })
                    .build();

}
