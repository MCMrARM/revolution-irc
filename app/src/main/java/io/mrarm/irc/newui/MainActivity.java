package io.mrarm.irc.newui;

import android.os.Bundle;

import io.mrarm.irc.R;
import io.mrarm.irc.ThemedActivity;

public class MainActivity extends ThemedActivity {

    private SlideableFragmentContainer mContainer;
    private SlideableFragmentToolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newui_main);

        mContainer = findViewById(R.id.container);
        mContainer.setFragmentManager(getSupportFragmentManager());

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setContainer(getSupportFragmentManager(), mContainer);

        mContainer.push(new MainFragment());
    }

    public SlideableFragmentContainer getContainer() {
        return mContainer;
    }


}
