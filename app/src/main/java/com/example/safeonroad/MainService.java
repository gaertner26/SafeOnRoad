package com.example.safeonroad;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
    private final int MIN_SPEED = 1;

    private static final long MIN_DISTANCE_CHECK_FOR_UPDATES = 0; //10 meters Location will update every 10 meters. Only after the user have moved the location will be updated
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000;
    private LocationManager locationManager;
    private String provider;
    private Location location;
    private Boolean GPSisEnabled;
    private static final String CHANNEL_ID = "my_channel_id";
    private static final int NOTIFICATION_ID = 123;

    private static final int NOTIFICATION_COLOR = Color.YELLOW;

    float autoCooldownStartTime = 0;
    float AUTOCOOLDOWNTIME = 5000; // Time, the user has to be slower than 1 m/s, before the Do Not Disturb Mode deactivates itself (Ampelpausen etc)

    String carID;


    public MainService() {

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
        super.onCreate();
        initLocation();
        initBluetooth();


    }

    public void doNotDisturbOn(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // Check if the notification policy access has been granted for the app.
        if ((Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted())) {     //ask for persmission
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= 23 && notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_NONE);      //no Interruption = Everything Blocked
        }

    }

    public void doNotDisturbOff () {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // Check if the notification policy access has been granted for the app.
        if ((Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted())) {     //ask for persmission
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= 23 && notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);      //no Interruption = Everything Blocked
        }
    }

    public int getDontDisturbMode(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted()) {     //ask for persmission
            return notificationManager.getCurrentInterruptionFilter();
        }
        return 1;
    }



    private void initBluetooth() {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
    }
    //Sandra 12.08.2019 (onStartCommand)
    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        Bundle extras = intent.getExtras();
        carID = extras.getString(carID);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    // TODO Hinweis darauf, dass GPS nicht an ist
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
            Log.d("SPEED","Created Location Updater");
            MainActivity.setText("Created Location Updater");
        }
    }

    @Override
    public void onLocationChanged (Location location){
        //do not disturb modus is activate, if speed is higher than 20 kilometers per hour
        this.location = location;
        Log.d("SPEED"," Current Speed: " + String.valueOf(location.getSpeed()));
        MainActivity.setText(" Current Speed: " + String.valueOf(location.getSpeed()));

        if(location.getSpeed()*3.6 >= MIN_SPEED ){ //&& getDontDisturbMode() != NotificationManager.INTERRUPTION_FILTER_NONE
            doNotDisturbOn();
            autoCooldownStartTime = 0;
            sendNotification();
        }


        if(location.getSpeed()*3.6 < MIN_SPEED){
            if(autoCooldownStartTime == 0){
                autoCooldownStartTime = System.currentTimeMillis();
            }else if(System.currentTimeMillis() - autoCooldownStartTime > AUTOCOOLDOWNTIME){
                doNotDisturbOff();
                autoCooldownStartTime = 0;
            }

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

    private void sendNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP );
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Intent stopServiceIntent = new Intent(this, MainActivity.class);
        stopServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        stopServiceIntent.putExtra(getString(R.string.NOTIFICATION_ID_KEY), NOTIFICATION_ID);
        PendingIntent stopServicePendingIntent = PendingIntent.getActivity(this, 0, stopServiceIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID )
                .setSmallIcon(R.drawable.auto_safe_on_road_on)
                .setContentTitle("SafeOnRoad")
                .setContentText("SafeOnRoad is now active")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(0)
                .setStyle(new NotificationCompat.BigTextStyle())
                .setColorized(true)
                .setColor(NOTIFICATION_COLOR)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_cancel_service,"Turn of SafeOnRoad", stopServicePendingIntent).build();
        builder.addAction(action);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_discription);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        doNotDisturbOff();
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
