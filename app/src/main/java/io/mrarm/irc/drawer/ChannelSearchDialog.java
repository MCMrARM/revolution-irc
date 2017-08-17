package io.mrarm.irc.drawer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.dialog.SearchDialog;
import io.mrarm.irc.util.ClickableRecyclerViewAdapter;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ChannelSearchDialog extends SearchDialog {

    private SuggestionsAdapter mAdapter;

    public ChannelSearchDialog(@NonNull Context context) {
        super(context);
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context,
                new int[] { R.attr.colorBackgroundFloating, android.R.attr.textColorSecondary });
        setBackgroundColor(ta.getColor(R.attr.colorBackgroundFloating, 0));
        int secondaryTextColor = ta.getColor(android.R.attr.textColorSecondary, 0);
        ta.recycle();
        int highlightTextColor = context.getResources().getColor(R.color.searchColorHighlight);

        mAdapter = new SuggestionsAdapter(secondaryTextColor, highlightTextColor);
        mAdapter.setItemClickListener((int index, Pair<ServerConnectionInfo, String> value) ->{
            ((MainActivity) getOwnerActivity()).openServer(value.first, value.second);
            dismiss();
        });
        setSuggestionsAdapter(mAdapter);
        onQueryTextChange("");
    }

    @Override
    public void onQueryTextSubmit(String query) {
    }

    @Override
    public void onQueryTextChange(String newText) {
        List<Pair<ServerConnectionInfo, String>> ret = new ArrayList<>();
        for (ServerConnectionInfo info : ServerConnectionManager.getInstance(getContext())
                .getConnections()) {
            for (String channel : info.getChannels()) {
                int iof = channel.indexOf(newText);
                if (iof != -1)
                    ret.add(new Pair<>(info, channel));
            }
        }
        Collections.sort(ret, (Pair<ServerConnectionInfo, String> l,
                               Pair<ServerConnectionInfo, String> r) ->
                Integer.compare(l.second.indexOf(newText), r.second.indexOf(newText)));
        mAdapter.setItems(newText, ret);
    }

    public static class SuggestionsAdapter extends ClickableRecyclerViewAdapter<SuggestionsAdapter.SuggestionHolder, Pair<ServerConnectionInfo, String>> {

        private String mHighlightQuery;
        private int mSecondaryTextColor;
        private int mHighlightTextColor;

        public SuggestionsAdapter(int secondaryTextColor, int highlightTextColor) {
            setViewHolderFactory(SuggestionHolder::new, R.layout.dialog_search_item);
            mSecondaryTextColor = secondaryTextColor;
            mHighlightTextColor = highlightTextColor;
        }

        public void setItems(String query, List<Pair<ServerConnectionInfo, String>> items) {
            mHighlightQuery = query;
            setItems(items);
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

}
