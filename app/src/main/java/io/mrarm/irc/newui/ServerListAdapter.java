package io.mrarm.irc.newui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
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
        setSource(source, false);
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
                    .build();

    public static final ViewHolderType<ServerUIInfo> INACTIVE_SERVER_TYPE =
            ViewHolderType.<ServerUIInfo>fromDataBinding(R.layout.main_server_list_server_inactive)
                    .setValueVarId(BR.server)
                    .build();

}
