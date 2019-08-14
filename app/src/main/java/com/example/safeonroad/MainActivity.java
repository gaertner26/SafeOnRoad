package com.example.safeonroad;

//changed by Victoria on 11.08.2019. UIs are made for tests and can be deleted.
//The Part of Code with Permissions is OK and can be used in project. Can be put in Comments for further application
// The part with Intent is also a test, and doesn't work and can be deleted


import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;


public class MainActivity extends AppCompatActivity{
    //UI to test the service with speedometer
    private Button start;
    private Button permissions;
    private TextView textView;
    private DrawerLayout drawLayout;
    private Location lastLocation;
    private Location newLocation;


    private final int PERMISSIONS_LOCATION = 3;
    private final int PERMISSION_NOT_GRANTED = 0;
    private final int PERMISSION_ALREADY_GRANTED = 1;
    private final int PERMISSION_ALREADY_REVOKED = -1;


    //Times for Cooldowns etc

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doNotDisturbOn();
        initViews();
        initActionBar();
        initLocation();


        //init Service and onDestroy added by Christoph
        //initService();





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
        float speed= newLocation.getSpeed();
        int i = (int)speed;
        textView = findViewById(R.id.textView);
        textView.setText(i);
    }
 //written by Victoria 12.08.2019, 22:34
    private void initActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawLayout = findViewById(R.id.drawer_Layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_closed);
        drawLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        //close Navigation and do not leave the activity!
        if(drawLayout.isDrawerOpen(GravityCompat.START)){
            drawLayout.closeDrawer(GravityCompat.START);
        }else {
        super.onBackPressed();
        }
    }

    private void initService(){

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

    // Christoph 12.9   18:30
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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        /*
        if(service.isActive == false){
            stopService()
        }
        */


        //otherwise, the service should run even if the app is closed
        // if the service is closed here, he can only be activated again by restarting the app and pressing the button
    }

    private void initViews(){



        start = findViewById(R.id.start);
        textView = findViewById(R.id.textView);
        permissions = findViewById(R.id.permissions);
        permissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions();
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            //hier the App should calculate in background the speed and then, according to the data, set the textView, but it obviously does not work so easy;((
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, MainService.class); //Changed to MainService by Sandra 2019-08-12 14:57
                startService(i);
            }
        });
    }

    private void requestPermissions() {

            String[] PERMISSIONS = {Manifest.permission.INTERNET,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,  Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
            int permission = hasPermissions(this, PERMISSIONS);
            if(permission == PERMISSION_NOT_GRANTED){
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_LOCATION);
            }else if(permission == PERMISSION_ALREADY_REVOKED) {
                showDialog(PERMISSIONS, PERMISSIONS_LOCATION);
            }else {
                Toast.makeText(this, R.string.prev_all_permissions, Toast.LENGTH_SHORT).show();
            }
        }
    private int hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
//                    This line checks if the permission already has been revoked one time
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,permission)){
                        return PERMISSION_ALREADY_REVOKED;
                    }else {
                        return PERMISSION_NOT_GRANTED;
                    }
                }
            }
        }
        return PERMISSION_ALREADY_GRANTED;
    }
    private void showDialog(final String [] PERMISSIONS, final int permissionCode){
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

}
