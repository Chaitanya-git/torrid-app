package com.example.chaitanya.heatwaverisknotifier;

import android.app.Notification;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class PushNotificationService extends FirebaseMessagingService {
    private static final int PUSH_NOTIFICATION_ID = 2;
    @Override
    public void onNewToken(String token){
        registerUserWithServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){
        Log.d("HEATWAVE", remoteMessage.getFrom());
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Utils.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentInfo(remoteMessage.getNotification().getBody())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle( new NotificationCompat.BigTextStyle().bigText(getString(R.string.alert_description_long)));

        notificationManagerCompat.notify(PUSH_NOTIFICATION_ID, builder.build());
    }
    private void registerUserWithServer(final String token){
        new LocationProvider(getBaseContext(), null).requestLocation(new LocationProviderResultListener() {
            @Override
            public void onSuccess(Location location) {
                Log.i("HEATWAVE","Sending location back to server...");
                RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                String endpointUrl = Utils.serverUrl+"users";
                JSONObject userRegJson = new JSONObject();
                try {
                    userRegJson.put("lat", location.getLatitude());
                    userRegJson.put("lon", location.getLongitude());
                    userRegJson.put("userid", token);
                } catch (JSONException e) {
                    Log.i("HEATWAVE", e.getLocalizedMessage());
                    e.printStackTrace();
                }
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, endpointUrl, userRegJson,
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
                queue.add(request);
            }

            @Override
            public void onPermissionDenied() {

            }

            @Override
            public void onFailure() {

            }
        });
    }
}
