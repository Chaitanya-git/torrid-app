package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

class Utils {
    private static final int LOCATION_REQUEST_RESULT = 1;
    static final String CHANNEL_ID = "HeatwaveAlerts";
    static String serverUrl = "http://192.168.0.110:3000/";
    static void requestAccessFineLocation(Activity activity){
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_RESULT);
    }

    static void displayEnableGPSPrompt(
            final Activity activity)
    {
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);

        builder.setMessage(R.string.gps_enable_user_request)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                d.dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                            }
                        });
        builder.create().show();
    }
    static void sendLocationToAppServer(Context context, Location location){
        RequestQueue queue = Volley.newRequestQueue(context);
        String endpointUrl = serverUrl+"hotloc";
        JSONObject jsonLocation = new JSONObject();
        try {
            jsonLocation.put("lat", location.getLatitude());
            jsonLocation.put("lon", location.getLongitude());
        } catch (JSONException e) {
            Log.i("HEATWAVE", e.getLocalizedMessage());
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, endpointUrl, jsonLocation,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("HEATWAVE", "Sent location to server");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("HEATWAVE", error.getMessage());
                    }
                }
                );
    }

    static void getResultsFromAppServer(Context context, Location location, final ServerResultListener listener){
        RequestQueue queue = Volley.newRequestQueue(context);

        String endpointUrl = serverUrl+"heatwave?"+"lat="+location.getLatitude()+"&lon="+location.getLongitude();
        System.out.println(endpointUrl);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, endpointUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            if(response.get("HEAT_WAVE_STATUS") == "TRUE")
                                listener.onResult(true);
                            else
                                listener.onResult(false);
                            System.out.println("Server response: " + response);
                        }catch (JSONException e) {
                            listener.onResponseFormatError(e);
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onVolleyError(error);
                        System.out.println("Server response error:" + error);
                    }
                });
        queue.add(request);
    }

    static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
