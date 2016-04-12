package com.vr.record;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vr.record.Recognizer.ResultsManager;
import com.vr.record.list.RecordingsDatabase;
import com.vr.record.list.RecordingsListActivity;
import com.vr.record.recorder.ContinuousRecordingService;
import com.vr.record.recorder.RecordActivity;

import android.os.Vibrator;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_EDIT = 1;
    private static boolean isRecording = false;

    private static MainActivity contextRecognitionHandler;
    private Intent recorder;
    private Button startRecognitionButton;

    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, RecordActivity.class), REQ_CODE_EDIT);
            }
        });

        final View testing = findViewById(R.id.testing);
        testing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Testing.class));
            }
        });

        final TextView recognizedSoundLabel = (TextView)findViewById(R.id.recognized_sound);
        ResultsManager.setTextViewReference(recognizedSoundLabel);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        ResultsManager.setVibrator(v);

        final Button recordingsListButton = (Button)findViewById(R.id.recordings_list);
        final FloatingActionButton rightRecordButton = (FloatingActionButton)findViewById(R.id.fab);


        contextRecognitionHandler=this;
        startRecognitionButton = (Button)findViewById(R.id.start_recognition);
        startRecognitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isRecording) {
                    startRecognitionButton.setText("Stop recognition");
                    isRecording = true;
                    progress = ProgressDialog.show(MainActivity.this, "Starting sound recognition",
                            "Please wait while loding sound patterns from your recordings list...",true);
                    recordingsListButton.setVisibility(View.INVISIBLE);
                    rightRecordButton.setVisibility(View.INVISIBLE);
                    ResultsManager.setProgressBar(progress);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            recorder = new Intent(contextRecognitionHandler, ContinuousRecordingService.class);
                            startService(recorder);
                        }
                    }).start();

                } else {
                    startRecognitionButton.setText("Start recognition");
                    isRecording = false;
                    stopService(recorder);
                    recordingsListButton.setVisibility(View.VISIBLE);
                    rightRecordButton.setVisibility(View.VISIBLE);
                }
            }
        });

        setupRecordingsListVisibility();
    }

    private void setupRecordingsListVisibility() {
        final View recordingsList = findViewById(R.id.recordings_list);

        final RecordingsDatabase recordingsDatabase = new RecordingsDatabase(this);
        if (recordingsDatabase.getAllRecordings().size() > 0) {
            recordingsList.setVisibility(View.VISIBLE);
            recordingsList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(new Intent(MainActivity.this, RecordingsListActivity.class), REQ_CODE_EDIT);
                }
            });
        } else {
            recordingsList.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_EDIT && resultCode == RESULT_OK) {
            setupRecordingsListVisibility();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_licenses) {
            startActivity(new Intent(this, Licenses.class));
            return true;
        }



        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_HOME || (keyCode == KeyEvent.KEYCODE_BACK)) {
        stop();
        }
        return super.onKeyDown(keyCode,event);
    }

    private void stop(){
        if (isRecording){
            startRecognitionButton.setText("Start recognition");
            isRecording = false;
            stopService(recorder);
        }
    }
}
