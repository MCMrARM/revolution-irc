package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.support.annotation.XmlRes;
import android.util.TypedValue;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.mrarm.irc.util.StyledAttributesHelper;

public class LiveThemeComponent {

    private Context mContext;
    private LiveThemeManager mLiveThemeManager;
    private Map<Integer, List<LiveThemeManager.ColorPropertyApplier>> mColors = new HashMap<>();
    private static final WeakHashMap<Resources, WeakReference<Resources.Theme>> sThemeCache
            = new WeakHashMap<>();

    public LiveThemeComponent(Context context) {
        mContext = context;
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

    public Resources.Theme getTheme() {
        WeakReference<Resources.Theme> ref = sThemeCache.get(mContext.getResources());
        if (ref != null) {
            Resources.Theme theme = ref.get();
            if (theme != null)
                return theme;
        }
        Resources.Theme theme = mContext.getResources().newTheme();
        sThemeCache.put(mContext.getResources(), new WeakReference<>(theme));
        return theme;
    }

    public boolean addColorAttr(StyledAttributesHelper attrs, int attr,
                                LiveThemeManager.ColorPropertyApplier applier) {
        TypedValue typedValue = new TypedValue();
        if (!attrs.getValue(attr, typedValue))
            return false;
        if (typedValue.type == TypedValue.TYPE_ATTRIBUTE) {
            addColorProperty(typedValue.data, applier);
            return true;
        }
        return false;
    }


    private void addColorProperty(int res, LiveThemeManager.ColorPropertyApplier applier) {
        List<LiveThemeManager.ColorPropertyApplier> appliers = mColors.get(res);
        if (appliers == null) {
            appliers = new ArrayList<>();
            mColors.put(res, appliers);
        }
        appliers.add(applier);
        if (mLiveThemeManager != null)
            mLiveThemeManager.addColorProperty(res, applier);
    }


}
