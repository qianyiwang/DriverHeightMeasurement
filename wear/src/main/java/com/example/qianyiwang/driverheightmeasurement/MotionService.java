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

/**
 * Created by wangqianyi on 2016-11-21.
 */
public class MotionService extends Service implements SensorEventListener, MessageApi.MessageListener {

    //Sensor variable
    private final String TAG = "MotionService";
    Sensor senAccelerometer, senGravity;
    SensorManager mSensorManager;
    float[] gravity, linear_acceleration, velocity;
    private float timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float dT, time;
    private float calibrate;
    int i = 0;

    public static String MESSAGE_PATH = "/from-phone";
    GoogleApiClient apiClient;
    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency
        mSensorManager.registerListener(this, senGravity , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency

        gravity = new float[3];
        linear_acceleration = new float[3];
        velocity = new float[3];
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        linear_acceleration[0] = 0;
        linear_acceleration[1] = 0;
        linear_acceleration[2] = 0;
        velocity[0] = 0;
        velocity[1] = 0;
        velocity[2] = 0;

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
        if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];
        }
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(timestamp!=0){
                final float alpha = 0.8f;
                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with the high-pass filter.
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];
//                float abs_acceleration = (float) Math.sqrt(Math.pow(linear_acceleration[0], 2) + Math.pow(linear_acceleration[1], 2) + Math.pow(linear_acceleration[2], 2));
//                Log.e(TAG,abs_acceleration+","+linear_acceleration[0]+","+linear_acceleration[1]+","+linear_acceleration[2]);
            }
        }
        dT = (event.timestamp - timestamp) * NS2S;
        if(GlobalVals.msg!=""){
            if(i!=0){
                if(i==1){
                    calibrate = calculateVelocity(dT);
                }
                else{
                    time = time+dT;
                    if(time<=GlobalVals.time_target){
                        GlobalVals.z_v = Math.abs(calculateVelocity(dT)-calibrate);
                        GlobalVals.distance = calculateDistance(dT);
                        Log.e(TAG,GlobalVals.distance+","+GlobalVals.z_v+","+linear_acceleration[2]);
                    }
                    else{
                        i = 0;
                        GlobalVals.msg = "";
                        time = 0;


                    }
                }
            }
            i++;
        }
        timestamp = event.timestamp;

    }

    private float calculateVelocity(float t){
        velocity[0] = velocity[0]+linear_acceleration[0]*t;
        velocity[1] = velocity[1]+linear_acceleration[1]*t;
        velocity[2] = velocity[2]+linear_acceleration[2]*t;
//        return (float) Math.sqrt(Math.pow(velocity[0], 2) + Math.pow(velocity[1], 2) + Math.pow(velocity[2], 2));
        return velocity[2];
    }

    private float calculateDistance(float t){
        return GlobalVals.distance+GlobalVals.z_v*t;
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
        }
    }
}
