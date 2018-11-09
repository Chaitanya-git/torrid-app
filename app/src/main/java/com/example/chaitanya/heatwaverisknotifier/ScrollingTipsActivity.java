package com.example.chaitanya.heatwaverisknotifier;

import android.os.Bundle;
import android.app.Activity;

public class ScrollingTipsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling_tips);
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
    }
}
