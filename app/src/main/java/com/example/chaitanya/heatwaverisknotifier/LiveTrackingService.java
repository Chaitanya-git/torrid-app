package com.example.chaitanya.heatwaverisknotifier;

import android.app.Service;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.autofill.Dataset;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.SensorsClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getAvailableLocales;
import static java.text.DateFormat.getDateInstance;

public class LiveTrackingService extends Service {
    private OnDataPointListener mHeartRateListener = new OnDataPointListener() {
        @Override
        public void onDataPoint(DataPoint dataPoint) {
            Log.i("HEATWAVE", "Got data point" + dataPoint);
        }
    };
    private SensorsClient mSensorsClient = null;
    boolean liveTrackingOn = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        startLiveTracking();
        return Service.START_STICKY;
    }

    @Override
    public  void onCreate(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Utils.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Live tracking active")
                .setContentText("HeatwaveRiskNotifier is currently monitoring your heart rate for anomalies")
                .setStyle(new NotificationCompat.BigTextStyle());
        startForeground(101, builder.build());
    }

    @Override
    public void onDestroy(){
        stopLiveTracking();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private GoogleSignInAccount getSignedInAccount(){
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        return GoogleSignIn.getAccountForExtension(getApplicationContext(), fitnessOptions);
    }

    private void startLiveTracking(){

        liveTrackingOn = true;
        Thread requestThread = new Thread(new Runnable(){
            @Override
            public void run() {
                while(liveTrackingOn){
                    // Setting a start and end date using a range of 2 minutes before this moment.
                    Calendar cal = Calendar.getInstance();
                    Date now = new Date();
                    cal.setTime(now);
                    final long endTime = cal.getTimeInMillis();
                    cal.add(Calendar.MINUTE, -2);
                    long startTime = cal.getTimeInMillis();

                    java.text.DateFormat dateFormat = getDateInstance();
                    Log.i("TORRID", "Range Start: " + dateFormat.format(startTime));
                    Log.i("TORRID", "Range End: " + dateFormat.format(endTime));

                    final DataReadRequest readRequest =
                            new DataReadRequest.Builder()
                                    .read(DataType.TYPE_HEART_RATE_BPM)
                                    .setTimeRange(startTime,endTime,TimeUnit.MILLISECONDS)
                                    .build();
                    Task<DataReadResponse> requestResponse = Fitness.getHistoryClient(getBaseContext(), GoogleSignIn.getLastSignedInAccount(getBaseContext())).readData(readRequest);
                    requestResponse.addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                        @Override
                        public void onSuccess(DataReadResponse dataReadResponse) {
                            List<DataSet> dataSets = dataReadResponse.getDataSets();
                            if(dataSets.size() > 0 && dataSets.get(0).getDataPoints().size() > 0) {
                                List<DataPoint> heartrates = dataSets.get(0).getDataPoints();
                                Log.i("TORRID_DATAPOINTS", dataSets.get(0).getDataPoints().get(dataSets.get(0).getDataPoints().size() - 1).toString());
                                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                                for(DataPoint bpm: heartrates){
                                    JSONObject bpmJSON = new JSONObject();
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    try {
                                        bpmJSON.put("userid", preferences.getString("userid", ""));
                                        bpmJSON.put("heart_rate", bpm.getValue(Field.FIELD_BPM));
                                    }
                                    catch (JSONException e){
                                        Log.i("TORRID_LIVE_TRACKING", "ERROR: "+e.getMessage());
                                    }
                                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, Utils.usersEndpoint, bpmJSON,
                                            new Response.Listener<JSONObject>() {
                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    Log.i("TORRID_LIVE_TRACKING: ", "Sent bpm to server");
                                                    checkIfAffected(response);
                                                }
                                            },
                                            new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    Log.i("TORRID_LIVE_TRACKING: ", "Error sending bpm");
                                                }
                                            }
                                    );
                                    queue.add(request);
                                }

                            }
                            else {
                                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                                JSONObject bpmJSON = new JSONObject();
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                try {
                                    bpmJSON.put("userid", preferences.getString("userid", ""));
                                    bpmJSON.put("heart_rate", 120);
                                }
                                catch (JSONException e){
                                    Log.i("TORRID_LIVE_TRACKING", "ERROR: "+e.getMessage());
                                }
                                JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, Utils.usersEndpoint, bpmJSON,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.i("TORRID_LIVE_TRACKING: ", "Sent bpm to server");
                                                checkIfAffected(response);
                                            }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.i("TORRID_LIVE_TRACKING: ", "Error sending bpm"+error.getLocalizedMessage());
                                            }
                                        }
                                );
                                queue.add(request);
                                Log.i("TORRID_DATAPOINTS", "NO new datapoints");
                            }
                        }
                    });
                    try{
                        Thread.sleep(2*60*1000); //2 minutes
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        requestThread.start();
    }

    private void stopLiveTracking(){
        //Fitness.getSensorsClient(getApplicationContext(), GoogleSignIn.getLastSignedInAccount(getApplicationContext()))
        liveTrackingOn = false;
    }

    private void checkIfAffected(JSONObject response){
        try {
            JSONArray data = response.getJSONArray("IsAnomaly");
            if (!data.getBoolean(data.length() - 1)) {
                Utils.notifyAffectedUser(getApplicationContext());
            }
        }
        catch (JSONException e){
            Log.e("TORRID_AFFECTED", "Error: "+e.getMessage());
        }

    }
}
