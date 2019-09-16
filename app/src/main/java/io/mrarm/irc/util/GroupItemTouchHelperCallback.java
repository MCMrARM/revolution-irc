package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Canvas;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public abstract class GroupItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final AdapterInterface mAdapter;
    private float mCreateNewGroupThreshold;

    private final Runnable mCreateNewGroupRunnable = this::createNewGroupFromHover;
    private RecyclerView.ViewHolder mCreateNewGroupVH;
    private boolean mCreateNewGroupBelow = false;
    private boolean mCreateNewGroupPosted = false;

    public GroupItemTouchHelperCallback(@NonNull AdapterInterface adapterInterface) {
        mAdapter = adapterInterface;
    }

    public void setCreateNewGroupThresholdDp(Context ctx, float value) {
        mCreateNewGroupThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, ctx.getResources().getDisplayMetrics());
    }

    private @NonNull AdapterInterface getAdapter() {
        return mAdapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        cancelCreateNewGroupFromHover(recyclerView);
        int srcPos = viewHolder.getAdapterPosition();
        int srcG = getAdapter().findGroupIndexAt(srcPos);
        int srcI = getAdapter().findGroupItemIndexAt(srcPos);
        int dstPos = target.getAdapterPosition();
        int dstG = getAdapter().findGroupIndexAt(dstPos);
        int dstI = getAdapter().findGroupItemIndexAt(dstPos);
        if (srcPos == dstPos || (srcG == dstG && srcI == dstI) || (srcG == -1 || srcI == -1))
            return true;
        if (dstI == -1) { // Cross group boundary?
            if (dstPos > srcPos) {
                dstG = getAdapter().findGroupIndexAt(dstPos + 1);
                dstI = getAdapter().findGroupItemIndexAt(dstPos + 1);
                if (dstI == -1) {
                    createGroup(dstG + 1);
                    dstG = dstG + 1;
                    dstI = 0;
                }
            } else {
                dstG = getAdapter().findGroupIndexAt(dstPos - 1);
                dstI = getAdapter().findGroupItemIndexAt(dstPos - 1) + 1;
                if (dstI == 0)
                    return true;
            }
        }
        // Log.v("GroupTouchHelper", "Move " + srcG + ":" + srcI + " to " + dstG + ":" + dstI);

        moveItem(srcG, srcI, dstG, dstI);
        if (getGroupSize(srcG) == 0)
            deleteGroup(srcG);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            if (isCurrentlyActive) {
                // HACK: The ItemTouchHelper has a bug where if you quickly drag an item to the end
                // of a list, it doesn't properly move it.
                RecyclerView.ViewHolder lvh = recyclerView.findViewHolderForAdapterPosition(
                        recyclerView.getAdapter().getItemCount() - 1);
                if (dY + viewHolder.itemView.getTop() > lvh.itemView.getBottom())
                    onMove(recyclerView, viewHolder, lvh);

                considerCreateNewGroupFromHover(recyclerView, viewHolder, dY);
            } else {
                cancelCreateNewGroupFromHover(recyclerView);
            }
        }
    }

    private void considerCreateNewGroupFromHover(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder, float dY) {
        mCreateNewGroupVH = viewHolder;
        int srcG = getAdapter().findGroupIndexAt(viewHolder.getAdapterPosition());
        int srcI = getAdapter().findGroupItemIndexAt(viewHolder.getAdapterPosition());

        // Don't consider creating a new group if we are already in a single-item group
        if (getGroupSize(srcG) <= 1) {
            cancelCreateNewGroupFromHover(rv);
            return;
        }

        if (dY < -mCreateNewGroupThreshold && srcI == 0) {
            mCreateNewGroupBelow = false;
        } else if (dY > mCreateNewGroupThreshold && srcI == getGroupSize(srcG) - 1) {
            mCreateNewGroupBelow = true;
        } else {
            cancelCreateNewGroupFromHover(rv);
            return;
        }
        if (!mCreateNewGroupPosted)
            rv.postDelayed(mCreateNewGroupRunnable, 100L);
        mCreateNewGroupPosted = true;
    }

    private void createNewGroupFromHover() {
        int srcG = getAdapter().findGroupIndexAt(mCreateNewGroupVH.getAdapterPosition());
        // Make sure we're already not in a single-item group
        if (getGroupSize(srcG) <= 1)
            return;
        int srcI = getAdapter().findGroupItemIndexAt(mCreateNewGroupVH.getAdapterPosition());
        int dstG = srcG;
        if (mCreateNewGroupBelow)
            ++dstG;
        else
            ++srcG;
        createGroup(dstG);
        moveItem(srcG, srcI, dstG, 0);
    }

    private void cancelCreateNewGroupFromHover(RecyclerView rv) {
        if (mCreateNewGroupPosted)
            rv.removeCallbacks(mCreateNewGroupRunnable);
        mCreateNewGroupVH = null;
        mCreateNewGroupPosted = false;
    }

    public abstract int getGroupSize(int group);

    public abstract void createGroup(int groupPos);

    public abstract void deleteGroup(int groupPos);

    public abstract void moveItem(int fromGrp, int fromIdx, int toGrp, int toIdx);


    public interface AdapterInterface {

        int findGroupIndexAt(int pos);

        int findGroupItemIndexAt(int pos);

    }

}
