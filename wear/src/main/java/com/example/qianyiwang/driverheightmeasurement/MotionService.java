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
    float[] acceleration, velocity, mRotationMatrix, acc_last;
    private float timestamp, last_accZ;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float dT, time;
    LinkedList<Float> distArr;
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
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency
//        mSensorManager.registerListener(this, senGravity , SensorManager.SENSOR_DELAY_FASTEST);//adjust the frequency
        mSensorManager.registerListener(this, senMagnetic , SensorManager.SENSOR_DELAY_FASTEST);

        acceleration = new float[3];
        velocity = new float[3];
        mRotationMatrix = new float[9];
        acc_last = new float[3];
        acceleration[0] = 0;
        acceleration[1] = 0;
        acceleration[2] = 0;

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
                final float alpha = 0.25f;

                acceleration[0] = event.values[0] * mRotationMatrix[0] + event.values[1] * mRotationMatrix[1] + event.values[2] * mRotationMatrix[2];
                acceleration[1] = event.values[0] * mRotationMatrix[3] + event.values[1] * mRotationMatrix[4] + event.values[2] * mRotationMatrix[5];
                acceleration[2] = event.values[0] * mRotationMatrix[6] + event.values[1] * mRotationMatrix[7] + event.values[2] * mRotationMatrix[8];
                acceleration[2] = acceleration[2] - SensorManager.GRAVITY_EARTH;
//                acceleration[0] /= SensorManager.GRAVITY_EARTH;
//                acceleration[1] /= SensorManager.GRAVITY_EARTH;
//                acceleration[2] = (acceleration[2] - SensorManager.GRAVITY_EARTH) / SensorManager.GRAVITY_EARTH;
//                Log.e(TAG, acceleration[0] + ","+ acceleration[1] + "," + acceleration[2]);
                // using ramp as filter
//                acc_last[0] = acceleration[0] * alpha + acc_last[0] * (1.0f - alpha);
//                acc_last[1] = acceleration[1] * alpha + acc_last[1] * (1.0f - alpha);
//                acc_last[2] = acceleration[2] * alpha + acc_last[2] * (1.0f - alpha);
//                acceleration[0] = acceleration[0] - acc_last[0];
//                acceleration[1] = acceleration[1] - acc_last[1];
//                acceleration[2] = acceleration[2] - acc_last[2];

//                 using low pass filter: y[i] := y[i-1] + α * (x[i] - y[i-1])
                acceleration[2] = last_accZ + alpha * (acceleration[2] - last_accZ);
                last_accZ = acceleration[2];

            }
        }
        dT = (event.timestamp - timestamp) * NS2S;
        if(GlobalVals.msg!=""){
            if(i>=60){
//                if(i==61){
//                    calibrate = acceleration[2];
//                }

                time = time+dT;
                if(time<=GlobalVals.time_target){
                    GlobalVals.z_v = calculateVelocity(dT);
                    GlobalVals.distance = calculateDistance(dT);
                    distArr.add(GlobalVals.distance);
                    Log.e(TAG,GlobalVals.distance+","+GlobalVals.z_v+","+acceleration[2]);
                }
                else{
                    float dist = postAnaly();
                    Log.e(TAG, "distance:"+dist);
                    initialVals();
                }

            }
            i++;
        }
        timestamp = event.timestamp;

    }

    private float postAnaly(){
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
        velocity[2] = velocity[2]+(acceleration[2])*t;
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
