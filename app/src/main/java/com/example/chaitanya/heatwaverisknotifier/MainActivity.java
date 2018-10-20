package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.renderscript.Element;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.SensorsClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetSequence;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;


public class MainActivity extends AppCompatActivity {

    private static final long MAX_LOCATION_AGE = 100000;
    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_onboarding";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 10;
    private OnDataPointListener mHeartRateListener = new OnDataPointListener() {
        @Override
        public void onDataPoint(DataPoint dataPoint) {
            Log.i("HEATWAVE", "Got data point" + dataPoint);
        }
    };
    private SensorsClient mSensorsClient = null;
    StatusUpdateService mStatusUpdateService;
    Location currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.createNotificationChannel(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CheckBox enableServiceCheckbox = findViewById(R.id.enable_background_checkbox);
        CheckBox enableLiveTrackingCheckbox = findViewById(R.id.enable_live_tracking_checkBox);

        mStatusUpdateService = new StatusUpdateService();

        Button checkButton = findViewById(R.id.check_button);

        enableServiceCheckbox.setChecked( preferences.getBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, false) );
        if(!preferences.getBoolean(PREFERENCE_COMPLETED_ONBOARDING, false)){
            showOnboarding();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREFERENCE_COMPLETED_ONBOARDING, true);
            editor.apply();
        }
        checkButton.setOnClickListener(new View.OnClickListener() {
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

        enableLiveTrackingCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    Log.i("HEATWAVE", "Starting live tracking...");
                    FitnessOptions fitnessOptions = FitnessOptions.builder()
                            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                            .build();
                    if(!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(getApplicationContext()), fitnessOptions)){
                        //compoundButton.setChecked(false);
                        GoogleSignIn.requestPermissions(
                                MainActivity.this,
                                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                                GoogleSignIn.getLastSignedInAccount(getApplicationContext()),
                                fitnessOptions);
                    }
                    else {
                        startLiveTracking();
                    }
                }
                else
                    stopLiveTracking();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE){
                startLiveTracking();
            }
        }
    }

    private GoogleSignInAccount getSignedInAccount(){
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        return GoogleSignIn.getAccountForExtension(getApplicationContext(), fitnessOptions);
    }

    private void startLiveTracking(){
        mSensorsClient = Fitness.getSensorsClient(getApplicationContext(), getSignedInAccount());
        mSensorsClient.add(new SensorRequest.Builder()
                                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                .setSamplingRate(1, TimeUnit.SECONDS)
                                .build(),
                        mHeartRateListener
                );
    }

    private void stopLiveTracking(){
        //Fitness.getSensorsClient(getApplicationContext(), GoogleSignIn.getLastSignedInAccount(getApplicationContext()))
        mSensorsClient.remove(mHeartRateListener)
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if(task.isSuccessful() && task.getResult())
                            Log.i("HEATWAVE", "Stopped live tracking.");
                        else
                            Log.i("HEATWAVE", "Unable to stop live tracking.");
                    }
                });
    }

    private void showOnboarding() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(R.id.check_button)
                .setPrimaryText(R.string.check_button_onboarding_primary)
                .setSecondaryText(R.string.check_button_onboarding_secondary)
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                        if(state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED)
                            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                                    .setTarget(R.id.enable_background_checkbox)
                                    .setPrimaryText(R.string.background_service_onboarding_primary)
                                    .setSecondaryText(R.string.background_service_onboarding_secondary)
                                    .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                                        @Override
                                        public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                                            if(state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED)
                                            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                                                    .setTarget(R.id.enable_live_tracking_checkBox)
                                                    .setPrimaryText(R.string.live_tracking_onboarding_primary)
                                                    .setSecondaryText(R.string.live_tracking_onboarding_secondary)
                                                    .setPromptFocal(new RectanglePromptFocal())
                                                    .show();
                                        }
                                    })
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
