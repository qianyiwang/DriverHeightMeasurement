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
        Sensor accSensor, gravitySensor;
        private float[] last_acc = new float[3];
        private float[] rotationMatrix = new float[9];
        private float[] gravityVal;
        private final float alpha = 0.8f;
        float[] mSensorBias;
        float[] mSensorDelta;
        float[] mSpeedBias;
        float[] mSpeedDelta;
        final float[] SENSOR_BIAS_STEP;
        final float[] SENSOR_ZERO_RANGE;
        final float[] SPEED_BIAS_STEP;
        final float[] SPEED_ZERO_RANGE;
        float[] speed;
        float[] pos;
        boolean gravity_flag = false;
        private static final float NS2S = 1.0f / 1000000000.0f;

        MySensorEvent(){
            sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            sensorManager.registerListener(this, accSensor , SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gravitySensor , SensorManager.SENSOR_DELAY_GAME);
            mSensorBias = new float[]{0, 0, 0};
            mSensorDelta = new float[]{0, 0, 0};
            mSpeedDelta = new float[]{0,0,0};
            mSpeedBias = new float[]{0,0,0};
            SENSOR_BIAS_STEP = new float[]{0.01f,0.01f,0.01f};
            SENSOR_ZERO_RANGE = new float[]{0.2f,0.2f,0.2f};
            SPEED_BIAS_STEP = new float[] {.5f,.5f,.5f};
            SPEED_ZERO_RANGE = new float[] {0.05f,0.05f,0.05f};
            speed = new float[]{0, 0, 0};
            pos = new float[]{0, 0, 0};
            gravityVal = new float[]{0, 0, 0};
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            if(sensorEvent.sensor == gravitySensor){
                gravityVal = sensorEvent.values;
                gravity_flag = true;
            }

            else if(sensorEvent.sensor==accSensor && gravity_flag){
                // Isolate the force of gravity with the low-pass filter.
                gravityVal[0] = alpha * gravityVal[0] + (1 - alpha) * sensorEvent.values[0];
                gravityVal[1] = alpha * gravityVal[1] + (1 - alpha) * sensorEvent.values[1];
                gravityVal[2] = alpha * gravityVal[2] + (1 - alpha) * sensorEvent.values[2];
                for (int i=0; i<3; i++) {
                    mSensorDelta[i] = sensorEvent.values[i]-gravityVal[i]-mSensorBias[i];
                    if (mSensorDelta[i] > 0) {
                        if (mSensorDelta[i] > SENSOR_BIAS_STEP[i]) {
                            mSensorBias[i] += SENSOR_BIAS_STEP[i];
                        } else {
                            mSensorBias[i] += (sensorEvent.values[i]-gravityVal[i]);
                        }
                    }
                    else {
                        if (mSensorDelta[i] < -SENSOR_BIAS_STEP[i]) {
                            mSensorBias[i] -= SENSOR_BIAS_STEP[i];
                        } else {
                            mSensorBias[i] -= (sensorEvent.values[i]-gravityVal[i]);
                        }
                    }
                    mSensorDelta[i] = (sensorEvent.values[i]-gravityVal[i])-mSensorBias[i];

                    if ((mSensorDelta[i] < SENSOR_ZERO_RANGE[i]) && (mSensorDelta[i] > -SENSOR_ZERO_RANGE[i])) {
                        mSensorDelta[i] = 0;
                    }
                }

                for (int i=0; i<3; i++) {
                    speed[i] += mSensorDelta[i];

                    mSpeedDelta[i] = speed[i]-mSpeedBias[i];
                    if (mSpeedDelta[i] > 0) {
                        if (mSpeedDelta[i] > SPEED_BIAS_STEP[i]) {
                            mSpeedBias[i] += SPEED_BIAS_STEP[i];
                        } else {
//                            mSpeedBias[i] += speed[i];
                        }
                    }
                    else {
                        if (mSpeedDelta[i] < -SPEED_BIAS_STEP[i]) {
                            mSpeedBias[i] -= SPEED_BIAS_STEP[i];
                        } else {
//                            mSpeedBias[i] -= speed[i];
                        }
                    }
                    mSpeedDelta[i] = speed[i]-mSpeedBias[i];

                    if ((mSpeedDelta[i] < SPEED_ZERO_RANGE[i]) && (mSpeedDelta[i] > -SPEED_ZERO_RANGE[i])) {
                        mSpeedDelta[i] = 0;
                    }

                    pos[i] += mSpeedDelta[i];
                }

//                    double acc_magnitude = Math.sqrt(mSensorDelta[0]*mSensorDelta[0]+mSensorDelta[1]*mSensorDelta[1]+mSensorDelta[2]*mSensorDelta[2]);
//                    calculateSpeed();
//                    calculateDistance();
//                Log.e(TAG, mSensorDelta[0]+","+mSensorDelta[1]+","+mSensorDelta[2]/*+","
//                            +(sensorEvent.values[0]-gravityVal[0])+","+(sensorEvent.values[1]-gravityVal[1])+","+(sensorEvent.values[2]-gravityVal[2])*/);
                Log.e(TAG, mSensorDelta[0]+",     "+mSensorDelta[1]+",     "+mSensorDelta[2]+",    "
                            +mSpeedDelta[0]+",     "+mSpeedDelta[1]+",    "+mSpeedDelta[2]+",    "+mSpeedBias[0]+",   "+ mSpeedBias[1]+",   "+ mSpeedBias[2]);
            }
        }

        private void calculateSpeed(){
            for(int i=0; i<3; i++){
                speed[i] += mSensorDelta[i];
            }
        }

        private void calculateDistance(){
            for(int i=0; i<3; i++){
                pos[i] += speed[i];
            }
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
