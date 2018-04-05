package com.subodhjena.objectcounter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_main);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView startDrawButton = (ImageView) findViewById(R.id.startDrawButton);
        startDrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DrawActivity.class));
            }
        });

        ImageView showTutorialButton = (ImageView) findViewById(R.id.showTutorialButton);
        showTutorialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TutorialActivity.class));
            }
        });

        ImageView startCalibrateButton = (ImageView) findViewById(R.id.startCalibrateButton);
        startCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CalibrateActivity.class));
            }
        });
    }
}
