package io.mrarm.irc.newui;

import android.os.Bundle;

import io.mrarm.irc.R;
import io.mrarm.irc.dagger.DaggerThemedActivity;
import io.mrarm.irc.newui.settings.GroupReorderFragment;

public class MainActivity extends DaggerThemedActivity {

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

        mToolbar.setNavigationButtonAction(() -> {
            if (mContainer.getFragmentCount() > 1)
                mContainer.popAnim();
        });

        //mContainer.push(new MainFragment());
        mContainer.push(new GroupReorderFragment());
    }

    public SlideableFragmentContainer getContainer() {
        return mContainer;
    }

    @Override
    public void onBackPressed() {
        if (mContainer.getFragmentCount() > 1) {
            mContainer.popAnim();
            return;
        }
        super.onBackPressed();
    }
}
