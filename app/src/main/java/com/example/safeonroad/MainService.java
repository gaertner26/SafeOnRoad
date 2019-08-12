package com.example.safeonroad;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import java.util.Calendar;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


// Christoph, 2019-8-12
public class MainService extends Service {
    public boolean isServiceActive = TRUE;

    private long manualCoolddownStartTime = 0;   //timestamp when user told the app to go to standby for 1 hr
    private long autoCooldownStartTime = 0; // timestamp when the car started to drive < 20 hm/h

    private int manualCooldownDuration = 60 * 60 * 1000;   //example: 1 hr
    private int autoCooldownDuration = 3 * 60 * 1000; //3 mins the car has to be driving < 20 km/h


    private int idBluethoothCar;


    public MainService() {
        //if this service is active, he should perform every 250 ms the following action:
        while(isServiceActive){
            //sleep(250);
            if(isBluetoothActive() && isBluetoothCarActive()){
                dontDisturb(TRUE);
            }else{
                long time = System.currentTimeMillis();
                if(System.currentTimeMillis() - manualCoolddownStartTime >= manualCooldownDuration){  //check, if app has been paused for 2 hours
                    int velocity = getVelocity(); //thread 2? idk
                    if(velocity >= 20 && getDontDisturb() == false){
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


        }



    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void dontDisturb(boolean state){
        if(getDontDisturb() == false && state == true){
            setDontDisturb = true;
        }else if(getDontDisturb() == true && state == false){
            setDontDisturb = false;
        }

    }

    private boolean isBluetoothActive(){
        //checks, if Bluetooth is active at the moment
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
