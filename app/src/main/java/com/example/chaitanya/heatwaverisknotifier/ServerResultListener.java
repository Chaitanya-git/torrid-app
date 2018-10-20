package com.example.chaitanya.heatwaverisknotifier;

import com.android.volley.VolleyError;

import org.json.JSONException;

public interface ServerResultListener {
    void onResult(boolean isHeatwave);
    void onResponseFormatError(JSONException e);
    void onVolleyError(VolleyError e);
}
