/**
 * © 2016 Twisted Pair Solutions, Inc. All rights reserved.
 * Twisted Pair Solutions Confidential Restricted.
 * Not for use or re-distribution without Twisted Pair Solutions prior written permission.
 */
package com.motorolasolutions.bluetoothscansample;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION_CONSTANT = 10;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 20;
    private BroadcastReceiver mBluetoothReceiver;
    List<BluetoothDevice> mListOfScannedDevices = new ArrayList<BluetoothDevice>();
    List<BluetoothDevice> mListOfPairedDevices = new ArrayList<BluetoothDevice>();
    ListView mLvScanDevices;
    ListView mLvPairedDevices;
    Button mBtnScan;
    ProgressBar mBluetoothScanProgressBar;
    BluetoothScanResultAdapter mPairedadapter;
    BluetoothScanResultAdapter mScannedAdapter;
    boolean isBluetoothOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermission(this);

        checkIfBluetoothOn();

        mLvScanDevices = (ListView) findViewById(R.id.lvScanDevices);
        mLvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        mBluetoothScanProgressBar = (ProgressBar) findViewById(R.id.bluetoothScanProgressBar);

        mScannedAdapter = new BluetoothScanResultAdapter(this, R.layout.bluetooth_item, mListOfScannedDevices);

        mPairedadapter = new BluetoothScanResultAdapter(this, R.layout.bluetooth_item, mListOfPairedDevices);
        mLvPairedDevices.setAdapter(mPairedadapter);
        loadPairedDevices();

        mLvScanDevices.setAdapter(mScannedAdapter);

        mBluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch(action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Here is where we should see the Klein button
                        Log.i(TAG, "onReceive ACTION_FOUND - " + foundDevice.getName());
                        mListOfScannedDevices.add(foundDevice);
                        mScannedAdapter.notifyDataSetChanged();
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i(TAG, "Scanning discovery finished");
                        mBluetoothScanProgressBar.setVisibility(View.GONE);
                        mBtnScan.setEnabled(true);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        Log.i(TAG, "onReceive ACTION_DISCOVERY_STARTED");
                        mBluetoothScanProgressBar.setVisibility(View.VISIBLE);
                        mBtnScan.setEnabled(false);
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                        BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (state == BluetoothDevice.BOND_BONDED) {
                            Log.i(TAG, "onReceive BOND_BONDED");
                        } else if (state == BluetoothDevice.BOND_NONE) {
                            Log.i(TAG, "onReceive BOND_NONE");
                        }
                        break;
                    case BluetoothDevice.ACTION_NAME_CHANGED:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.i(TAG, "onReceive ACTION_NAME_CHANGED = " + device.getName() + " - " + device.getAddress());
//                        String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                        break;
                }
            }
        };

        setFilters(mBluetoothReceiver);


        mBtnScan = (Button) findViewById(R.id.btnScan);
        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBluetoothOn) {
                    loadPairedDevices();
                    mListOfScannedDevices.clear();
                    mScannedAdapter.clear();
                    mScannedAdapter.notifyDataSetChanged();
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                    bluetoothAdapter.startDiscovery();
                } else {
                    Toast.makeText(MainActivity.this, "Please turn bluetooth on", Toast.LENGTH_SHORT).show();
                }
            }
        });

        AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

                Log.i(TAG, "Selected device " + device.getName());
            }
        };
        mLvScanDevices.setOnItemClickListener(itemClickListener);
        mLvPairedDevices.setOnItemClickListener(itemClickListener);

    }

    private void loadPairedDevices() {
        Set pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        mListOfPairedDevices.clear();
        mPairedadapter.notifyDataSetChanged();
        mListOfPairedDevices.addAll(pairedDevices);
        mPairedadapter.notifyDataSetChanged();
    }

    private void setFilters(BroadcastReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
    }

    public class BluetoothScanResultAdapter extends ArrayAdapter<BluetoothDevice> {

        public BluetoothScanResultAdapter(Context context, int textViewResourceId, List<BluetoothDevice> foundDevices) {
            super(context, textViewResourceId, foundDevices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice device = getItem(position);
            if (device != null) {
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    convertView = inflater.inflate(R.layout.bluetooth_item, null);
                }

                TextView tvDeviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
                TextView tvAddress = (TextView) convertView.findViewById(R.id.tvAddress);

                String deviceName = "Unknown device";
                if (device.getName() != null && device.getName().length() > 0) {
                    deviceName = device.getName();
                }
                tvDeviceName.setText(deviceName);

                String deviceAddress = "Unknown Address";
                if (device.getAddress() != null) {
                    deviceAddress = device.getAddress();
                }
                tvAddress.setText(deviceAddress);

            }
            return convertView;
        }
    }

    private static void checkLocationPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION_CONSTANT);
            }
        }
    }

    private void checkIfBluetoothOn() {
        // Automatically turn bluetooth on if not on
        if (BluetoothAdapter.getDefaultAdapter() != null &&
                !(BluetoothAdapter.getDefaultAdapter().isEnabled())) {
            isBluetoothOn = false;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        } else {
            isBluetoothOn = true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                isBluetoothOn = true;
            } else {
                isBluetoothOn = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBluetoothReceiver);
        super.onDestroy();
    }
}
