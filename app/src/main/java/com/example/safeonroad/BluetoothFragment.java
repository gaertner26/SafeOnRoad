package com.example.safeonroad;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothFragment extends Fragment {
    private ListView BtPairedDevices;
    private TextView bluetoothText;
    private TextView bluetoothCar;
    private Button search;
    private String carID;
    private static final int REQUEST_ENABLE = 1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        BtPairedDevices = (ListView) view.findViewById(R.id.paired_devices);
        bluetoothText = (TextView) view.findViewById(R.id.bluetooth_text);
        bluetoothCar = (TextView) view.findViewById(R.id.bluetooth_car);
        search = (Button) view.findViewById(R.id.searchBluetooth);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtPairedDevices.setVisibility(View.VISIBLE);
                getCarId();
            }
        });
        loadCarID();
        return view;

    }

    /**
     * called in onCreateView
     * gets the MAC adress and name of the bluetooth device selected as the users car in a session before
     */

    private void loadCarID() {
        try {
            SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
            carID = sharedPref.getString("CARNAME", null);
            bluetoothCar.setText( carID + " " + sharedPref.getString("CARMAC", null));
        }catch (Exception e){

        }
    }
    private void saveCarID(String carName, String carMAC){
        SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("CARNAME", carName);  //TODO: CARID in R.string abspeichern
        editor.putString("CARMAC", carMAC);
        editor.commit();
    }

    /**
     * called when button search is clicked
     * get a list of paired bluetooth devices
     * user can select one to be saved as his car
     */

    private void getCarId() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d("BLUETOOTH","in Get Car ID");
        if(bluetoothAdapter == null) {
            Toast toast =  Toast.makeText(getActivity(),"Your device does not support bluetooth", Toast.LENGTH_LONG);
            toast.show();
            Log.d("BLUETOOTH","first {]");
        }
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE);
            Log.d("BLUETOOTH","second []");
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String[] btDevices = new String[pairedDevices.size()];

        final ArrayList<String> adresses = new ArrayList<>();

        int index = 0;
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                btDevices[index] = device.getName();
                adresses.add(device.getAddress());
                index++;
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, btDevices);
            BtPairedDevices.setAdapter(arrayAdapter);
            BtPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    HomeFragment.setText(adresses.get(position));
                    carID = adresses.get(position);
                    String selectedItem = (String) BtPairedDevices.getAdapter().getItem(position);
                    bluetoothCar.setText(selectedItem);
                    sendDataToHomeFragment(selectedItem);

                    BtPairedDevices.setVisibility(View.INVISIBLE);
                    saveCarID(carID, selectedItem);
                }

                private void sendDataToHomeFragment(String selectedItem) {
                    Bundle b = new Bundle();
                    b.putString("carID",selectedItem);
                    HomeFragment homeFragment = new HomeFragment();
                    homeFragment.setArguments(b);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container, homeFragment).commit();

                }
            });

        }else{
            Log.d("BLUETOOTH","No Bluethooth Devices found");
        }

    }


}


