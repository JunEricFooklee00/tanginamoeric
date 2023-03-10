package com.test.tanginamoeric;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    //for request permission
    private boolean isReadPermissionGranted = false;
    private boolean isWritePermissionGranted = false;
    private boolean isLocationPermissionGranted = false;
    private boolean isBackgroundLocationPermissionGranted = false;
    ActivityResultLauncher<String[]> mPermissionResultLauncher;


    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;
    Button btn_new_way_point, btn_show_WayPointList, btn_showMap;

    Switch sw_locationupdates, sw_gps;

    boolean updateOn = false;

    Location currentLocation;

    List<Location> savedLocations;


    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        btn_new_way_point = findViewById(R.id.btn_new_way_point);
        btn_show_WayPointList = findViewById(R.id.btn_show_WayPointList);
        tv_wayPointCounts = findViewById(R.id.tv_CountOfCrumbs);
        btn_showMap = findViewById(R.id.btn_showMap);

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {

                if (result.get(android.Manifest.permission.READ_EXTERNAL_STORAGE) != null) {
                    isReadPermissionGranted = result.get(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                if (result.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != null) {
                    isWritePermissionGranted = result.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if (result.get(android.Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
//                if (result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != null) {
//                    isBackgroundLocationPermissionGranted = result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//                }

            }

        });
//        locationRequest = new LocationRequest();
//        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
//        locationRequest.setFasterstInterval(1000 * FAST_UPDATE_INTERVAL);


//        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateUIValues(locationResult.getLastLocation());
            }
        };
        btn_new_way_point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MyApplication myApplication = (MyApplication) getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);

            }
        });
        btn_show_WayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ShowSavedLocationsList.class);
                startActivity(i);
            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });


        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
//                    locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                    locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
                            .setWaitForAccurateLocation(false)
                            .setMinUpdateIntervalMillis(2000)
                            .setMaxUpdateDelayMillis(100)
                            .build();

                    tv_sensor.setText("Using GPS Sensors");

                } else {
//                    locationRequest.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
                    locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 100)
                            .setWaitForAccurateLocation(false)
                            .setMinUpdateIntervalMillis(2000)
                            .setMaxUpdateDelayMillis(100)
                            .build();

                    tv_sensor.setText("Using Towers + WIFI");

                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
        requestPermission();
    }//end of onCreate

    private void startLocationUpdates() {
        tv_updates.setText("Location is being Tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Location is not being tracked");
        tv_lat.setText("Location is not being tracked");
        tv_lon.setText("Location is not being tracked");
        tv_speed.setText("Location is not being tracked");
        tv_address.setText("Location is not being tracked");
        tv_accuracy.setText("Location is not being tracked");
        tv_altitude.setText("Location is not being tracked");
        tv_sensor.setText("Location is not being tracked");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }


    private void requestPermission() {

        Log.v("Result", "Requesting Permission");

        boolean minSDK = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

        boolean isReadPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        boolean isLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean isWritePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        boolean isBackgroundLocationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        isWritePermissionGranted = isWritePermissionGranted || minSDK;

        List<String> permissionRequest = new ArrayList<String>();

        if (!isReadPermissionGranted) {

            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        }
        if (!isBackgroundLocationPermissionGranted) {

            permissionRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        }
        if (!isWritePermissionGranted) {

            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!isLocationPermissionGranted) {

            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissionRequest.isEmpty()) {

            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }

    }

    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    updateUIValues(location);
                    currentLocation = location;
                }
            });
        } else {
            requestPermission();
        }
    }

    private void updateUIValues(Location location) {
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));
        tv_lat.setText(String.valueOf(location.getLatitude()));

        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("Altitude not available.");
        }
        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Speed Not Available");
        }
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addresses.get(0).getAddressLine(0));
        } catch (Exception e) {
            tv_address.setText("Unable to get Street Address");
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));


    }
}