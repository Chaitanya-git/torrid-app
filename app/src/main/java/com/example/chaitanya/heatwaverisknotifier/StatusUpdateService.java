package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class StatusUpdateService extends JobService {

    private boolean mRisk = false;
    private boolean mError = false;
    private static final long MAX_LOCATION_AGE = 100000;
    private static final int STATUS_UPDATE_SERIVICE_ID = 10;
    private FusedLocationProviderClient mFusedLocationCLient;
    Location currentLocation = null;

    public static final JobInfo JOB_INFO;
    static{
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(STATUS_UPDATE_SERIVICE_ID,new ComponentName("com.example.chaitanya.heatwaverisknotifier",StatusUpdateService.class.getName()));
        jobInfoBuilder.setPeriodic(24*60*60*1000);
        jobInfoBuilder.setPersisted(true);
        JOB_INFO = jobInfoBuilder.build();
    }
    public void scheduleJob(Context context){
        Log.i("HEATWAVE",context.toString());
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert js != null;
        js.schedule(JOB_INFO);
    }

    public void cancelJob(Context context){
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert js != null;
        js.cancel(STATUS_UPDATE_SERIVICE_ID);
    }

    private void sendAlerts(){
        Log.i("HEATWAVE", "Sending Alerts");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Utils.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setContentTitle(getString(R.string.alert_title))
                .setContentInfo(getString(R.string.alert_description_short))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle( new NotificationCompat.BigTextStyle().bigText(getString(R.string.alert_description_long)))
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(0, builder.build());
    }
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        mFusedLocationCLient = LocationServices.getFusedLocationProviderClient(this);
        Log.i("HEATWAVE", "Starting job...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationCLient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    currentLocation = location;
                    Utils.getResultsFromAppServer(getApplicationContext(), location, new ServerResultListener() {
                        @Override
                        public void onResult(boolean isHeatwave) {
                            mRisk = isHeatwave;
                            Log.i("HEATWAVE", "Received result");
                            //TODO: If true send a notification
                            if(!isHeatwave) {
                                Utils.sendLocationToAppServer(getApplicationContext(), location);
                                sendAlerts();
                            }

                            jobFinished(jobParameters, false);
                        }

                        @Override
                        public void onResponseFormatError(JSONException e) {
                            Log.i("HEATWAVE", "Server response error");
                            mError = true;
                            jobFinished(jobParameters, false);
                        }

                        @Override
                        public void onVolleyError(VolleyError e) {
                            Log.i("HEATWAVE", "Volley error");
                            mError = true;
                            jobFinished(jobParameters, true);
                        }
                    });

                }
            });
            else jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
