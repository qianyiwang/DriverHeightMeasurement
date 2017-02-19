package com.example.qianyiwang.driverheightmeasurement;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

public class MainAPP extends Activity {

    TextView abs_v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main_app);

        abs_v = (TextView)findViewById(R.id.text);
        GlobalVals.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(getBaseContext(), MotionService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(getBaseContext(), MotionService.class));
    }
}
