package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableList;

import io.mrarm.irc.R;
import io.mrarm.irc.view.ServerIconView;

public class DefaultInsertBeforeGroup extends BaseGroup {

    @Override
    public String getName(Context ctx) {
        return ctx.getString(R.string.group_default_insert_before);
    }

    @Override
    public ServerIconView.CustomizationInfo getIconCustomization(Context ctx) {
        ServerIconView.CustomizationInfo ret = new ServerIconView.CustomizationInfo();
        ret.setColor(ctx.getResources().getColor(R.color.ircLightGray));
        ret.setCustomText("-");
        return ret;
    }

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        return null;
    }

}
