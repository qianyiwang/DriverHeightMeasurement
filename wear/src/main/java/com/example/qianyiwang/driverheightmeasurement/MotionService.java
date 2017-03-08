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
        float[] speed = new float[3];
        float[] pos = new float[3];

        MySensorEvent(){
            sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accSensor , SensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            if(sensorEvent.sensor==accSensor){
                float[] acc_global;

//                using low pass filter: y[i] := y[i-1] + α * (x[i] - y[i-1])
                for (int i=0; i<2; i++){
                    sensorEvent.values[i] = last_acc[i] + alpha * (sensorEvent.values[i] - last_acc[i]);
                    last_acc[i] = sensorEvent.values[i];
                }
                acc_global = accCalibrationFilter(sensorEvent.values);
                double acc_magnitude = Math.sqrt(acc_global[0]*acc_global[0]+acc_global[1]*acc_global[1]+acc_global[2]*acc_global[2]);
            }
        }

        private float[] accCalibrationFilter(float[] data){
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

        private float[] calculateSpeed(float[] speed, float[] acc_global){
            for(int i=0; i<3; i++){
                speed[i] += acc_global[i];
            }
            return speed;
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
