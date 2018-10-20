package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

class LocationProvider {
    private static final int RESOLVE_ERROR_REQUEST_CODE = 12;
    private FusedLocationProviderClient mFusedLocationCLient;
    private Context mContext;
    private Activity mActivity;
    private LocationCallback mLocationCallback;

    LocationProvider(Context context, Activity activity) {
        mFusedLocationCLient = LocationServices.getFusedLocationProviderClient(context);
        mContext = context;
        mActivity = activity;
    }

    void requestLocation(final LocationProviderResultListener listener) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationCLient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if (location != null)
                        listener.onSuccess(location);
                    else {
                        final LocationRequest request = new LocationRequest();
                        request.setInterval(5000);
                        request.setFastestInterval(1000);
                        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

                        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                                .addLocationRequest(request);

                        SettingsClient client = LocationServices.getSettingsClient(mContext);
                        Task<LocationSettingsResponse> response = client.checkLocationSettings(builder.build());
                        response.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                                mLocationCallback = new LocationCallback(){
                                    @Override
                                    public void onLocationResult(LocationResult locationResult){
                                        if(locationResult == null ) listener.onFailure();
                                        else {
                                            listener.onSuccess(locationResult.getLastLocation());
                                        }
                                        mFusedLocationCLient.removeLocationUpdates(mLocationCallback);
                                    }
                                };
                                LocationSettingsResponse locationSettingsResponse = null;
                                try {
                                    locationSettingsResponse = task.getResult(ApiException.class);
                                } catch (ApiException e) {
                                    switch (e.getStatusCode()){
                                        case LocationSettingsStatusCodes
                                                .RESOLUTION_REQUIRED:
                                            try {
                                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                                Log.i("HEATWAVE", "mActivity" + mActivity);
                                                if(mActivity != null)
                                                    resolvable.startResolutionForResult(mActivity, RESOLVE_ERROR_REQUEST_CODE);
                                            }
                                            catch (IntentSender.SendIntentException intentException){
                                                //Ignore
                                            }

                                    }
                                }
                                if (locationSettingsResponse != null && locationSettingsResponse.getLocationSettingsStates().isLocationPresent())
                                    mFusedLocationCLient.requestLocationUpdates(request, mLocationCallback, null);
                                else
                                    listener.onFailure();
                            }
                        });
                    }
                }
            });
        else
            listener.onPermissionDenied();
    }

}
