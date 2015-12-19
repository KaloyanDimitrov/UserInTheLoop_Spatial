package com.example.kaloyan.interfaceuserinloop;

import android.app.VoiceInteractor;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class OptionsActivity extends AppCompatActivity {

    private int walkingDistance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        //for storing settings
        final SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();

       //editor.putInt("distance",10);
       // editor.commit();

        final SeekBar modeBar = (SeekBar)findViewById(R.id.modeBar);
        SeekBar meterBar = (SeekBar)findViewById(R.id.meterBar);

        modeBar.setMax(2);
        meterBar.setMax(500);

        modeBar.setProgress(settings.getInt("mode", 0));
        meterBar.setProgress(settings.getInt("meter",500));

        modeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt("mode", seekBar.getProgress());
                editor.commit();
                String toastMsg = "";
                if(modeBar.getProgress() == 0)
                    toastMsg = "Heat Map mode selected";
                if(modeBar.getProgress() == 1)
                    toastMsg = "Cell Towers mode selected";
                if(modeBar.getProgress() == 2)
                    toastMsg = "Better Location mode selected";
                //TODO reduce number of toasts
                Toast.makeText(OptionsActivity.this, toastMsg,
                       Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        meterBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt("meter",seekBar.getProgress());
                editor.commit();
                walkingDistance = seekBar.getProgress();
                //Toast.makeText(OptionsActivity.this, "You are willing to walk " + seekBar.getProgress()+" meters" ,
                //        Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        final CheckBox boxShareInfo = (CheckBox) findViewById(R.id.boxShareInfo);
        boxShareInfo.setChecked(settings.getBoolean("shareDataEnabled", true));//tick the box if there is a previous setting recorded
        boxShareInfo.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){
                //clicking this button negates the previous preference
                editor.putBoolean("shareDataEnabled",
                        boxShareInfo.isChecked()); //true is default value if previous setting is found
                editor.commit();
            }
        });




    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Toast.makeText(OptionsActivity.this, "You are willing to walk " + walkingDistance + " meters.",
              Toast.LENGTH_SHORT).show();
       // editor.putInt("distance",Integer.valueOf(distance.getText().toString()));
    }

}
