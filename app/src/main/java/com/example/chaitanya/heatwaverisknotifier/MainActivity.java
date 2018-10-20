package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.Set;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetSequence;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;


public class MainActivity extends AppCompatActivity {

    private static final long MAX_LOCATION_AGE = 100000;
    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_onboarding";
    StatusUpdateService mStatusUpdateService;
    Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.createNotificationChannel(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CheckBox enableServiceCheckbox = findViewById(R.id.enable_background_checkbox);
        mStatusUpdateService = new StatusUpdateService();
        Button CheckButton = findViewById(R.id.check_button);

        enableServiceCheckbox.setChecked( preferences.getBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, false) );
        if(!preferences.getBoolean(PREFERENCE_COMPLETED_ONBOARDING, false)){
            showOnboarding();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREFERENCE_COMPLETED_ONBOARDING, true);
            editor.apply();
        }
        CheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LocationManager locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        System.out.println(location);
                        displayResultsFromServer(location);
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
                else {
                    try {
                        assert locationManager != null;
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location == null || location.getTime() - Calendar.getInstance().getTimeInMillis() > MAX_LOCATION_AGE) {
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, locationListener);
                            else
                                Utils.displayEnableGPSPrompt(MainActivity.this);
                        } else
                            displayResultsFromServer(location);

                    } catch (NullPointerException e) {
                        DialogFragment errorDialog = new LocationRequestErrorDialogFragment();
                        errorDialog.show(getSupportFragmentManager(), "gps_location_request_error");
                    }
                }
            }
        });

        enableServiceCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, isChecked);
                editor.apply();

                if(isChecked)
                    mStatusUpdateService.scheduleJob(getApplicationContext());
                else
                    mStatusUpdateService.cancelJob(getApplicationContext());
            }
        });
    }

    private void showOnboarding() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(R.id.check_button)
                .setPrimaryText("Check for alerts")
                .setSecondaryText("Press \"Check\" to quickly check for a heatwave alert in your area")
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                        if(state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED)
                            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                                    .setTarget(R.id.enable_background_checkbox)
                                    .setPrimaryText("Enable background service")
                                    .setSecondaryText("Enable the app's background service to have the app automatically check for alerts in your area once a day and notify you of any new alerts")
                                    .setPromptStateChangeListener(null)
                                    .setPromptFocal(new RectanglePromptFocal())
                                    .show();
                    }
                })
                .setFocalRadius((float) 170.0)
                .show();
    }

    void displayResultsFromServer(Location location){
        final TextView riskView = findViewById(R.id.riskTextView);
        Utils.getResultsFromAppServer(this, location, new ServerResultListener() {
            @Override
            public void onResult(boolean isHeatwave) {
                if(isHeatwave)
                    riskView.setText(R.string.at_risk_message);
                else
                    riskView.setText(R.string.safe_message);
            }

            @Override
            public void onResponseFormatError(JSONException e) {
                riskView.setText(R.string.unexpected_server_response_message);
            }

            @Override
            public void onVolleyError(VolleyError e) {
                riskView.setText(R.string.network_error);
            }
        });
    }

    public void setCurrentLocation(Location location){
        TextView RiskView = findViewById(R.id.riskTextView);
        RiskView.setText(R.string.at_risk_message);
        currentLocation = location;
    }

    public void createLocationRequest(){
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * 24 * 60 * 60);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        //SettingsClient
    }

}
