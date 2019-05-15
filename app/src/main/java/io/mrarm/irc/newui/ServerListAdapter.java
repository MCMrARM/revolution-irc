package io.mrarm.irc.newui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.recyclerview.widget.RecyclerView;

import io.mrarm.dataadapter.ConditionalDataFragment;
import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.DataMerger;
import io.mrarm.dataadapter.ListData;
import io.mrarm.dataadapter.SimpleViewHolder;
import io.mrarm.dataadapter.SingleItemData;
import io.mrarm.dataadapter.ViewHolderType;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;
import io.mrarm.irc.view.ServerIconView;
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
                .add(new ConditionalDataFragment<>(new SingleItemData<>(R.string.server_list_header_active, HeaderHolder.TYPE), showActiveHeader))
                .add(new ListData<>(activeData.getConnections(), ActiveServerHolder.TYPE))
                .add(new ConditionalDataFragment<>(new SingleItemData<>(R.string.server_list_header_inactive, HeaderHolder.TYPE), showInactiveHeader))
                .add(new ListData<>(inactiveData.getInactiveConnections(), ActiveServerHolder.TYPE));
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
        return getSource().getHolderTypeFor(index) != HeaderHolder.TYPE;
    }


    public static class HeaderHolder extends SimpleViewHolder<Integer> {

        public static final ViewHolderType<Integer> TYPE = ViewHolderType.from((ctx, parent) -> {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_header, parent, false);
            return new HeaderHolder(view);
        });

        private TextView mTextView;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(Integer integer) {
            mTextView.setText(integer);
        }

    }


    public static class ActiveServerHolder extends SimpleViewHolder<Object> {

        public static final ViewHolderType<Object> TYPE = ViewHolderType.from((ctx, parent) -> {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_server, parent, false);
            return new ActiveServerHolder(view);
        });

        private ServerIconView mServerIcon;
        private TextView mName;
        private TextView mDescription;

        public ActiveServerHolder(@NonNull View itemView) {
            super(itemView);
            mServerIcon = itemView.findViewById(R.id.icon);
            mName = itemView.findViewById(R.id.name);
            mDescription = itemView.findViewById(R.id.desc);
        }

        @Override
        public void bind(Object o) {
            if (o instanceof ServerConnectionInfo)
                bind((ServerConnectionInfo) o);
            if (o instanceof ServerConfigData)
                bind((ServerConfigData) o);
        }

        public void bind(ServerConnectionInfo conn) {
            mServerIcon.setServerName(conn.getName().substring(0, 1));
            mName.setText(conn.getName());
            int statusId = R.string.server_list_state_disconnected;
            if (conn.isConnected())
                statusId = R.string.server_list_state_connected;
            else if (conn.isConnecting())
                statusId = R.string.server_list_state_connecting;
            int channelCount = conn.getChannels().size();
            String channelCounter = mDescription.getResources().getQuantityString(
                    R.plurals.server_list_channel_counter, channelCount, channelCount);

            mDescription.setText(mDescription.getResources().getString(
                    R.string.server_list_channel_counter_with_state,
                    channelCounter, mDescription.getResources().getString(statusId)));
        }

        public void bind(ServerConfigData conn) {
            mServerIcon.setServerName(conn.name.substring(0, 1));
            mName.setText(conn.name);
        }

    }

}
