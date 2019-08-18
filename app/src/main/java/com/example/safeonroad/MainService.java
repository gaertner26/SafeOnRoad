package com.example.safeonroad;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Calendar;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


// Christoph, 2019-8-12
public class MainService extends Service implements LocationListener {

    //private Location lastLocation;
    //private Location newLocation;

    private BluetoothAdapter bluetoothAdapter;
    public boolean isServiceActive = TRUE;

    /*private long manualCoolddownStartTime = 0;   //timestamp when user told the app to go to standby for 1 hr
    private long autoCooldownStartTime = 0; // timestamp when the car started to drive < 20 hm/h

    private int manualCooldownDuration = 60 * 60 * 1000;   //example: 1 hr
    private int autoCooldownDuration = 3 * 60 * 1000; //3 mins the car has to be driving < 20 km/h


    private int idBluethoothCar;
    */
    private final int MIN_SPEED = 20;

    private static final long MIN_DISTANCE_CHECK_FOR_UPDATES = 1; //10 meters Location will update every 10 meters. Only after the user have moved the location will be updated
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000;
    private LocationManager locationManager;
    private String provider;
    private Location location;
    private Boolean GPSisEnabled;

    public MainService() {
        //initialize Bluetooth
        //initBluetooth();
        //initializeLocation

        //if this service is active, he should perform every 250 ms the following action:


        /*while (isServiceActive) {



            if (velocity >= 20) {
                dontDisturb(TRUE);
            } else if (velocity < 20) {
                dontDisturb(FALSE);
            }

            //sleep(250);
            if(isBluetoothActive() && isBluetoothCarActive()){
                dontDisturb(TRUE);
            }else{
                long time = System.currentTimeMillis();
                if(System.currentTimeMillis() - manualCoolddownStartTime >= manualCooldownDuration){  //check, if app has been paused for 2 hours
                    float velocity = location.getSpeed(); //changed by Sandra 2019-08-12 14:55
                    if(velocity >= 20){ //&& getDontDisturb() == false){
                        dontDisturb(TRUE);
                        autoCooldownStartTime = 0;
                    }else if(velocity < 20){
                        if(autoCooldownStartTime == 0){   // check, if the car has been at least 2 mins slower than 20 km/h
                            autoCooldownStartTime = System.currentTimeMillis();
                        }else if(System.currentTimeMillis() - autoCooldownStartTime > autoCooldownDuration){
                            dontDisturb(FALSE);
                        }
                    }

                }
            }



        }*/

    }

    //App did break, when start-Button was clicked, this fixed the problem
    @Override
    public void onCreate(){
        initLocation();
        initBluetooth();
    }
    private void dontDisturb(boolean state){
        /*
        if(getDontDisturb() == false && state == true){
            setDontDisturb = true;
        }else if(getDontDisturb() == true && state == false){
            setDontDisturb = false;
        }
        */

    }

    public void doNotDisturbOn(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // Check if the notification policy access has been granted for the app.
        if ((Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted())) {     //ask for persmission
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= 23 && notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);      //no Interruption = Everything Blocked
        }

    }

    public void doNotDisturbOff () {
        //do-not-disturb-modus has to be turned off, if person is slower than 20 km/h for some time.
    }




    private void initBluetooth() {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
    }
    //Sandra 12.08.2019 (onStartCommand)
    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //by Sandra 2019-08-14
    private void initLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.GPS_PROVIDER;
        GPSisEnabled = locationManager.isProviderEnabled(provider);
        if(GPSisEnabled) {
            if (ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHECK_FOR_UPDATES, this);
            location = locationManager.getLastKnownLocation(provider);

        }
    }

    @Override
    public void onLocationChanged (Location location){
        //do not disturb modus is activate, if speed is higher than 20 kilometers per hour
        this.location = location;
        if(location.getSpeed()*3.6 >= MIN_SPEED){
            doNotDisturbOn();
        }
    }

    @Override
    public void onStatusChanged (String s,int i, Bundle bundle){

    }

    @Override
    public void onProviderEnabled (String provider){

    }

    @Override
    public void onProviderDisabled (String provider){

    }

    //Bluetooth Permissions are in the Manifest now by Sandra 2019-08-12 15:01
    /*private boolean isBluetoothActive(){
        //if-Abfrage by Sandra 2019-08-12 15:16
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled() ) {
            return FALSE;
        }
        return TRUE;
    }

    private boolean isBluetoothCarActive(){
        // if( idBluethoothCar is in Range)...
        return TRUE;
    }

    public void setServiceActive(boolean serviceActive) {
        isServiceActive = serviceActive;
    }

    public void setIdBluethoothCar(int idBluethoothCar) {
        this.idBluethoothCar = idBluethoothCar;
    }

    public void setManualCoolddownStartTime(long manualCoolddownStartTime) {
        this.manualCoolddownStartTime = manualCoolddownStartTime;
    }

    public void setManualCooldownDuration(int manualCooldownDuration) {
        this.manualCooldownDuration = manualCooldownDuration;
    }

    public void setAutoCooldownDuration(int autoCooldownDuration) {
        this.autoCooldownDuration = autoCooldownDuration;
    }
    */
}
