package com.example.chaitanya.heatwaverisknotifier;

import android.location.Location;

public interface LocationProviderResultListener {
    void onSuccess(Location location);
    void onPermissionDenied();
    void onFailure();
}
