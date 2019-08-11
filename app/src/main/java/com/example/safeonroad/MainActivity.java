package com.example.safeonroad;

//changed by Victoria on 11.08.2019. UIs are made for tests and can be deleted.
//The Part of Code with Permissions is OK and can be used in project. Can be put in Comments for further application
// The part with Intent is also a test, and doesn't work and can be deleted


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{
    //UI to test the service with speedometer
    private Button start;
    private Button permissions;
    private TextView textView;


    private final int PERMISSIONS_LOCATION = 3;
    private final int PERMISSION_NOT_GRANTED = 0;
    private final int PERMISSION_ALREADY_GRANTED = 1;
    private final int PERMISSION_ALREADY_REVOKED = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                                         Intent i = new Intent(MainActivity.this, LocationService.class);
                                         startService(i);
                                     }
                                 });

    }

    private void requestPermissions() {

            String[] PERMISSIONS = {Manifest.permission.INTERNET,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
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
