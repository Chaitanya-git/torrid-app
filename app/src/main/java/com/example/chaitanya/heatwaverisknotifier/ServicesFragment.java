package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

import static com.example.chaitanya.heatwaverisknotifier.Utils.LOCATION_REQUEST_RESULT;

public class ServicesFragment extends Fragment {
    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_ENABLE_LIVE_TRACKING = "enable_live_tracking";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_services_onboarding";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 10;
    private Intent mLiveTrackingServiceIntent = null;
    private Switch mEnableLiveTrackingSwitch;
    private Switch mEnableServiceSwitch;
    private static final String TAG = "torrid";
    StatusUpdateService mStatusUpdateService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i("SERVICE_FRAGMENT", "IM HERE");
        return inflater.inflate(R.layout.services_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());
        mEnableServiceSwitch = getView().findViewById(R.id.enable_background_switch);
        mEnableLiveTrackingSwitch = getView().findViewById(R.id.enable_live_tracking_switch);

        mStatusUpdateService = new StatusUpdateService();
        mLiveTrackingServiceIntent = new Intent(getView().getContext(), LiveTrackingService.class);
        mEnableServiceSwitch.setChecked( preferences.getBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, false) );

        if(!preferences.getBoolean(PREFERENCE_COMPLETED_ONBOARDING,false)){
            showOnboarding();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREFERENCE_COMPLETED_ONBOARDING, true);
            editor.apply();
        }

        mEnableServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, isChecked);
                editor.apply();

                if(isChecked) {
                    if(ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                            new LocationRationaleDialogFragment().show(getActivity().getSupportFragmentManager(), "location_access_request");
                        }
                        else{
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Utils.LOCATION_REQUEST_RESULT);
                        }
                    }
                    mStatusUpdateService.scheduleJob(getActivity().getApplicationContext());
                }
                else
                    mStatusUpdateService.cancelJob(getActivity().getApplicationContext());
            }
        });

        mEnableLiveTrackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
                        compoundButton.setChecked(false);
                    }
                    else if(!preferences.contains("userid")){
                        new UserRegistrationPromptDialog().show(getFragmentManager(),"user_reg_prompt");
                        compoundButton.setChecked(false);
                    }
                    else {
                        startLiveTrackingService();
                    }
                }
                else stopLiveTrackingService();

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCE_ENABLE_LIVE_TRACKING, compoundButton.isChecked());
                editor.apply();
            }
        });
        mEnableLiveTrackingSwitch.setChecked(preferences.getBoolean(PREFERENCE_ENABLE_LIVE_TRACKING, false));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i("TORRID_PERMISSION", "CALLED");
        switch (requestCode) {
            case Utils.LOCATION_REQUEST_RESULT: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("TORRID_GRANTED", "PICKING");
                    mEnableServiceSwitch.setChecked(true);
                }
                else mEnableServiceSwitch.setChecked(false);
            }
        }
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

    private void showOnboarding(){
        new MaterialTapTargetPrompt.Builder(getActivity())
                .setTarget(R.id.enable_background_switch)
                .setPrimaryText(R.string.background_service_onboarding_primary)
                .setSecondaryText(R.string.background_service_onboarding_secondary)
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                        if(state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED)
                            new MaterialTapTargetPrompt.Builder(getActivity())
                                    .setTarget(R.id.enable_live_tracking_switch)
                                    .setPrimaryText(R.string.live_tracking_onboarding_primary)
                                    .setSecondaryText(R.string.live_tracking_onboarding_secondary)
                                    .setPromptFocal(new RectanglePromptFocal())
                                    .show();
                    }
                })
                .setPromptFocal(new RectanglePromptFocal())
                .show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent Data){
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE){
                mEnableLiveTrackingSwitch.setChecked(true);
            }
        }
    }

}
