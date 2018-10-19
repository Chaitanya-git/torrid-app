package com.example.chaitanya.heatwaverisknotifier;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class LocationRequestErrorDialogFragment extends DialogFragment {
    //LocationManager locationManager;
    //LocationListener locationListener;
    //LocationRequestErrorDialogFragment(LocationManager, LocationListener){
    //}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.gps_location_error)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        return builder.create();
    }
}
