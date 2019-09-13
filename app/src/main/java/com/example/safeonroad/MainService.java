package com.example.safeonroad;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;



public class MainService extends Service implements LocationListener {

    private boolean isDontDisturbOn = false;

    // GPS Mode
    private LocationManager locationManager;
    private static final long MIN_DISTANCE_CHECK_FOR_UPDATES = 10; //10 meters Location will update every 10 meters. Only after the user have moved the location will be updated
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000; // time between speed checks
    private final int MIN_SPEED = 20; // speed, at which the mode will be turned on
    private int AUTOCOOLDOWNTIME = 10000; // Time, the user has to be slower than 1 m/s, before the Do Not Disturb Mode deactivates itself
    private long gpsCooldownStartTime = -1;


    //Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private String carID; // MAC Adress of the linked Bluetooth Device
    private boolean hasFoundBluetoothInTime = false;
    private Handler handler; // Extra Thread for the Bluetooth Updates
    private int timeBetweenBluetoothChecks = 5000;

    // Notification
    private static final String CHANNEL_ID = "my_channel_id";
    private static final int NOTIFICATION_ID = 123;
    private static final int NOTIFICATION_COLOR = Color.YELLOW;


    /**
     * called when the service gets started
     * used for retrieving the MAC adress of the wanted bluetooth device
     * @param intent Intent with possible Extra
     */
    public int onStartCommand (Intent intent, int flags, int startId){
        try{
            Bundle extras = intent.getExtras();
            carID = extras.getString("carID");
            if(carID == null){
                this.carID = "";
            }
            if(isBluetoothAllowed() && isBluetoothModeAvaliable()){
                startBluetoothMode();
            } else if(isGPSModeAvaliable()){
                startGPSMode();
            }else{
                stopSelf();
            }
        }catch (Exception e){

        }
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Checks, if the app is able to start the GPS Mode
     */
    private boolean isGPSModeAvaliable(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager == null){
            return false;
        }else {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }

    /**
     * Checks, if the app is able to start the Bluetooth Mode
     */
    private boolean isBluetoothModeAvaliable(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null && !carID.equals("")&& bluetoothAdapter.isEnabled()){
            return true;
        }else {
            return false;
        }
    }

    private boolean isBluetoothAllowed(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
           return true;
        }else{
            return false;
        }
    }


    /**
     * called by the onLocationChanged method
     * Used to change the dont-disturb mode
     * @param value TRUE = Mode on, FALSE = Mode off
     */
    private void changeDoNotDisturbMode(boolean value){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // Check if the notification policy access has been granted for the app
        if ((Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted())) {     //ask for persmission
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= 23 && notificationManager.isNotificationPolicyAccessGranted()) {
            if(value){
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);      //dont let any notfication come through
                isDontDisturbOn = true;
            }else{
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);      //let every notification through
                isDontDisturbOn = false;
            }
        }
    }

    /**
     * Creates a Handler which calls itself every "timeBetweenBluetoothChecks" ms, and starts to discover Bluetooth devices in range
     * if he has successfully found the linked device, he will keep or change the do-not-disturb mode to on
     * if he cant Use Bluetooth anymore, he will try switching to GPS, otherwise he stops the Service
     */
    private void startBluetoothMode(){
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isBluetoothModeAvaliable()){
                    if(!hasFoundBluetoothInTime){
                        changeDoNotDisturbMode(false);
                    }
                    IntentFilter scannedDevicesFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(reciever, scannedDevicesFilter);

                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.startDiscovery();
                    hasFoundBluetoothInTime = false;
                    handler.postDelayed(this, timeBetweenBluetoothChecks);

                }else{
                    if(isGPSModeAvaliable()){    //if bluetooth isnt on anymore, switch to GPS mode if possible
                        startGPSMode();
                    }else{
                        stopSelf();
                    }
                }
            }
        }, 500);  //the delay between the start of the service and the first search
    }

    /**
     * BroadcastReceiver which is used to determine weather the linked device has been found or not
     */
    private final BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(carID.equals(device.getAddress())){
                    changeDoNotDisturbMode(true);
                    hasFoundBluetoothInTime = true;
                    sendNotification();
                }
            }
        }
    };

    /**
     * Stops the handler tread
     * stops discovering new devices
     */
    private void stopBluetoothMode(){
        if(bluetoothAdapter != null){
            handler.removeCallbacksAndMessages(null);
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Requests Location updates every "MIN_TIME_BETWEEN_UPDATES" ms
     */
    private void startGPSMode() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String provider = LocationManager.GPS_PROVIDER;
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            if (ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHECK_FOR_UPDATES, this);
        }
    }

    private void stopGPSMode(){
        if(locationManager != null){
            try{
                locationManager.removeUpdates(this);
                unregisterReceiver(mGpsSwitchStateReceiver);
            }catch (Exception e){

            }
        }
    }

    /**
     * Usually called every second by the locationManager
     * Retrieves the speed and decides, weather the state of the do-not-Disturb Mode should be changed or not
     * @param location current location
     */
    @Override
    public void onLocationChanged (Location location){
        double speed = location.getSpeed()*3.6;
        if(speed >= MIN_SPEED){
            gpsCooldownStartTime = -1;
            if(!isDontDisturbOn){
                changeDoNotDisturbMode(true);
                sendNotification();
            }
        }
        if(speed < MIN_SPEED && isDontDisturbOn){
            if(gpsCooldownStartTime == -1){
                gpsCooldownStartTime = System.currentTimeMillis();
            }else if(System.currentTimeMillis() - gpsCooldownStartTime > AUTOCOOLDOWNTIME){
                changeDoNotDisturbMode(false);
                gpsCooldownStartTime = -1;
            }
        }
    }

    /**
     * Checks if the GPS of the device has been turned off, if yes, he tries to switch to the Bluetooth mode of the app
     * if he cant activate the bluetooth mode, he will close the service
     */
    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                try {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {   //if gps gets turned off
                        stopGPSMode();
                        if(isBluetoothAllowed() && isBluetoothModeAvaliable()){   //stop service if there is no bluetooth avaliable atm
                            startBluetoothMode();
                        }else{
                            stopSelf();
                        }
                    }
                }catch (Exception e){

                }
            }
        }
    };


    /**
     * Called when the Do-Not-Disturb Mode gets enabled by the App
     * Creates a Notification, in which the User can go back to the App, or turn it off directly
     */
    private void sendNotification() {
        Intent intent = new Intent(this, HomeFragment.class);
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

    /**
     * Called when the Service stops itself or gets stopped by system
     * Disables GPS and Bluetooth Mode
     */
    @Override
    public void onDestroy(){
        stopGPSMode();
        stopBluetoothMode();
        changeDoNotDisturbMode(false);
        super.onDestroy();
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
}
