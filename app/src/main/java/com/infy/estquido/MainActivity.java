package com.infy.estquido;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.infy.estquido.app.This;
import com.infy.estquido.app.services.EstquidoCBLService;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private Uri mGoogleMapsIntentURI;



    private AutoCompleteTextView tv_building;

    private String center;
    private String building;
    private Location location;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        This.CONTEXT.set(getApplicationContext());
        This.APPLICATION.set(getApplication());
        This.MAIN_ACTIVITY.set(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tv_building = findViewById(R.id.tv_building);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MainActivity.this, location -> {
                            if (location != null) {
                                EstquidoCBLService.inferCenter(location, center -> {
                                    MainActivity.this.location = location;
                                    MainActivity.this.center = center;
                                    EstquidoCBLService.fetchSpots(center, map -> {

                                    });
                                });
                                timer.cancel();
                            }
                        });
            }
        }, 0, 1000);
    }

    public void startOutdoorNav(View view) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mGoogleMapsIntentURI);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void searchBuilding(View view) {
        building = tv_building.getText().toString();
        EstquidoCBLService.fetchBuilding(center, building, new EstquidoCBLService.OnBuildingsFetchedCallback() {
            @Override
            public void onBuildingFetched(Map<String, Object> map) {
                Intent intent = new Intent(MainActivity.this, CheckpointsActivity.class);
                intent.putExtra("center", center);
                intent.putExtra("location", location);
                intent.putExtra("building", building);
                startActivity(intent);
            }
        });


    }
}