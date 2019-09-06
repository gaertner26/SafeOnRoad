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
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


// Christoph, 2019-8-12
public class MainService extends Service implements LocationListener {

    //private Location lastLocation;
    //private Location newLocation;

    private BluetoothAdapter bluetoothAdapter;
    //public boolean isServiceActive = TRUE;

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
    //private String provider;
    //private Location location;
    //private Boolean GPSisEnabled;
    private static final String CHANNEL_ID = "my_channel_id";
    private static final int NOTIFICATION_ID = 123;

    private static final int NOTIFICATION_COLOR = Color.YELLOW;

    long autoCooldownStartTime = -1;
    int AUTOCOOLDOWNTIME = 5000; // Time, the user has to be slower than 1 m/s, before the Do Not Disturb Mode deactivates itself (Ampelpausen etc)

    String carID;

    private boolean isDontDisturbOn = FALSE;
    private boolean hasfoundCarInTime = false;

    @Override
    public void onCreate(){
        super.onCreate();

        //startBluetoothMode();


    }
    public int onStartCommand (Intent intent, int flags, int startId){
        Log.d("BLUE1", "On Start Command");
        try{
            Bundle extras = intent.getExtras();
            carID = extras.getString("carID");
            if(carID == null){
                carID = "ABCD";
            }
            //Log.d("BLUE1", carID+"");
            if(isBluetoothAllowed() && isBluetoothModeAvaliable()){
                //Log.d("BLUE1", "gothere1");
                startBluetoothMode();
                //Log.d("BLUE1", "gothere2");
            } else if(isGPSAllowed() && isGPSModeAvaliable()){
                startGPSMode();
                Log.d("BLUE1", "gothere3");
            }else{
                stopSelf();
                Log.d("BLUE1", "gothere4");
            }
        }catch (Exception e){

        }



        return Service.START_NOT_STICKY;
    }

    private boolean isGPSAllowed(){
        return true;
    }
    private boolean isGPSModeAvaliable(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //String provider = LocationManager.GPS_PROVIDER;
        if(locationManager == null){
            Log.d("BLUE1", "loc null");
            return false;
        }else {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }

    private boolean isBluetoothModeAvaliable(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null && !carID.equals("ABCD")&& bluetoothAdapter.isEnabled()){

            Log.d("BLUE1", "BLUETOOTH AVALIABLE");
            return true;
        }else {
            Log.d("BLUE1", "BLUETOOTH NOT AVALIABLE");
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
        // Check if the notification policy access has been granted for the app.
        if ((Build.VERSION.SDK_INT >= 23 && !notificationManager.isNotificationPolicyAccessGranted())) {     //ask for persmission
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= 23 && notificationManager.isNotificationPolicyAccessGranted()) {
            if(value){
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);      //dont let any notfication come through
                isDontDisturbOn = TRUE;
            }else{
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);      //let every notification through
                isDontDisturbOn = FALSE;
            }
        }
    }

    private final BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BLUE1", device.getName());
                //Log.d("BLUE1", device.getAddress()+ " and this is CARID: " + carID +"---");

                if(carID.equals(device.getAddress())){
                    Log.d("BLUE1", "Device Adress Matched carID!");
                    changeDoNotDisturbMode(true);
                    hasfoundCarInTime = true;
                }
            }
        }
    };

    private void startBluetoothMode(){
        changeDoNotDisturbMode(false);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isBluetoothModeAvaliable()){
                    if(hasfoundCarInTime == false){
                        changeDoNotDisturbMode(false);
                        sendNotification();
                    }

                    IntentFilter scannedDevicesFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(reciever, scannedDevicesFilter);

                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.startDiscovery();
                    hasfoundCarInTime = false;
                    Log.d("BLUE1", "Started Discovering Devices");
                    handler.postDelayed(this, 5000);
                }else{
                    if(isGPSAllowed() && isGPSModeAvaliable()){    //if bluetooth isnt on anymore, switch to GPS mode if possible
                        startGPSMode();
                    }else{
                        Log.d("BLUE1", "Stopped Self!");
                        stopSelf();
                    }
                }
                //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                /*
                if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
                    initBluetooth();
                }
                */
                /*
                if(!bluetoothAdapter.isEnabled()){
                    Log.d("BLUE1", "couldnt make bluetooth adapter!");
                    Log.d("BLUE1", carID+"");
                }
                */
                /*
                Log.d("SPEED", carID+" this was carID");
                Log.d("SPEED","hello");


                if(bluetoothAdapter.isEnabled() && !carID.equals("")){  //if he successfully could establish a bluetooth connection
                    //stopGPSMode();
                    if(hasfoundCarInTime == false){
                        changeDoNotDisturbMode(false);
                    }

                    IntentFilter scannedDevicesFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(reciever, scannedDevicesFilter);

                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.startDiscovery();
                    hasfoundCarInTime = false;
                    Log.d("BLUE1", "Started Discovering Devices");
                    handler.postDelayed(this, 5000);
                }else {
                    //final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
                    //if( manager.isProviderEnabled( LocationManager.GPS_PROVIDER )){
                    if(isGPSAllowed() && isGPSModeAvaliable()){
                        startGPSMode();
                    }else{
                        stopSelf();
                    }
                }
                */
            }
        }, 500);  //the time is in miliseconds
    }

    /**
     * called when the service gets started
     * used for retrieving the MAC adress of the wanted bluetooth device
     * @param intent Intent with the Extra
     */


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
        // TODO: Return the communication channel to the service.

        //throw new UnsupportedOperationException("Not yet implemented");
    }



    /*
    @Override
    public boolean onUnbind (Intent intent) {
        return super.onUnbind(intent);
    }
    */


    /**
     * Called when the Service gets started
     * Instantiates the Location Manger if GPS in enabled, otherwise stops the Service again
     */

    private void stopGPSMode(){
        if(locationManager != null){
            try{
                locationManager.removeUpdates(this);
                unregisterReceiver(mGpsSwitchStateReceiver);
            }catch (Exception e){

            }
        }
    }
    private void startGPSMode() {
        changeDoNotDisturbMode(false);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String provider = LocationManager.GPS_PROVIDER;
        Boolean GPSisEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //Log.d("BLUE1", "in startGPSMODE");
        if(GPSisEnabled) {
            Log.d("BLUE1", "GPS enabled");
            registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            if (ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHECK_FOR_UPDATES, this);
            //Log.d("SPEED","Created Location Updater");
            //HomeFragment.setText("Created Location Updater");
        }else{
            //Toast.makeText(this, "Please turn on your GPS!", Toast.LENGTH_SHORT).show();
            //stopSelf();
        }
    }

    /**
     * Usually called every second by the locationManager
     * Retrieves the speed and decides, weather the state of the do-not-Disturb Mode should be changed or not
     * @param location current location
     */
    @Override
    public void onLocationChanged (Location location){
        //this.location = location;
        double speed = location.getSpeed()*3.6;
        Log.d("BLUE1"," Current Speed: " + speed);
        HomeFragment.setText(" Current Speed: " + speed);

        //Log.d("MODE", getDontDisturbMode()+"");
        if(speed >= MIN_SPEED){
            autoCooldownStartTime = -1;
            if(isDontDisturbOn == FALSE){
                changeDoNotDisturbMode(TRUE);
                sendNotification();
            }
        }
        Log.d("MODE", autoCooldownStartTime +"");

        if(speed < MIN_SPEED && isDontDisturbOn){
            if(autoCooldownStartTime == -1){
                autoCooldownStartTime = System.currentTimeMillis();
            }else if(System.currentTimeMillis() - autoCooldownStartTime > AUTOCOOLDOWNTIME){
                changeDoNotDisturbMode(FALSE);
                Log.d("MODE", "Dont Disturb Turned off");
                autoCooldownStartTime = -1;
            }
        }
    }

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
                            Log.d("BLUE1", "Stopped Self!");
                            stopSelf();
                        }
                    }
                }catch (Exception e){

                }
            }
        }
    };

    @Override
    public void onStatusChanged (String s,int i, Bundle bundle){

    }

    @Override
    public void onProviderEnabled (String provider){

    }

    @Override
    public void onProviderDisabled (String provider){

    }

    /**
     * When the dont-disturb mode gets activated in the onLocationChanged method, this Notification tells the user, that the SafeOnRoad App has done that
     */
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
        Log.d("BLUE1", "OnDestroy");
        stopGPSMode();
        Log.d("BLUE1", "OnDestroy executed 1");
        if(bluetoothAdapter != null){
            bluetoothAdapter.cancelDiscovery();
        }
        Log.d("BLUE1", "OnDestroy executed2");
        changeDoNotDisturbMode(false);
        Log.d("BLUE1", "OnDestroy executed3");
        //super.onDestroy();
        //changeDoNotDisturbMode(FALSE);
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
