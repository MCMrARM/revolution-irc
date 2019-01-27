package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.TypedValue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.theme.ThemeManager;

public class LiveThemeComponent {

    private Context mContext;
    private LiveThemeManager mLiveThemeManager;
    private Map<Integer, List<LiveThemeManager.ColorPropertyApplier>> mColors = new HashMap<>();
    private static final WeakHashMap<Context, WeakReference<Resources.Theme>> sThemeCache
            = new WeakHashMap<>();

    public LiveThemeComponent(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public void setLiveThemeManager(LiveThemeManager mgr) {
        mLiveThemeManager = mgr;
        if (mgr == null)
            return;
        for (Map.Entry<Integer, List<LiveThemeManager.ColorPropertyApplier>> e :
             mColors.entrySet()) {
            for (LiveThemeManager.ColorPropertyApplier v : e.getValue())
                mgr.addColorProperty(e.getKey(), v);
        }
    }

    public LiveThemeManager getLiveThemeManager() {
        return mLiveThemeManager;
    }

    private static Resources.Theme getThemeForContext(Context context) {
        WeakReference<Resources.Theme> retWeak = sThemeCache.get(context);
        Resources.Theme ret = null;
        if (retWeak != null)
            ret = retWeak.get();
        if (ret != null)
            return ret;

        if (context instanceof ContextWrapper) {
            Resources.Theme baseTheme = getThemeForContext(((ContextWrapper) context).getBaseContext());
            ret = baseTheme;
            int childResId = 0;
            if (context instanceof androidx.appcompat.view.ContextThemeWrapper) {
                childResId = ((androidx.appcompat.view.ContextThemeWrapper) context)
                        .getThemeResId();
            } else if (context instanceof android.view.ContextThemeWrapper) {
                childResId = LiveThemeUtils.getContextThemeWrapperResId(
                        (android.view.ContextThemeWrapper) context);
            }
            ThemeManager.ThemeResInfo t = ThemeManager.getInstance(context).getCurrentTheme();
            if (childResId == t.getThemeResId() || childResId == t.getThemeNoActionBarResId())
                childResId = R.style.LiveThemeHelperTheme;
            if (childResId != 0) {
                Resources.Theme newTheme = context.getResources().newTheme();
                newTheme.setTo(baseTheme);
                newTheme.applyStyle(childResId, true);
                ret = newTheme;
            }
        } else {
            ret = context.getResources().newTheme();
        }
        sThemeCache.put(context, new WeakReference<>(ret));
        return ret;
    }

    public Resources.Theme getTheme() {
        return getThemeForContext(mContext);
    }

    public boolean addColorAttr(StyledAttributesHelper attrs,
                                int attr, LiveThemeManager.ColorPropertyApplier applier,
                                ColorStateListApplier colorStateListApplier) {
        TypedValue typedValue = new TypedValue();
        if (!attrs.getValue(attr, typedValue))
            return false;
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            addColorProperty(typedValue.resourceId, applier);
            return true;
        } else if (typedValue.type != TypedValue.TYPE_NULL && colorStateListApplier != null) {
            try {
                ThemedColorStateList th = ThemedColorStateList.createFromXml(
                        mContext.getResources(),
                        mContext.getResources().getXml(typedValue.resourceId), getTheme());
                th.attachToComponent(this, () ->
                        colorStateListApplier.onColorStateListChanged(th.createColorStateList()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean addColorAttr(StyledAttributesHelper attrs, int attr,
                                LiveThemeManager.ColorPropertyApplier applier) {
        return addColorAttr(attrs, attr, applier, null);
    }


    public void addColorProperty(int res, LiveThemeManager.ColorPropertyApplier applier) {
        List<LiveThemeManager.ColorPropertyApplier> appliers = mColors.get(res);
        if (appliers == null) {
            appliers = new ArrayList<>();
            mColors.put(res, appliers);
        }
        appliers.add(applier);
        if (mLiveThemeManager != null)
            mLiveThemeManager.addColorProperty(res, applier);
    }


    public interface ColorStateListApplier {

        void onColorStateListChanged(ColorStateList newStateList);

    }


}
