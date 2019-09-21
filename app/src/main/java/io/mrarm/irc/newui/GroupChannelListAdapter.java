package io.mrarm.irc.newui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import io.mrarm.dataadapter.ConditionalDataFragment;
import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.DataMerger;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.SingleItemData;
import io.mrarm.dataadapter.ViewHolderType;
import io.mrarm.irc.BR;
import io.mrarm.irc.R;
import io.mrarm.irc.newui.group.MasterGroup;
import io.mrarm.irc.newui.group.ServerChannelPair;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;
import io.mrarm.observabletransform.ObservableLists;
import io.mrarm.observabletransform.Observables;

public class GroupChannelListAdapter extends DataAdapter
        implements RecyclerViewElevationDecoration.ItemElevationCallback {

    private final RecyclerViewElevationDecoration mDecoration;

    private CallbackInterface mInterface;

    public GroupChannelListAdapter(Context context, MasterGroup group) {
        DataMerger m = new DataMerger();
        m.setContext(this);
        m.add(new SingleItemData<>(group.getName(context), TYPE_TITLE));
        m.add(new DataMerger(group.getGroups(), g -> {
            DataMerger ret = new DataMerger();
            if (g.getChannels() == null)
                return ret;
            ObservableBoolean showHeader = Observables.booleanTransform(
                    ObservableLists.size(g.getChannels()), (x) -> x > 0);
            ret.add(new ConditionalDataFragment<>(new SingleItemData<>(
                    g.getName(context), TYPE_HEADER), showHeader));
            ret.add(new ListData<>(g.getChannels(), TYPE_CHANNEL));
            return ret;
        }));
        setSource(m);
        mDecoration = new RecyclerViewElevationDecoration(context, this);
    }

    public void setCallbackInterface(CallbackInterface callbackInterface) {
        mInterface = callbackInterface;
    }

    @Override
    public boolean isItemElevated(int position) {
        return getSource().getHolderTypeFor(position) == TYPE_CHANNEL;
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

    public static ViewHolderType<String> TYPE_TITLE = ViewHolderType.<String>fromDataBinding(R.layout.main_server_list_title)
            .setValueVarId(BR.titleStr)
            .build();

    public static ViewHolderType<String> TYPE_HEADER = ViewHolderType.<String>fromDataBinding(R.layout.main_server_list_header)
            .setValueVarId(BR.titleStr)
            .build();

    public static ViewHolderType<ServerChannelPair> TYPE_CHANNEL = ViewHolderType.<ServerChannelPair>fromDataBinding(R.layout.main_server_list_channel)
            .setValueVarId(BR.channel)
            .<GroupChannelListAdapter, ViewDataBinding>onBind((h, b, item, context) -> {
                h.itemView.setOnClickListener((v) ->
                        context.mInterface.onChatOpened(item));
            })
            .build();

    public interface CallbackInterface {

        void onChatOpened(ServerChannelPair entry);

    }
}
