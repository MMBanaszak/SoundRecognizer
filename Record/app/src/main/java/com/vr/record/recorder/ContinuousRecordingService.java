package com.vr.record.recorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vr.record.Recognizer.ResultsManager;
import com.vr.record.Recognizer.SoundsManager;
import com.vr.record.list.Recording;
import com.vr.record.list.RecordingsDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.List;

public class ContinuousRecordingService extends Service implements ContinuousRecorder.OnBufferReadyListener {

    private static final Object WAV_EXTENSION = ".wav";

    private static final String TAG = ContinuousRecordingService.class.getSimpleName();
    private SoundsManager soundsManager;
    private ContinuousRecorder continuousRecorder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(TAG, "onCreate ");

        continuousRecorder = new ContinuousRecorder(this);
        final RecordingsDatabase recordingsDatabase = new RecordingsDatabase(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Recording> allRecordings = recordingsDatabase.getAllRecordings();

                final List<SoundFile> wzor = new LinkedList<>();
                final List<String> wzorNazwy = new LinkedList<>();

                final List<SoundFile> rest = new LinkedList<>();

                final int size = allRecordings.size();

                File filename;
                for (final Recording rec : allRecordings) {

                    filename = new File(createRecordingsDir() + "/" + rec.getTimestamp() + WAV_EXTENSION);
                    try {
                        //createListener(rec)
                        SoundFile soundFile = SoundFile.create(filename.getAbsolutePath(), null);
                        if (!rec.getName().contains("test")) {
                            wzor.add(soundFile);
                            wzorNazwy.add(rec.getName());
                        } else {
                            rest.add(soundFile);
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "IOException", e);
                    } catch (SoundFile.InvalidInputException e) {
                        Log.e(TAG, "InvalidInputException", e);
                    }
                }

                initializeSoundPatterns(wzor, wzorNazwy, rest);
                continuousRecorder.start();

                ResultsManager.deactivateProgressbar();
            }
        }).start();
    }

    private void initializeSoundPatterns(List<SoundFile> wzor, List<String> wzorNazwy, List<SoundFile> rest) {

        soundsManager = new SoundsManager();
        for (int i = 0; i < wzor.size(); ++i) {

            SoundFile soundFile = wzor.get(i);

            ShortBuffer samples = soundFile.getSamples();
            short[] array = new short[samples.limit()];
            samples.get(array);

            soundsManager.addSound(array, wzorNazwy.get(i));
        }
    }

    private File createRecordingsDir() {
        File file = new File(getExternalFilesDir(null) + "/recordings");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        continuousRecorder.stop();
        Log.e(TAG, "onDestroy ");
    }

    @Override
    public void onBufferReady(short[] array) {
        String s = soundsManager.recognizeSound(array);
        if(!s.contains("noise")) {
            //Notify recognized sound.
            ResultsManager.setRecognizedSound(s);
        }
        Log.e(TAG, "FINAL RECOGNITION: " + s);
    }
}
