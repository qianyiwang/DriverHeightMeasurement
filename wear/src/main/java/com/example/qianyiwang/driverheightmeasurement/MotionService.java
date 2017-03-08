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

            }public class MotionService extends Service {

                private final String TAG = "MotionService";
                MySensorEvent mySensorEvent;

                @Override
                public void onCreate() {
                    super.onCreate();
                    mySensorEvent = new MySensorEvent();
                    Toast.makeText(this, "motion sensor on", 0).show();
                }

                @Override
                public void onDestroy() {
                    super.onDestroy();
                    mySensorEvent.stopSensor();
                    Toast.makeText(this, "motion sensor off", 0).show();
                }

                @Nullable
                @Override
                public IBinder onBind(Intent intent) {
                    return null;
                }

                private class MySensorEvent implements SensorEventListener{

                    SensorManager sensorManager;
                    Sensor accSensor, rotationSensor;
                    private float[] last_acc = new float[3];
                    private float[] rotationMatrix = new float[9];
                    private final float alpha = 0.25f;
                    private int accuracy = 0;

                    MySensorEvent(){
                        sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
                        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                        sensorManager.registerListener(this, accSensor , SensorManager.SENSOR_DELAY_NORMAL);
                        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }

                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {

                        if(sensorEvent.sensor == rotationSensor){

                            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);
                            accuracy = sensorEvent.accuracy;

//                Log.e(TAG, "rotation: "+rotationMatrix[6] + " " + rotationMatrix[7] + " " + rotationMatrix[8]);
                        }

                        else if(sensorEvent.sensor==accSensor){

//                using low pass filter: y[i] := y[i-1] + α * (x[i] - y[i-1])
                            for (int i=0; i<2; i++){
                                sensorEvent.values[i] = last_acc[i] + alpha * (sensorEvent.values[i] - last_acc[i]);
                                last_acc[i] = sensorEvent.values[i];
                            }

                            float[] acc_global = calculateGlobalOrientationValue(sensorEvent.values);
                            if(acc_global[2]<0){
                                acc_global[2] += SensorManager.GRAVITY_EARTH;
                            }
                            else{
                                acc_global[2] -= SensorManager.GRAVITY_EARTH;
                            }
                            acc_global = calibrationFilter(acc_global);
                            Log.e(TAG, "acc_global: "+acc_global[0] + " " + acc_global[1] + " " + acc_global[2] + "accuracy: "+accuracy);
                        }
                    }

                    private float[] calibrationFilter(float[] data){
                        float[] mSensorBias = {0,0,0};
                        float[] mSensorDelta = {0,0,0};
                        final float[] SENSOR_BIAS_STEP = {0.01f,0.01f,0.02f};
                        final float[] SENSOR_ZERO_RANGE = {0.2f,0.2f,0.4f};
                        for (int i=0; i<3; i++) {
                            mSensorDelta[i] = data[i]-mSensorBias[i];
                            if (mSensorDelta[i] > 0) {
                                if (mSensorDelta[i] > SENSOR_BIAS_STEP[i]) {
                                    mSensorBias[i] += SENSOR_BIAS_STEP[i];
                                } else {
                                    mSensorBias[i] += data[i];
                                }
                            }
                            else {
                                if (mSensorDelta[i] < -SENSOR_BIAS_STEP[i]) {
                                    mSensorBias[i] -= SENSOR_BIAS_STEP[i];
                                } else {
                                    mSensorBias[i] -= data[i];
                                }
                            }
                            mSensorDelta[i] = data[i]-mSensorBias[i];

                            if ((mSensorDelta[i] < SENSOR_ZERO_RANGE[i]) && (mSensorDelta[i] > -SENSOR_ZERO_RANGE[i])) {
                                mSensorDelta[i] = 0;
                            }
                        }
                        return mSensorDelta;
                    }

                    private float[] calculateGlobalOrientationValue(float[] data){
                        float[] result = new float[3];
                        result[0] = data[0] * rotationMatrix[0] + data[1] * rotationMatrix[1] + data[2] * rotationMatrix[2];
                        result[1] = data[0] * rotationMatrix[3] + data[1] * rotationMatrix[4] + data[2] * rotationMatrix[5];
                        result[2] = data[0] * rotationMatrix[6] + data[1] * rotationMatrix[7] + data[2] * rotationMatrix[8];
                        return result;
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }

                    public void stopSensor(){
                        sensorManager.unregisterListener(this);
                    }
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
