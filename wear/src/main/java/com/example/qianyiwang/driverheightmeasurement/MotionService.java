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
    Sensor senAccelerometer, senGravity, senMagnetic;
    SensorManager mSensorManager;
    float[] gravity, linear_acceleration, velocity, mRotationMatrix;
    private float zAccCurrent; // current acceleration including gravity
    private float zAccLast; // last acceleration including gravity
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
//        senGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        senMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);//adjust the frequency
//        mSensorManager.registerListener(this, senGravity , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency
        mSensorManager.registerListener(this, senMagnetic , SensorManager.SENSOR_DELAY_FASTEST);

        gravity = new float[3];
        linear_acceleration = new float[3];
        velocity = new float[3];
        mRotationMatrix = new float[9];
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
//        if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
//            gravity[0] = event.values[0];
//            gravity[1] = event.values[1];
//            gravity[2] = event.values[2];
//        }

        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
//            Log.e("rotation", mRotationMatrix[6]+","+mRotationMatrix[7]+","+mRotationMatrix[8]);
        }

        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(timestamp!=0){
                final float alpha = 0.8f;

                linear_acceleration[0] = event.values[0] * mRotationMatrix[0] + event.values[1] * mRotationMatrix[1] + event.values[2] * mRotationMatrix[2];
                linear_acceleration[1] = event.values[0] * mRotationMatrix[3] + event.values[1] * mRotationMatrix[4] + event.values[2] * mRotationMatrix[5];
                linear_acceleration[2] = event.values[0] * mRotationMatrix[6] + event.values[1] * mRotationMatrix[7] + event.values[2] * mRotationMatrix[8];
                linear_acceleration[0] /= SensorManager.GRAVITY_EARTH;
                linear_acceleration[1] /= SensorManager.GRAVITY_EARTH;
                linear_acceleration[2] = (linear_acceleration[2] - SensorManager.GRAVITY_EARTH) / SensorManager.GRAVITY_EARTH;
//                // Isolate the force of gravity with the low-pass filter.
//                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//
//                // Remove the gravity contribution with the high-pass filter.
//                linear_acceleration[0] = event.values[0] - gravity[0];
//                linear_acceleration[1] = event.values[1] - gravity[1];
//                linear_acceleration[2] = event.values[2] - gravity[2];
//                zAccLast = zAccCurrent;
//                zAccCurrent = linear_acceleration[2];//(float) Math.sqrt(acc_x * acc_x + acc_y * acc_y + acc_z * acc_z);
//                float delta = zAccCurrent - zAccLast;
//                linear_acceleration[2] = delta*linear_acceleration[2] + (1-delta)*linear_acceleration[2]; // perform low-cut filter

            }
        }
        dT = (event.timestamp - timestamp) * NS2S;
        if(GlobalVals.msg!=""){
            if(i>=60){
//                if(i==61){
//                    calibrate = linear_acceleration[2];
//                }

                time = time+dT;
                if(time<=GlobalVals.time_target){
                    GlobalVals.z_v = calculateVelocity(dT);
                    GlobalVals.distance = calculateDistance(dT);
                    Log.e(TAG,GlobalVals.distance+","+GlobalVals.z_v+","+linear_acceleration[2]);
                }
                else{
                    i = 0;
                    GlobalVals.msg = "";
                    time = 0;
                    GlobalVals.distance = 0;
                    GlobalVals.z_v = 0;
                    GlobalVals.vibrator.vibrate(50);
                }

            }
            i++;
        }
        timestamp = event.timestamp;

    }

    private float calculateVelocity(float t){
//        velocity[0] = velocity[0]+linear_acceleration[0]*t;
//        velocity[1] = velocity[1]+linear_acceleration[1]*t;
//        if(linear_acceleration[2]>=-0.5&&linear_acceleration[2]<=0.5){
//            linear_acceleration[2] = 0;
//        }
        velocity[2] = velocity[2]+(linear_acceleration[2])*t;
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
            GlobalVals.vibrator.vibrate(50);
        }
    }
}
