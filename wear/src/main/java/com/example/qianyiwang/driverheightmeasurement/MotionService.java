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


/**
 * Created by wangqianyi on 2016-11-21.
 */
public class MotionService extends Service {

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
                Log.e(TAG, "rotation: "+rotationMatrix[6] + " " + rotationMatrix[7] + " " + rotationMatrix[8]);
            }

            if(sensorEvent.sensor==accSensor){

//                using low pass filter: y[i] := y[i-1] + α * (x[i] - y[i-1])
                for (int i=0; i<2; i++){
                    sensorEvent.values[i] = last_acc[i] + alpha * (sensorEvent.values[i] - last_acc[i]);
                    last_acc[i] = sensorEvent.values[i];
                }

                calculateGlobalOrientationValue(sensorEvent.values);
            }
        }

        private float[] calculateGlobalOrientationValue(float[] data){

            return null;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        public void stopSensor(){
            sensorManager.unregisterListener(this);
        }
    }

}
