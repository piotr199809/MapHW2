package com.example.maphw2;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;

    List<LatLng> positionList;

    private final String MARKERS_JSON_FILE = "markers.json";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private StringBuilder stringBuilder;

    private Button clear_memory_btn;
    private FloatingActionButton value_ac_btn;
    private FloatingActionButton hide_btn;
    private TextView textView;
    private LinearLayout linearLayout;
    private ConstraintLayout mainContainer;

    private boolean startMeasure;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        positionList = new ArrayList<>();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        startMeasure = true;

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            //Success!
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            //Failure! No accelerometer
            Toast.makeText(getApplicationContext(), "Failure! No accelerometer", Toast.LENGTH_LONG).show();
        }

        clear_memory_btn = findViewById(R.id.clear_memory_button);
        value_ac_btn = findViewById(R.id.value_ac_btn);
        hide_btn = findViewById(R.id.hide_btn);
        textView = findViewById(R.id.textView);
        linearLayout = findViewById(R.id.linearLayout);
        mainContainer = findViewById(R.id.mainView);

        hide_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator buttonsAnimator;
                int y = mainContainer.getHeight() + 240;
                buttonsAnimator = ObjectAnimator.ofFloat(linearLayout, "y", y);
                buttonsAnimator.setDuration(500);
                buttonsAnimator.start();

                if (startMeasure == false)
                    textViewAnimateHide();

                startMeasure = true;
            }
        });

        value_ac_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startMeasure == true) {
                    if (accelerometer != null) {
                        sensorManager.registerListener(MapsActivity.this, accelerometer, 100000);
                    }
                    textViewAnimateDisplay();
                    startMeasure = false;
                } else {
                    textViewAnimateHide();

                    if (accelerometer != null) {
                        sensorManager.unregisterListener(MapsActivity.this, accelerometer);
                    }

                    startMeasure = true;
                }

            }
        });

        clear_memory_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                positionList.clear();
                saveToJson();
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();
        displayMarker();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));

        positionList.add(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.getUiSettings().setMapToolbarEnabled(false);
        ObjectAnimator buttonsAnimator;

        int y = mainContainer.getHeight() - 240;

        buttonsAnimator = ObjectAnimator.ofFloat(linearLayout, "y", y);
        buttonsAnimator.setDuration(500);
        buttonsAnimator.start();

        return false;
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private void saveToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(positionList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKERS_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 100000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>() {
            }.getType();
            List<LatLng> o = gson.fromJson(readJson, collectionType);
            if (o != null) {
                for (LatLng latLng : o) {
                    positionList.add(latLng);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        saveToJson();
        super.onPause();
    }

    @Override
    protected void onStop() {
        saveToJson();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saveToJson();
        super.onDestroy();
    }

    public void displayMarker() {
        for (LatLng latLng : positionList)
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude, latLng.longitude))
                    .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        stringBuilder = new StringBuilder();
        stringBuilder.append("Acceleration:\n")
                .append(String.format("x: %.4f ", event.values[0]))
                .append(String.format("y: %.4f ", event.values[1]));

        textView.setText(stringBuilder.toString());

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void textViewAnimateDisplay() {
        ObjectAnimator textViewAnimator;
        float fromScaleY = 0f;
        float toScaleY = 1f;
        textViewAnimator = ObjectAnimator.ofFloat(textView, "scaleY", fromScaleY, toScaleY);
        textViewAnimator.setInterpolator(new AccelerateInterpolator());
        textViewAnimator.setDuration(200);
        textViewAnimator.start();
    }

    public void textViewAnimateHide() {
        ObjectAnimator textViewAnimator;
        float fromScaleY = 1f;
        float toScaleY = 0f;
        textViewAnimator = ObjectAnimator.ofFloat(textView, "scaleY", fromScaleY, toScaleY);
        textViewAnimator.setInterpolator(new AccelerateInterpolator());
        textViewAnimator.setDuration(200);
        textViewAnimator.start();
    }
}
