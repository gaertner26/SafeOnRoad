package com.example.safeonroad;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MainService extends Service {
    public boolean isServiceActive = TRUE;


    private int idBluethoothCar;
    public MainService() {
        //if this service is active, he should perform every 250 ms the following action:
        while(isServiceActive){
            //sleep(250);
            if(isBluetoothActive() && isBluetoothCarActive()){
                dontDisturb(TRUE);
            }else{
                if(currentTime - TimeCooldownStarted >= cooldownDuration){  //check, if app has been paused for 2 hours
                    int velocity = getVelocity(); //thread 2? idk
                    if(velocity >= 20km/h  AND getDontDisturb() == false){
                        dontDisturb(TRUE);
                        autoCooldownStartTime = 0;
                    }else if(velocity < 20){
                        if(autoCooldownStartTime == 0){   // check, if the car has been at least 2 mins slower than 20 km/h
                            autoCooldownStartTime = currentTume();
                        }else if(currentTime - autoCooldownStartTime > 2 minutes){
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

    public void setIdBluethoothCar(int id){
        idBluethoothCar = id;
    }

    public void setActiveState(boolean state){
        isServiceActive = state;
    }

}
