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
