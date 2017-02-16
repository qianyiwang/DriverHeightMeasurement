package com.example.qianyiwang.driverheightmeasurement;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by wangqianyi on 2016-11-21.
 */
public class MotionService extends Service implements SensorEventListener {

    //Sensor variable
    Sensor senAccelerometer, senGravity;
    SensorManager mSensorManager;
    float[] gravity, linear_acceleration, velocity;
    private float timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
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
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with the high-pass filter.
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];
            }
        }
        GlobalVals.abs_v = calculateVelocity(event.timestamp-timestamp);
        Log.e("abs_v",GlobalVals.abs_v+"");
        timestamp = event.timestamp;

    }

    private float calculateVelocity(float t){
        float v_x = velocity[0]+linear_acceleration[0]*t;
        float v_y = velocity[1]+linear_acceleration[1]*t;
        float v_z = velocity[2]+linear_acceleration[2]*t;
        return (float) Math.sqrt(Math.pow(v_x, 2) + Math.pow(v_y, 2) + Math.pow(v_z, 2));
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

}
