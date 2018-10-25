package com.example.chaitanya.heatwaverisknotifier;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class ServicesFragment extends Fragment {
    private static final String PREFERENCE_ENABLE_BACKGROUND_SERVICE = "enable_background_service";
    private static final String PREFERENCE_ENABLE_LIVE_TRACKING = "enable_live_tracking";
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_onboarding";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 10;
    private Intent mLiveTrackingServiceIntent = null;
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
        Switch enableServiceSwitch = getView().findViewById(R.id.enable_background_checkbox);
        Switch enableLiveTrackingSwitch = getView().findViewById(R.id.enable_live_tracking_checkBox);

        mStatusUpdateService = new StatusUpdateService();
        mLiveTrackingServiceIntent = new Intent(getView().getContext(), LiveTrackingService.class);
        enableServiceSwitch.setChecked( preferences.getBoolean(PREFERENCE_ENABLE_BACKGROUND_SERVICE, false) );

        enableServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

        enableLiveTrackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCE_ENABLE_LIVE_TRACKING, isChecked);
                editor.apply();
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
        enableLiveTrackingSwitch.setChecked(preferences.getBoolean(PREFERENCE_ENABLE_LIVE_TRACKING, false));

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

}
