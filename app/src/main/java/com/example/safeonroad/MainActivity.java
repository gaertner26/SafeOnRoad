package com.example.safeonroad;

//changed by Victoria on 11.08.2019. UIs are made for tests and can be deleted.
//The Part of Code with Permissions is OK and can be used in project. Can be put in Comments for further application
// The part with Intent is also a test, and doesn't work and can be deleted


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Set;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener /*implements LocationListener */{
    //UI to test the service with speedometer
    //private Button appOffOn;

    //Switch switchEnableButton;
    private ImageView appOnOff;
    private static TextView textView;
    private DrawerLayout drawLayout;
    private Button button3;
    private TextView bluetoothCar;

    private boolean f = false;
    private final int PERMISSIONS_LOCATION = 3;
    private final int PERMISSION_NOT_GRANTED = 0;
    private final int PERMISSION_ALREADY_GRANTED = 1;
    private final int PERMISSION_ALREADY_REVOKED = -1;
    private final int MIN_SPEED = 20;

    private static final long MIN_DISTANCE_CHECK_FOR_UPDATES = 1; //10 meters Location will update every 10 meters. Only after the user have moved the location will be updated
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000;
    private LocationManager locationManager;
    private String provider;
    private Location location;
    private Boolean GPSisEnabled;

    private int carID;
    private static final int REQUEST_ENABLE = 1;


    //Times for Cooldowns etc

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initActionBar();
        // init location + textView by Sandra 2019-08-14 12:56
        /*initLocation();
        if(location != null) {
            textView.setText(Integer.toString((int) location.getLongitude()));
        }else{
            textView.setText("Your device has no GPS");
        }
        //init Service and onDestroy added by Christoph
        //initService();
        */
    }


    private void initActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawLayout = findViewById(R.id.drawer_Layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_closed);
        drawLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        //close Navigation and do not leave the activity!
        if (drawLayout.isDrawerOpen(GravityCompat.START)) {
            drawLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initService() {
        Intent i = new Intent(this, MainService.class);
        startService(i);
        textView.setText("Service Started!");
        appOnOff.setImageResource(R.drawable.safeonroadon);

        /*
        //if Main Button Was pressed: Start MainBackgroundService in ANOTHER THREAD!
        service.setIdBluethoothCar(int carID);
        service.setActiveState(true);
        service.start();


        // if big Button was pressed:
        service.setActiveState(false);   // AND service gets closed when this activity gets closed -> if case later
        // or
        stop(service)  //?
        */
    }

    public static void setText(String newText){
        textView.setText(newText);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*
        if(service.isActive == false){
            stopService()
        }
        */


        //otherwise, the service should run even if the app is closed
        // if the service is closed here, he can only be activated again by restarting the app and pressing the button
    }

    private void initViews() {
       //DO NOT DELETE switchEnableButton = findViewById(R.id.switch_app_enable);
        NavigationView navi = findViewById(R.id.navigation);
        navi.setNavigationItemSelectedListener(this);

        textView = findViewById(R.id.textView);

        appOnOff = findViewById(R.id.app_on);
        appOnOff.setImageResource(R.drawable.safeonroadoff);
        appOnOff.setOnClickListener(new View.OnClickListener() {
            //hier the App should calculate in background the speed and then, according to the data, set the textView, but it obviously does not work so easy;((
            @Override
            public void onClick(View view) {
                requestPermissions();
                initService();
            }
        });
    /** PLEASE! DO NOT DELETE, may be it will work))
     *  switchEnableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    appOffOn.setEnabled(true);
                } else{
                    stopService(i);
                    appOffOn.setEnabled(false);

                }
            }
        });**/
        button3 = findViewById(R.id.button3);
        bluetoothCar = findViewById(R.id.textViewBluetoothCar);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCarId();
            }
        });

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.bluetooth_on:
                getCarId();
                break;
            case R.id.auto_start:
                initService();
                break;
        }
        return true;
    }

    private void getCarId() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast toast = Toast.makeText(this, "Your device does not support bluetooth", Toast.LENGTH_LONG);
            toast.show();
        }
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                bluetoothCar.setText(deviceName);
            }
        }
    }

    private void requestPermissions() {

        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        int permission = hasPermissions(this, PERMISSIONS);
        if (permission == PERMISSION_NOT_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_LOCATION);
        } else if (permission == PERMISSION_ALREADY_REVOKED) {
            showDialog(PERMISSIONS, PERMISSIONS_LOCATION);
        } else {
            Toast.makeText(this, R.string.prev_all_permissions, Toast.LENGTH_SHORT).show();
        }
    }

    private int hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
//                    This line checks if the permission already has been revoked one time
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission)) {
                        return PERMISSION_ALREADY_REVOKED;
                    } else {
                        appOnOff.setImageResource(R.drawable.safeonroadoff);
                        return PERMISSION_NOT_GRANTED;

                    }
                }
            }
        }
        return PERMISSION_ALREADY_GRANTED;
    }

    private void showDialog(final String[] PERMISSIONS, final int permissionCode) {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(
                this);
        dialog.setTitle(R.string.dialog_title);
        dialog.setMessage(R.string.dialog_message);
        dialog.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, permissionCode);
            }
        });
        dialog.show();
    }

    /*private void initLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.GPS_PROVIDER;
        GPSisEnabled = locationManager.isProviderEnabled(provider);
        if(GPSisEnabled) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHECK_FOR_UPDATES, this);
            location = locationManager.getLastKnownLocation(provider);

        }
    }
    @Override
    public void onLocationChanged (Location location){
        this.location = location;
        if(location != null) {
            textView.setText("You moved");
        }else{
            textView.setText("Your location isn´t available");
        }
        if(location.getSpeed()* 3.6 >= MIN_SPEED){
            textView.setText("zu schnell");
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
    */

    }



