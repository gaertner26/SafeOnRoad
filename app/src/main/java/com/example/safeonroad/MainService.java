package com.example.safeonroad;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Calendar;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


// Christoph, 2019-8-12
public class MainService extends Service {
    private Location lastLocation;
    private Location newLocation;
    private BluetoothAdapter bluetoothAdapter;
    public boolean isServiceActive = TRUE;

    private long manualCoolddownStartTime = 0;   //timestamp when user told the app to go to standby for 1 hr
    private long autoCooldownStartTime = 0; // timestamp when the car started to drive < 20 hm/h

    private int manualCooldownDuration = 60 * 60 * 1000;   //example: 1 hr
    private int autoCooldownDuration = 3 * 60 * 1000; //3 mins the car has to be driving < 20 km/h


    private int idBluethoothCar;


    public MainService() {
        //initialize Bluetooth
        initBluetooth();
        //if this service is active, he should perform every 250 ms the following action:


        while (isServiceActive) {


            float velocity = getSpeed(); //changed by Sandra 2019-08-12 14:55
            if (velocity >= 20) {
                dontDisturb(TRUE);
            } else if (velocity < 20) {
                dontDisturb(FALSE);
            }




            /*
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
            */


        }

    }

    private float getSpeed() {
        initLocation();
        float distance = newLocation.distanceTo(lastLocation);

        float speed = distance / 1;   //jede sekunde location abfragen?
        lastLocation = newLocation;
        return speed;


    }

    //By Sandra 2019-08-12 14:55
    private void initLocation() {
        try {
            String service = Context.LOCATION_SERVICE;
            LocationManager locationManager = (LocationManager) getSystemService(service);
            String provider = LocationManager.GPS_PROVIDER;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                newLocation = locationManager.getLastKnownLocation(provider);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(this, "You have to accept the Permissions", Toast.LENGTH_SHORT);
            toast.show();
        }
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




    private void initBluetooth() {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
    }
    //Sandra 12.08.2019 (onStartCommand)
    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    //Bluetooth Permissions are in the Manifest now by Sandra 2019-08-12 15:01
    private boolean isBluetoothActive(){
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

}
