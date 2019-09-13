package com.example.safeonroad;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

/**
 * Shows the Button where you can start/stop the App
 */
public class HomeFragment extends Fragment {
    private ImageView appOnOffImage;
    private Button getBluetoothButton;
    private String carID = "";


    private final int PERMISSIONS_LOCATION = 3;
    private final int PERMISSION_NOT_GRANTED = 0;
    private final int PERMISSION_ALREADY_GRANTED = 1;
    private final int PERMISSION_ALREADY_REVOKED = -1;

    /**
     * creates listener fpr the View Elements
     * loads in the previously saved CarID
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        getBluetoothButton = view.findViewById(R.id.goToBluetoothFragment);
        getBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new BluetoothFragment()).addToBackStack(null).commit();
            }
        });
        appOnOffImage = view.findViewById(R.id.app_on);
        appOnOffImage.setImageResource(R.drawable.safeonroadstart);
        appOnOffImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.isAppOn == false) {
                    appOnOffImage.setImageResource(R.drawable.safeonroadon);
                    requestPermissions();
                    initService();
                    MainActivity.isAppOn = true;
                }else if(MainActivity.isAppOn) {
                    appOnOffImage.setImageResource(R.drawable.safeonroadoff);
                    Intent i = new Intent(getActivity(), MainService.class);
                    getActivity().stopService(i);
                    MainActivity.isAppOn = false;
                }
            }
        });
        loadCarID();
        if(isMyServiceRunning(MainService.class)){
            appOnOffImage.setImageResource(R.drawable.safeonroadon);
        }
        return view;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                MainActivity.isAppOn = true;
                return true;
            }
        }
        return false;
    }

    /**
     * called in onCreateView
     * retrieves the stored MAC Adress of the linked Blueooth Device
     */
    private void loadCarID() {
        try {
            SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
            carID = sharedPref.getString("CARNAME", null);
        }catch (Exception e){

        }
    }

    /**
     * called when clickable ImageView appOnOff is clicked
     * starts MainService and puts carID as an Extra
     */
    private void initService() {
        Intent i = new Intent(getActivity(), MainService.class);
        if(carID == null){
            carID = "";
        }
        i.putExtra("carID", carID);
        getActivity().startService(i);
    }

    /**
     * called when clickable ImageView appOnOff is clicked
     *  checks for internet, location and bluetooth permissions so that MainService can be started
     */
    private void requestPermissions() {
        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        int permission = hasPermissions(getActivity(), PERMISSIONS);
        if (permission == PERMISSION_NOT_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSIONS_LOCATION);
        } else if (permission == PERMISSION_ALREADY_REVOKED) {
            showDialog(PERMISSIONS, PERMISSIONS_LOCATION);
        } else {
            Toast.makeText(getActivity(), R.string.prev_all_permissions, Toast.LENGTH_SHORT).show();
        }
    }

    private int hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                //This line checks if the permission already has been revoked one time
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission)) {
                        return PERMISSION_ALREADY_REVOKED;
                    } else {
                        appOnOffImage.setImageResource(R.drawable.safeonroadoff);
                        return PERMISSION_NOT_GRANTED;

                    }
                }
            }
        }
        return PERMISSION_ALREADY_GRANTED;
    }

    /**
     * Shows dialog in which the user has to accept the permissions
     */
    private void showDialog(final String[] PERMISSIONS, final int permissionCode) {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(
                getActivity());
        dialog.setTitle(R.string.dialog_title);
        dialog.setMessage(R.string.dialog_message);
        dialog.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, permissionCode);
            }
        });
        dialog.show();
    }

}


