package com.yushan.eagledemo.track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yushan.eagledemo.R;
import com.yushan.eagledemo.track.utils.Constants;


/**
 * 启动页
 */
public class SplashActivity extends BaseActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Constants.SPLASH_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "thread sleep failed");
                } finally {
                    turnToMain();
                }
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private void turnToMain() {
        Intent intent = new Intent(this, MainActivity.class);
//        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_from_right, R.anim.keep);
        //        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_splash;
    }

}
