package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

public class HomeFragment extends Fragment{
    private static final String PREFERENCE_COMPLETED_ONBOARDING = "completed_home_onboarding";
    private static final String TAG = "torrid";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());

        Button checkButton = getView().findViewById(R.id.check_button);

        Toolbar appBar = getView().findViewById(R.id.toolbar);
        appBar.setTitle(R.string.torrid);
        appBar.setTitleTextColor(Color.WHITE);

        ((AppCompatActivity) getActivity()).setSupportActionBar(appBar);

        if(!preferences.getBoolean(PREFERENCE_COMPLETED_ONBOARDING, false)){
            showOnboarding();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREFERENCE_COMPLETED_ONBOARDING, true);
            editor.apply();
        }


        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new LocationProvider(getActivity().getApplicationContext(),getActivity()).requestLocation(new LocationProviderResultListener() {
                    @Override
                    public void onSuccess(Location location) {
                        displayResultsFromServer(location);
                    }

                    @Override
                    public void onPermissionDenied() {
                        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                            new LocationRationaleDialogFragment().show(getActivity().getSupportFragmentManager(), "location_access_request");
                        }
                        else{
                            Utils.requestAccessFineLocation(getActivity());
                        }
                    }

                    @Override
                    public void onFailure() {
                        Log.i("HEATWAVE", "Failed getting location");
                    }
                });

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("ACTIONBAR", "ITEM:"+item.getItemId());

        switch (item.getItemId()) {
            case R.id.tips_button:
                Intent tipsIntent = new Intent(getContext(), TipsActivity.class);
                startActivity(tipsIntent);
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    private void showOnboarding() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(R.id.check_button)
                .setPrimaryText(R.string.check_button_onboarding_primary)
                .setSecondaryText(R.string.check_button_onboarding_secondary)
                .setFocalRadius((float) 170.0)
                .show();
    }

    void displayResultsFromServer(Location location){
        final TextView riskView = getView().findViewById(R.id.riskTextView);
        Utils.getResultsFromAppServer(getContext(), location, new ServerResultListener() {
            @Override
            public void onResult(boolean isHeatwave) {
                if(isHeatwave) {
                    riskView.setText(R.string.at_risk_message);
                    //getView().getRootView().setBackgroundColor(0xff8c00);
                }
                else
                    riskView.setText(R.string.safe_message);
            }

            @Override
            public void onResponseFormatError(JSONException e) {

                riskView.setText(R.string.unexpected_server_response_message);
            }

            @Override
            public void onVolleyError(VolleyError e) {
                riskView.setText(R.string.network_error);
                //getActivity().findViewById(R.id.home_fragment_layout).setBackgroundColor(0xff8c00);
            }
        });
    }
}
