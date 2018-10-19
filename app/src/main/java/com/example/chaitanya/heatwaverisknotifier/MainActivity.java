package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final long MAX_LOCATION_AGE = 100000;
    Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //if(preferences.getBoolean());

        Button CheckButton = findViewById(R.id.check_button);

        CheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LocationManager locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        System.out.println(location);
                        getResultsFromAppServer(location);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                };
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){
                        new LocationRationaleDialogFragment().show(getSupportFragmentManager(), "location_access_request");
                    }
                    else{
                        Utils.requestAccessFineLocation(MainActivity.this);
                    }
                }
                else{
                    try {
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if( location == null || location.getTime() - Calendar.getInstance().getTimeInMillis() > MAX_LOCATION_AGE) {
                            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, locationListener);
                            else
                                Utils.displayEnableGPSPrompt(MainActivity.this);
                        }
                        else
                            getResultsFromAppServer(location);
                    }
                    catch (NullPointerException e) {
                        DialogFragment errorDialog = new LocationRequestErrorDialogFragment();
                        errorDialog.show(getSupportFragmentManager(), "gps_location_request_error");
                    }
                }
            }
        });
    }

    public void getResultsFromAppServer(Location location){
        RequestQueue queue = Volley.newRequestQueue(this);
        String serverUrl = "http://radiant-bastion-64391.herokuapp.com/";
        serverUrl = "http://192.168.0.110:3000/";
        String endpointUrl = serverUrl+"heatwave?"+"lat="+location.getLatitude()+"&lon="+location.getLongitude();
        final TextView RiskView = findViewById(R.id.riskTextView);
        System.out.println(endpointUrl);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, endpointUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            if(response.get("HEAT_WAVE_STATUS") == "TRUE")
                                RiskView.setText("AT RISK!");
                            else
                                RiskView.setText("Looks like you're safe");
                            System.out.println("Server response: " + response);
                        }catch (JSONException e) {
                            RiskView.setText("Didn't get expected response from server.");
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        RiskView.setText("Server error!");
                        System.out.println("Server response error:" + error);
                    }
                });
        queue.add(request);
    }
    public void setCurrentLocation(Location location){
        TextView RiskView = findViewById(R.id.riskTextView);
        RiskView.setText("At Risk!");
        currentLocation = location;
    }

}
