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
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.renderscript.Element;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.SensorsClient;
import com.google.android.gms.fitness.data.BleDevice;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.BleScanCallback;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetSequence;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

import static java.text.DateFormat.getDateInstance;


public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_onboarding";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 10;
    private Intent mLiveTrackingServiceIntent = null;
    private static final String TAG = "torrid";
    StatusUpdateService mStatusUpdateService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.createNotificationChannel(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CheckBox enableServiceCheckbox = findViewById(R.id.enable_background_checkbox);
        CheckBox enableLiveTrackingCheckbox = findViewById(R.id.enable_live_tracking_checkBox);

        mStatusUpdateService = new StatusUpdateService();
        mLiveTrackingServiceIntent = new Intent(this, LiveTrackingService.class);

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

                new LocationProvider(getApplicationContext(),MainActivity.this).requestLocation(new LocationProviderResultListener() {
                    @Override
                    public void onSuccess(Location location) {
                        displayResultsFromServer(location);
                    }

                    @Override
                    public void onPermissionDenied() {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){
                            new LocationRationaleDialogFragment().show(getSupportFragmentManager(), "location_access_request");
                        }
                        else{
                            Utils.requestAccessFineLocation(MainActivity.this);
                        }
                    }

                    @Override
                    public void onFailure() {
                        Log.i("HEATWAVE", "Failed getting location");
                    }
                });

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
                            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
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
                        startLiveTrackingService();
                    }
                }
                else stopLiveTrackingService();
            }
        });

    }

    private void startLiveTrackingService(){
        startService(mLiveTrackingServiceIntent);
    }

    private void stopLiveTrackingService(){

        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully unsubscribed for data type: " );
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Subscription not removed
                        Log.i(TAG, "Failed to unsubscribe for data type: ");
                    }
                });

        stopService(mLiveTrackingServiceIntent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE){
                startLiveTrackingService();
            }
        }
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
}
