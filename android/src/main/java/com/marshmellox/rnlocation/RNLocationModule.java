package com.marshmelloX.rnlocation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNLocationModule extends ReactContextBaseJavaModule {
    // React Class Name as called from JS
    public static final String REACT_CLASS = "RNLocation";
    // Unique Name for Log TAG
    public static final String TAG = RNLocationModule.class.getSimpleName();
    // Location settings
    private double distanceFilter;
    private double desiredAccuracy;
    private double headingFilter;
    // Heading specific
    private float mAzimuth;
    private float[] orientation;
    private float[] rMat;
    private String sensorDelay;
    private Sensor mSensor;
    private SensorEventListener mSensorListener;
    private SensorManager mSensorManager;
    // Location specific
    private String locationProvider;
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private LocationManager locationManager;

    //The React Native Context
    ReactApplicationContext mReactContext;

    // Constructor Method as called in Package
    public RNLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        // Save Context for later use
        mReactContext = reactContext;
        // default values
        distanceFilter = 5.0;
        desiredAccuracy = 10.0;
        headingFilter = 1.0;
        // init heading sensors
        mAzimuth = 0;
        orientation = new float[3];
        rMat = new float[9];
        sensorDelay = Sensor.SENSOR_DELAY_GAME;
        mSensorManager = (SensorManager) mReactContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // init location manager
        locationManager = (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
        locationProvider = LocationManager.NETWORK_PROVIDER;
        if (locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)) {
          locationProvider = LocationManager.GPS_PROVIDER;
        }
        mLastLocation = locationManager.getLastKnownLocation(locationProvider);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    /*
     * Location permission request (TODO)
     */
    @ReactMethod
    public void requestAlwaysAuthorization() {
        Log.i(TAG, "Requesting AlwaysAuthorization");
    }

    /*
     * Location permission request (TODO)
     */
    @ReactMethod
    public void requestWhenInUseAuthorization() {
        Log.i(TAG, "Requesting WhenInUseAuthorization");
    }

    /*
     * Set sensor delay
     */
    @ReactMethod
    public void setSensorDelay(String delay) {
        self.sensorDelay = delay;
    }

    /*
     * Set distance filter
     */
    @ReactMethod
    public void setDistanceFilter(double distance) {
        self.distanceFilter = distance;
    }

    /*
     * Set desired accuracy
     */
    @ReactMethod
    public void setDesiredAccuracy(double accuracy) {
        self.desiredAccuracy = accuracy;
    }

    /*
     * Set heading filter
     */
    @ReactMethod
    public void setHeadingFilter(double degrees) {
        self.headingFilter = degrees;
    }

    /*
     * Heading updates (TODO: use ACCELEROMETER and MAGNETIC_FIELD to detect device orientation)
     */
    @ReactMethod
    public void startUpdatingHeading() {
        mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
              if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

              // calculate th rotation matrix
              SensorManager.getRotationMatrixFromVector(rMat, event.values);

              // get the azimuth value (orientation[0]) in degree
              float newAzimuth = (((((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360) -
                  (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2]))) + 360) % 360);

              //dont react to changes smaller than the filter value
              if (Math.abs(mAzimuth - newAzimuth) < headingFilter) return;

              // Create Map with Parameters to send to JS
              WritableMap params = Arguments.createMap();
              params.putDouble("heading", newAzimuth);
              // Send Event to JS to update Location
              sendEvent(mReactContext, "headingUpdated", params);

              mAzimuth = newAzimuth;
            }
        };
        mSensorManager.registerListener(mSensorListener, mSensor, sensorDelay);
    }

    @ReactMethod
    public void stopUpdatingHeading() {
        mSensorManager.unregisterListener(mSensorListener);
    }

    /*
     * Location updates
     */
    @ReactMethod
    public void startUpdatingLocation() {
        mLocationListener = new LocationListener() {
            @Override
            public void onStatusChanged(String str,int in,Bundle bd) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onProviderDisabled(String str) {
            }

            @Override
            public void onLocationChanged(Location loc) {
                mLastLocation = loc;
                if (mLastLocation != null) {
                  try {
                    double longitude;
                    double latitude;
                    double speed;
                    double altitude;
                    double accuracy;
                    double course;
                    double timestamp;

                    // Receive Longitude / Latitude from (updated) Last Location
                    longitude = mLastLocation.getLongitude();
                    latitude = mLastLocation.getLatitude();
                    speed = mLastLocation.getSpeed();
                    altitude = mLastLocation.getAltitude();
                    accuracy = mLastLocation.getAccuracy();
                    course = mLastLocation.getBearing();
                    timestamp = System.currentTimeMillis();

                    if (accuracy < desiredAccuracy) return;

                    Log.i(TAG, "Got new location. Lng: " +longitude+" Lat: "+latitude);

                   // Create Map with Parameters to send to JS
                    WritableMap params = Arguments.createMap();
                    params.putDouble("longitude", longitude);
                    params.putDouble("latitude", latitude);
                    params.putDouble("speed", speed);
                    params.putDouble("altitude", altitude);
                    params.putDouble("accuracy", accuracy);
                    params.putDouble("course", course);
                    params.putDouble("timestamp", timestamp);

                    // Send Event to JS to update Location
                    sendEvent(mReactContext, "locationUpdated", params);
                  } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "Location services disconnected.");
                  }
              }
            }
        };

        locationProvider = LocationManager.NETWORK_PROVIDER;

        if (locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)) {
          locationProvider = LocationManager.GPS_PROVIDER;
        }

        locationManager.requestLocationUpdates(locationProvider, distanceFilter, 1, mLocationListener);
    }

    @ReactMethod
    public void stopUpdatingLocation() {
        try {
          locationManager.removeUpdates(mLocationListener);
          Log.i(TAG, "Location service disabled.");
        } catch(Exception e) {
          e.printStackTrace();
        }
    }

    /*
     * Internal function for communicating with JS
     */
    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
              .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
              .emit(eventName, params);
        } else {
            Log.i(TAG, "Waiting for CatalystInstance...");
        }
    }
}
