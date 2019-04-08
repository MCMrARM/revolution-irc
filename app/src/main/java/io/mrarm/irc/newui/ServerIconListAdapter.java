package io.mrarm.irc.newui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class ServerIconListAdapter extends ListAdapter<ServerConnectionInfo,
        ServerIconListAdapter.ServerIcon> {

    private final ServerIconListData mData;
    private IconClickListener mClickListener;

    public ServerIconListAdapter(ServerIconListData data) {
        super(new DiffUtil.ItemCallback<ServerConnectionInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull ServerConnectionInfo oldItem,
                                           @NonNull ServerConnectionInfo newItem) {
                return oldItem.getUUID().equals(newItem.getUUID());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ServerConnectionInfo oldItem,
                                              @NonNull ServerConnectionInfo newItem) {
                return oldItem.getUUID().equals(newItem.getUUID()) &&
                        oldItem.getName().equalsIgnoreCase(newItem.getName());
            }
        });
        mData = data;
        submitList(mData.getList());
        mData.addListener(() -> submitList(mData.getList()));
    }

    public void setClickListener(IconClickListener listener) {
        mClickListener = listener;
    }

    @NonNull
    @Override
    public ServerIcon onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.server_icon_list_text_entry, parent, false);
        return new TextServerIcon(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerIcon holder, int position) {
        holder.bind(getItem(position));
    }

    public abstract class ServerIcon extends RecyclerView.ViewHolder {

        public ServerIcon(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener((v) -> {
                if (mClickListener != null)
                    mClickListener.onIconClicked(getAdapterPosition());
            });
        }

        public abstract void bind(ServerConnectionInfo server);

    }

    public class TextServerIcon extends ServerIcon {

        private TextView mTextView;

        public TextServerIcon(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.text);
        }

        @Override
        public void bind(ServerConnectionInfo server) {
            mTextView.setText(server.getName().substring(0, 1).toUpperCase());
        }

    }

    public interface IconClickListener {

        void onIconClicked(int index);

    }

}
