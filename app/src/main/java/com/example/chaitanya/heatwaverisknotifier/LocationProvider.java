package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class LocationProvider {
    private FusedLocationProviderClient mFusedLocationCLient;
    private Context mContext;
    private LocationCallback mLocationCallback;

    LocationProvider(Context context) {
        mFusedLocationCLient = LocationServices.getFusedLocationProviderClient(context);
        mContext = context;
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
                        response.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
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
                                if (locationSettingsResponse.getLocationSettingsStates().isLocationPresent())
                                    mFusedLocationCLient.requestLocationUpdates(request, mLocationCallback, null);
                                else
                                    listener.onFailure();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            });
        else
            listener.onPermissionDenied();
    }

}
