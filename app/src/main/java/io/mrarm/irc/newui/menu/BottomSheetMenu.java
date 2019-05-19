package io.mrarm.irc.newui.menu;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import io.mrarm.irc.R;

public class BottomSheetMenu extends CustomMenu {

    private BottomSheetDialog mDialog;
    private RecyclerView mRecyclerView;

    public BottomSheetMenu(Context context) {
        super(context);
        mDialog = new BottomSheetDialog(context);
        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(new BottomSheetMenuAdapter(this, getItems()));
        int verticalPadding = context.getResources().getDimensionPixelSize(
                R.dimen.bottom_sheet_menu_vertical_margin);
        mRecyclerView.setPadding(0, verticalPadding, 0, verticalPadding);
        mDialog.setContentView(mRecyclerView);
    }

    public void show() {
        mDialog.show();
    }

    @Override
    public void close() {
        mDialog.cancel();
    }

}
