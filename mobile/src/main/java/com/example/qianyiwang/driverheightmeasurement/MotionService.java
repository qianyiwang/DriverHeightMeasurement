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
        Sensor acc_linearSensor, rotationSensor;
        private float[] rotationMatrix = new float[9];
        private float[] mLastAccelerometer = new float[3];
        private float[] mLastMagnetometer = new float[3];

        MySensorEvent(){
            sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            acc_linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sensorManager.registerListener(this, acc_linearSensor , SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, rotationSensor , SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.sensor==acc_linearSensor){

            }
            else if(sensorEvent.sensor==rotationSensor){
                SensorManager.getRotationMatrix(rotationMatrix, null, mLastAccelerometer, mLastMagnetometer);
                Log.e(TAG, "rotation: "+rotationMatrix[6] + " " + rotationMatrix[7] + " " + rotationMatrix[8]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        public void stopSensor(){
            sensorManager.unregisterListener(this);
        }
    }

}
