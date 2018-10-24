package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

public class HomeFragment extends Fragment{
    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_onboarding";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 10;
    private Intent mLiveTrackingServiceIntent = null;
    private static final String TAG = "torrid";
    StatusUpdateService mStatusUpdateService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());
        CheckBox enableServiceCheckbox = getView().findViewById(R.id.enable_background_checkbox);
        CheckBox enableLiveTrackingCheckbox = getView().findViewById(R.id.enable_live_tracking_checkBox);

        mStatusUpdateService = new StatusUpdateService();
        mLiveTrackingServiceIntent = new Intent(getView().getContext(), LiveTrackingService.class);

        Button checkButton = getView().findViewById(R.id.check_button);

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

                new LocationProvider(getActivity().getApplicationContext(),getActivity()).requestLocation(new LocationProviderResultListener() {
                    @Override
                    public void onSuccess(Location location) {
                        displayResultsFromServer(location);
                    }

                    @Override
                    public void onPermissionDenied() {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                            new LocationRationaleDialogFragment().show(getActivity().getSupportFragmentManager(), "location_access_request");
                        }
                        else{
                            Utils.requestAccessFineLocation(getActivity());
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
                    mStatusUpdateService.scheduleJob(getActivity().getApplicationContext());
                else
                    mStatusUpdateService.cancelJob(getActivity().getApplicationContext());
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
                    if(!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext()), fitnessOptions)){
                        //compoundButton.setChecked(false);
                        GoogleSignIn.requestPermissions(
                                getActivity(),
                                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                                GoogleSignIn.getLastSignedInAccount(getActivity().getApplicationContext()),
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
        getActivity().startService(mLiveTrackingServiceIntent);
    }

    private void stopLiveTrackingService(){

        Fitness.getRecordingClient(getActivity(), GoogleSignIn.getLastSignedInAccount(getActivity().getBaseContext()))
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

        getActivity().stopService(mLiveTrackingServiceIntent);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent Data){
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
                            new MaterialTapTargetPrompt.Builder(getActivity())
                                    .setTarget(R.id.enable_background_checkbox)
                                    .setPrimaryText(R.string.background_service_onboarding_primary)
                                    .setSecondaryText(R.string.background_service_onboarding_secondary)
                                    .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                                        @Override
                                        public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                                            if(state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED)
                                                new MaterialTapTargetPrompt.Builder(getActivity())
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
        final TextView riskView = getView().findViewById(R.id.riskTextView);
        Utils.getResultsFromAppServer(getContext(), location, new ServerResultListener() {
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
