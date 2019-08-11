package com.example.safeonroad;
//created by Victoria am 11.08.2019 _ can be modified or transferred in another class
//Permissions and service are registered in the manifest
import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class LocationService extends Service implements LocationListener {
    private IBinder iBinder;

//further comes everything that concerns Location
    protected LocationManager locationManager;
    private LocationListener listener;
    //the min distance to check for updates im meters
    private static final long MIN_DISTANCE_CHECK_FOR_UPDATES = 1; //10 meters Location will update every 10 meters. Only after the user have moved the location will be updated
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000; // check updates every 1 sec (1000*1)
    private Location location;
    private Boolean GPSisEnabled;

    public void onCreate(){
    }

    public void getLocation(){
        try {
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            String provider = LocationManager.GPS_PROVIDER;
            GPSisEnabled = locationManager.isProviderEnabled(provider);
            if (GPSisEnabled) {
                //checking Permissions on a run time
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //request missing permissions - for details: ActivityCompat requestPermissions
                }

                //permissions are checked => now request Location Updates:
               locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHECK_FOR_UPDATES, this);
                //location = locationManager.getLastKnownLocation(provider);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        return Service.START_NOT_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            this.location = location;
            double currentSpeed = (location.getSpeed()*3600)/1000;
            if (currentSpeed >15){
                //trigger the action => NotDisturb-Modus on (pending Intent with:
                initSafeOnRoad();
            }

        }

    }

    private void initSafeOnRoad() {
        //
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
 //write a new intent??? which will send a user to the setting panel, where the user can enable GPS
    }
}
