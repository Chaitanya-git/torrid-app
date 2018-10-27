package com.example.chaitanya.heatwaverisknotifier;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private static final String USER_NAME = "username";
    private static final String PHONE = "phone_number";
    private final int RESULT_PICK_IMAGE = 5;
    private final int REQUEST_STORAGE_PERMISSION = 10;
    private String ONBOARDING_COMPLETED = "profile_onboarding_completed";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        ImageButton profilePic = getActivity().findViewById(R.id.profileImageButton);
        Button updateButton = getActivity().findViewById(R.id.update_button);
        loadProfilePic();
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                }
                else {
                    pickProfilePic();
                }
            }
        });
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        EditText nameInput = getView().findViewById(R.id.name_input_layout);
        nameInput.setText(preferences.getString(USER_NAME,""));

        EditText phoneInput = getView().findViewById(R.id.contact_input_layout);
        phoneInput.setText(preferences.getString(PHONE, ""));

        if(!preferences.getBoolean(ONBOARDING_COMPLETED, false)){
            Log.i("TORRID PROFILE","Onboarding not done");
            UserRegistrationPromptDialog dialog = new UserRegistrationPromptDialog();
            dialog.show(getFragmentManager(), "user_reg_prompt");
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(ONBOARDING_COMPLETED,true);
            editor.apply();
        }

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextInputLayout nameField = getActivity().findViewById(R.id.name_input_field);
                String name = nameField.getEditText().getText().toString();
                TextInputEditText phoneField = getActivity().findViewById(R.id.contact_input_layout);
                String phoneNumber = phoneField.getText().toString();

                RequestQueue queue = Volley.newRequestQueue(getActivity().getBaseContext());
                String endpointUrl = Utils.serverUrl+"users";
                JSONObject userRegJson = new JSONObject();

                try {
                    if(preferences.contains("userid"))
                        userRegJson.put("userid", preferences.getString("userid", ""));
                    else {
                        userRegJson.put("regtoken", "regtoken");
                        /*userRegJson.put("lat",13.352585);
                        userRegJson.put("lon",74.793579);
                        */
                    }
                    Log.i("HEAT", "Error"+preferences.getString("userid", ""));
                    userRegJson.put("name", name);
                    userRegJson.put("num", Long.valueOf(phoneNumber));
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(USER_NAME,name);
                    editor.putString(PHONE, phoneNumber);
                    editor.apply();
                } catch (JSONException e) {
                    Log.i("HEATWAVE", e.getLocalizedMessage());
                    e.printStackTrace();
                }
                JsonObjectRequest request;
                if(preferences.contains("userid")) {
                    request = new JsonObjectRequest(Request.Method.PUT, endpointUrl, userRegJson,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.i("HEATWAVE", "Sent user details to server");
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.i("HEATWAVE_V", "Error:" + error.getMessage());
                                }
                            }
                    );
                    Log.i("TORRID_REG", "Updating details");
                }
                else {
                    request = new JsonObjectRequest(Request.Method.POST, endpointUrl, userRegJson,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    SharedPreferences.Editor editor = preferences.edit();
                                    Log.i("TORRID_REG_RESP", response.toString());
                                    try {
                                        editor.putString("userid", response.getJSONObject("data").getString("_id"));
                                    } catch (JSONException e) {
                                        Log.e("TORRID_USER_REG_ERROR", e.getLocalizedMessage());
                                        e.printStackTrace();
                                    }
                                    editor.apply();
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    error.printStackTrace();
                                }
                            });
                    Log.i("TORRID_REG", "Registering new user");
                }
                Log.i("TORRID_JSON", userRegJson.toString());
                queue.add(request);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if (requestCode == RESULT_PICK_IMAGE && null != data) {
                Uri selectedImage = data.getData();
                if(selectedImage == null) return;
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContext().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                ImageButton proPic = getActivity().findViewById(R.id.profileImageButton);
                Bitmap proPicBitmap = BitmapFactory.decodeFile(picturePath);
                proPicBitmap = Bitmap.createScaledBitmap(proPicBitmap, 350, 350, false);
                proPic.setImageBitmap(proPicBitmap);
                saveProfilePic(proPicBitmap);
            }
            else if(requestCode == REQUEST_STORAGE_PERMISSION){
                pickProfilePic();
            }
        }

    }

    private void pickProfilePic(){
        Intent pickPic = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        startActivityForResult(pickPic, RESULT_PICK_IMAGE);
    }

    private void saveProfilePic(Bitmap proPicBitmap){
        ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
        File directory = cw.getDir("profile", Context.MODE_PRIVATE);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File mypath = new File(directory, "thumbnail.png");

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(mypath);
            proPicBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e("SAVE_IMAGE", e.getMessage(), e);
        }
    }

    private void loadProfilePic(){
        ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
        File directory = cw.getDir("profile", Context.MODE_PRIVATE);
        if (!directory.exists()) {
            Log.i("PROFILE_PAGE", "Directory not found");
            return;
        }
        File mypath = new File(directory, "thumbnail.png");
        ImageButton proPicButton = getActivity().findViewById(R.id.profileImageButton);
        try{
            FileInputStream inputStream;
            inputStream = new FileInputStream(mypath);
            Bitmap pic = BitmapFactory.decodeStream(inputStream);
            proPicButton.setImageBitmap(pic);
        }
        catch (FileNotFoundException e){
            proPicButton.setBackgroundResource(R.drawable.pro_pic_dummy);
        }
    }
}
