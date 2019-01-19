package io.mrarm.irc.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.ClickableRecyclerViewAdapter;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ChannelSearchDialog extends SearchDialog {

    private SuggestionsAdapter mAdapter;

    public ChannelSearchDialog(@NonNull Context context, ChannelSelectedListener listener) {
        super(context);

        getSearchView().setBackgroundColor(StyledAttributesHelper.getColor(context, R.attr.colorBackgroundFloating, 0));

        mAdapter = new SuggestionsAdapter(context);
        mAdapter.setItemClickListener((int index, Pair<ServerConnectionInfo, String> value) ->{
            if (listener != null)
                listener.onChannelSelected(value.first, value.second);
            dismiss();
        });
        setSuggestionsAdapter(mAdapter);
    }

    public void onQueryTextChange(String newText) {
        mAdapter.filterWithQuery(newText);
    }

    public static class SuggestionsAdapter extends ClickableRecyclerViewAdapter<SuggestionsAdapter.SuggestionHolder, Pair<ServerConnectionInfo, String>> {

        private final ServerConnectionManager mConnectionManager;
        private final int mSecondaryTextColor;
        private final int mHighlightTextColor;
        private String mHighlightQuery;

        public SuggestionsAdapter(Context context) {
            setViewHolderFactory(SuggestionHolder::new, R.layout.dialog_search_item);
            mConnectionManager = ServerConnectionManager.getInstance(context);
            mSecondaryTextColor = StyledAttributesHelper.getColor(context,
                    android.R.attr.textColorSecondary, 0);
            mHighlightTextColor = context.getResources().getColor(R.color.searchColorHighlight);
            filterWithQuery("");
        }

        public void filterWithQuery(String query) {
            List<Pair<ServerConnectionInfo, String>> ret = new ArrayList<>();
            for (ServerConnectionInfo info : mConnectionManager.getConnections()) {
                for (String channel : info.getChannels()) {
                    int iof = channel.indexOf(query);
                    if (iof != -1)
                        ret.add(new Pair<>(info, channel));
                }
            }
            Collections.sort(ret, (Pair<ServerConnectionInfo, String> l,
                                   Pair<ServerConnectionInfo, String> r) ->
                    Integer.compare(l.second.indexOf(query), r.second.indexOf(query)));
            mHighlightQuery = query;
            setItems(ret);
        }

        public class SuggestionHolder extends ClickableRecyclerViewAdapter.ViewHolder<Pair<ServerConnectionInfo, String>> {
            private TextView mText;

            public SuggestionHolder(View itemView) {
                super(itemView);
                mText = itemView.findViewById(R.id.text);
            }

            @Override
            public void bind(Pair<ServerConnectionInfo, String> item) {
                String name = item.first.getName();
                String channel = item.second;
                int iof = channel.indexOf(mHighlightQuery);
                SpannableString str = new SpannableString(channel + "  " + name);
                str.setSpan(new ForegroundColorSpan(mHighlightTextColor), iof, iof + mHighlightQuery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                str.setSpan(new ForegroundColorSpan(mSecondaryTextColor), channel.length() + 2, channel.length() + 2 + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mText.setText(str);
            }
        }

    }

    public interface ChannelSelectedListener {

        void onChannelSelected(ServerConnectionInfo server, String channel);

    }

}
