package io.mrarm.irc.setup;

import android.content.Intent;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AppCompatActivity;

import io.mrarm.irc.R;

public class SetupActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_NEXT_STEP = 10001;
    public static final int RESULT_CODE_FINISHED = 10001;

    @Override
    public void finish() {
        super.finish();
        setSlideAnimation(true);
    }

    protected void setSlideAnimation(boolean fromRight) {
        if (ViewCompat.getLayoutDirection(getWindow().getDecorView())
                == ViewCompat.LAYOUT_DIRECTION_RTL)
            fromRight = !fromRight;

        if (fromRight)
            overridePendingTransition(R.anim.slide_rtl_enter, R.anim.slide_rtl_exit);
        else
            overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    public void setSetupFinished() {
        setResult(RESULT_CODE_FINISHED);
        finish();
    }

    public void startNextActivity(Intent intent) {
        startActivityForResult(intent, REQUEST_CODE_NEXT_STEP);
        setSlideAnimation(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEXT_STEP && resultCode == RESULT_CODE_FINISHED)
            setSetupFinished();
        super.onActivityResult(requestCode, resultCode, data);
    }

}
