package com.example.qianyiwang.driverheightmeasurement;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by wangqianyi on 2016-11-21.
 */
public class MotionService extends Service implements SensorEventListener, MessageApi.MessageListener {

    //Sensor variable
    private final String TAG = "MotionService";
    Sensor senAccelerometer, senGravity, senMagnetic;
    SensorManager mSensorManager;
    float[] velocity, mRotationMatrix, acc_last;
    private float timestamp, abs_acc;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float dT, time;
    private float calibrate;
    int i = 0;
    LinkedList<Float> distArr;

    public static String MESSAGE_PATH = "/from-phone";
    GoogleApiClient apiClient;
    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        senGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency
//        mSensorManager.registerListener(this, senGravity , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency

        velocity = new float[3];
        mRotationMatrix = new float[9];
        acc_last = new float[3];
        distArr = new LinkedList<>();

        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        apiClient.connect();
        Wearable.MessageApi.addListener(apiClient, this);//very important
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        Toast.makeText(this,"stop motion service",0).show();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            if(timestamp!=0){
                final float alpha = 0.25f;
                // using low pass filter: y[i] := y[i-1] + α * (x[i] - y[i-1])
                float acc_x = acc_last[0] + alpha * (event.values[0] - acc_last[0]);
                float acc_y = acc_last[1] + alpha * (event.values[1] - acc_last[1]);
                float acc_z = acc_last[2] + alpha * (event.values[2] - acc_last[2]);

                acc_last[0] = acc_x;
                acc_last[1] = acc_y;
                acc_last[2] = acc_z;

                abs_acc = (float) Math.sqrt(acc_x*acc_x+acc_y*acc_y+acc_z*acc_z);

            }
        }
        dT = (event.timestamp - timestamp) * NS2S;
        if(GlobalVals.msg!=""){
            if(i>30){
//                if(i==61){
//                    calibrate = acceleration[2];
//                }

                time = time+dT;
                if(time<=GlobalVals.time_target){
                    GlobalVals.z_v = calculateVelocity(dT);
                    GlobalVals.distance = calculateDistance(dT);
                    distArr.add(GlobalVals.distance);
                    Log.e(TAG,GlobalVals.distance+","+GlobalVals.z_v+","+abs_acc);
                }
                else{
                    float max = postAnalysis();
                    Log.e(TAG, "max:"+max);
                    initialVals();
                }

            }
            i++;
        }
        timestamp = event.timestamp;

    }

    private float postAnalysis(){
        Float max = Collections.max(distArr);
        return max;
    }

    private void initialVals(){
        i = 0;
        GlobalVals.msg = "";
        time = 0;
        GlobalVals.distance = 0;
        GlobalVals.z_v = 0;
        GlobalVals.vibrator.vibrate(50);
        velocity[0] = 0;
        velocity[1] = 0;
        velocity[2] = 0;
        acc_last[0] = 0;
        acc_last[1] = 0;
        acc_last[2] = 0;
        distArr.clear();
    }

    private float calculateVelocity(float t){
        velocity[2] = velocity[2]+(abs_acc)*t;
        return velocity[2];
    }

    private float calculateDistance(float t){
        return GlobalVals.distance+velocity[2]*t;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this,"start motion service",0).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equalsIgnoreCase(MESSAGE_PATH)){
            GlobalVals.msg = new String(messageEvent.getData());
            Log.e(TAG, GlobalVals.msg);
            GlobalVals.vibrator.vibrate(50);
        }
    }
}
