package io.mrarm.irc.newui.menu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public abstract class CustomMenu implements Menu {

    private final Context mContext;
    private final ObservableList<Item> mItems = new ObservableArrayList<>();
    private int mVisibleItemCount = 0;
    private MenuItem.OnMenuItemClickListener mDefaultClickListener;

    public CustomMenu(Context context) {
        mContext = context;
    }

    protected ObservableList<MenuItem> getItems() {
        //noinspection unchecked
        return (ObservableList<MenuItem>) (ObservableList) mItems;
    }

    public void setClickListener(MenuItem.OnMenuItemClickListener listener) {
        mDefaultClickListener = listener;
    }

    @Override
    public MenuItem add(CharSequence title) {
        return add(NONE, NONE, NONE, title);
    }

    @Override
    public MenuItem add(int titleRes) {
        return add(NONE, NONE, NONE, mContext.getString(titleRes));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        Item item = new Item(itemId, groupId, order, title);
        mItems.add(item);
        return item;
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return add(groupId, itemId, order, mContext.getString(titleRes));
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeItem(int id) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).mId == id) {
                mItems.remove(i);
                return;
            }
        }
    }

    @Override
    public void removeGroup(int groupId) {
        for (int i = mItems.size() - 1; i >= 0; --i) {
            if (mItems.get(i).mGroupId == groupId)
                mItems.remove(i);
        }
    }

    @Override
    public void clear() {
        for (Item i : mItems)
            i.mDeleted = true;
        mItems.clear();
        mVisibleItemCount = 0;
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {

    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        for (Item i : mItems) {
            if (i.mGroupId == group)
                i.setVisible(visible);
        }
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        for (Item i : mItems) {
            if (i.mGroupId == group)
                i.setEnabled(enabled);
        }
    }

    @Override
    public boolean hasVisibleItems() {
        return mVisibleItemCount > 0;
    }

    @Override
    public MenuItem findItem(int id) {
        for (Item i : mItems)
            if (i.mId == id)
                return i;
        return null;
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    public MenuItem getItem(int index) {
        return mItems.get(index);
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return false;
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return false;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean performIdentifierAction(int id, int flags) {
        Item itm = (Item) findItem(id);
        if (itm.mClickListener != null && itm.mClickListener.onMenuItemClick(itm))
            return true;
        if (mDefaultClickListener != null && mDefaultClickListener.onMenuItemClick(itm))
            return true;
        return false;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
    }

    private class Item implements MenuItem {

        private int mId;
        private int mGroupId;
        private int mOrder;
        private CharSequence mTitle;
        private Drawable mIcon;
        private boolean mIsVisible;
        private boolean mEnabled = true;
        private OnMenuItemClickListener mClickListener;

        private boolean mDeleted = false;

        Item(int id, int group, int order, CharSequence title) {
            mId = id;
            mGroupId = group;
            mOrder = order;
            mTitle = title;
            mIsVisible = true;
            ++mVisibleItemCount;
        }

        @Override
        public int getItemId() {
            return mId;
        }

        @Override
        public int getGroupId() {
            return mGroupId;
        }

        @Override
        public int getOrder() {
            return mOrder;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        @Override
        public MenuItem setTitle(int title) {
            mTitle = mContext.getString(title);
            return this;
        }

        @Override
        public CharSequence getTitle() {
            return mTitle;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            return this;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return null;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            mIcon = ContextCompat.getDrawable(mContext, iconRes);
            return this;
        }

        @Override
        public Drawable getIcon() {
            return mIcon;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            return this;
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            return this;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            return this;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            return this;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            return this;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            return this;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            if (!mDeleted && visible != mIsVisible) {
                if (visible)
                    ++mVisibleItemCount;
                else
                    --mVisibleItemCount;
            }
            mIsVisible = visible;
            return this;
        }

        @Override
        public boolean isVisible() {
            return mIsVisible;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return mEnabled;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener listener) {
            mClickListener = listener;
            return this;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {
        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            return this;
        }

        @Override
        public MenuItem setActionView(View view) {
            return this;
        }

        @Override
        public MenuItem setActionView(int resId) {
            return this;
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            return this;
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            return this;
        }
    }

}
