package com.example.chaitanya.heatwaverisknotifier;

import android.location.Location;

public interface LocationProviderResultListener {
    void onSuccess(final Location location);
    void onPermissionDenied();
    void onFailure();
}
