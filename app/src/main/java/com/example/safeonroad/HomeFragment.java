package com.example.safeonroad;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class HomeFragment extends Fragment {

    private ImageView appOnOff;

    private int appOnOffpos=0;
    private Button getBluetooth;
    private static TextView textView;

    private final int PERMISSIONS_LOCATION = 3;
    private final int PERMISSION_NOT_GRANTED = 0;
    private final int PERMISSION_ALREADY_GRANTED = 1;
    private final int PERMISSION_ALREADY_REVOKED = -1;
    String carID = "";
    private static final int REQUEST_ENABLE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //textView = (TextView) textView.findViewById(R.id.textView) ;
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        getBluetooth = (Button) view.findViewById(R.id.goToBluetoothFragment);
        getBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new BluetoothFragment()).addToBackStack(null).commit();
            }
        });
        appOnOff = (ImageView) view.findViewById(R.id.app_on);
        appOnOff.setImageResource(R.drawable.safeonroadoff);

        appOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (appOnOffpos == 0) {
                    appOnOff.setImageResource(R.drawable.safeonroadon);
                    requestPermissions();
                    initService();
                    appOnOffpos = 1;
                } else if (appOnOffpos == 1) {
                    appOnOff.setImageResource(R.drawable.safeonroadoff);
                    Intent i = new Intent(getActivity(), MainService.class);
                    getActivity().stopService(i);
                    appOnOffpos = 0;
                }
            }
        });

        /**Bundle b = getArguments();
        if (b != null) {
            carId = b.getString("carID");
        }**/
        loadCarID();
        return view;
    }


    private void loadCarID() {
        try {
            SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
            carID = sharedPref.getString("CARNAME", null);
            //bluetoothCar.setText( carID + " " + sharedPref.getString("CARMAC", null));
        }catch (Exception e){

        }
    }

    private void initService() {
        Intent i = new Intent(getActivity(), MainService.class);
        i.putExtra("carID", carID);
        Log.d("BLUE1", carID+"That was CARID in Fragment");
        getActivity().startService(i);

    }

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
     static void setText(String text){
    //textView.setText(text);
    }
}


