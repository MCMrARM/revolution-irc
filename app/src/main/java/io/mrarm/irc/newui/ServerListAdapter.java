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
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.persistence.ServerUIInfo;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;
import io.mrarm.observabletransform.ObservableLists;
import io.mrarm.observabletransform.Observables;

public class ServerListAdapter extends DataAdapter implements
        RecyclerViewElevationDecoration.ItemElevationCallback {

    private final RecyclerViewElevationDecoration mDecoration;
    private ItemClickListener<ServerConnectionInfo> mActiveItemClickListener;
    private ItemClickListener<ServerUIInfo> mInactiveItemClickListener;

    public ServerListAdapter(Context context, ServerActiveListData activeData,
                             ServerInactiveListData inactiveData) {
        mDecoration = new RecyclerViewElevationDecoration(context, this);

        ObservableBoolean showActiveHeader = Observables.booleanTransform(
                ObservableLists.size(activeData.getConnections()), (x) -> x > 0);
        ObservableBoolean showInactiveHeader = Observables.booleanTransform(
                ObservableLists.size(inactiveData.getInactiveConnections()), (x) -> x > 0);

        DataMerger source = new DataMerger()
                .add(new ConditionalDataFragment<>(new SingleItemData<>(R.string.server_list_header_active, HEADER_TYPE), showActiveHeader))
                .add(new ListData<>(activeData.getConnections(), ACTIVE_SERVER_TYPE))
                .add(new ConditionalDataFragment<>(new SingleItemData<>(R.string.server_list_header_inactive, HEADER_TYPE), showInactiveHeader))
                .add(new ListData<>(inactiveData.getInactiveConnections(), INACTIVE_SERVER_TYPE));
        source.setContext(this);
        setSource(source, false);
    }

    public void setActiveItemClickListener(ItemClickListener<ServerConnectionInfo> listener) {
        this.mActiveItemClickListener = listener;
    }

    public void setInactiveItemClickListener(ItemClickListener<ServerUIInfo> listener) {
        this.mInactiveItemClickListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.addItemDecoration(mDecoration);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.removeItemDecoration(mDecoration);
    }

    @Override
    public boolean isItemElevated(int index) {
        return getSource().getHolderTypeFor(index) != HEADER_TYPE;
    }


    public static final ViewHolderType<Integer> HEADER_TYPE =
            ViewHolderType.<Integer>fromDataBinding(R.layout.main_server_list_header)
                    .setValueVarId(BR.titleId)
                    .build();

    public static final ViewHolderType<ServerConnectionInfo> ACTIVE_SERVER_TYPE =
            ViewHolderType.<ServerConnectionInfo>fromDataBinding(R.layout.main_server_list_server_active)
                    .setValueVarId(BR.server)
                    .<ServerListAdapter, ViewDataBinding>onBind((h, b, item, context) -> {
                        h.itemView.setOnClickListener((v) ->
                                context.mActiveItemClickListener.onItemClicked(item));
                    })
                    .build();

    public static final ViewHolderType<ServerUIInfo> INACTIVE_SERVER_TYPE =
            ViewHolderType.<ServerUIInfo>fromDataBinding(R.layout.main_server_list_server_inactive)
                    .setValueVarId(BR.server)
                    .<ServerListAdapter, ViewDataBinding>onBind((h, b, item, context) -> {
                        h.itemView.setOnClickListener((v) ->
                                context.mInactiveItemClickListener.onItemClicked(item));
                    })
                    .build();

    public interface ItemClickListener<T> {
        void onItemClicked(T item);
    }

}
