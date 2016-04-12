package com.vr.record;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.vr.record.Recognizer.SoundsManager;
import com.vr.record.list.Recording;
import com.vr.record.list.RecordingsDatabase;
import com.vr.record.recorder.SoundFile;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Testing extends AppCompatActivity {

    private static final Object WAV_EXTENSION = ".wav";
    private static final String TAG = Testing.class.getSimpleName();


    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        final RecordingsDatabase recordingsDatabase = new RecordingsDatabase(this);
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

        testing(wzor, wzorNazwy, rest);

    }

    private SoundFile.ProgressListener createListener(final Recording rec) {
        return new SoundFile.ProgressListener() {
            @Override
            public boolean reportProgress(double fractionComplete) {
                Log.e(TAG, "report progres" + fractionComplete);
                if (fractionComplete > 0.99f) {
                    Log.e(TAG, "completed loading file " + rec.getName());
                    counter += 1;
                    return false;
                }
                return true;
            }
        };
    }

    private void testing(List<SoundFile> wzor, List<String> wzorNazwy, List<SoundFile> rest) {

        SoundsManager soundsManager = new SoundsManager();
        for (int i = 0; i < wzor.size(); ++i) {

            SoundFile soundFile = wzor.get(i);

            ShortBuffer samples = soundFile.getSamples();
            short[] array = new short[samples.limit()];
            samples.get(array);

            soundsManager.addSound(array, wzorNazwy.get(i));
        }

        for (SoundFile sf : rest) {
            ShortBuffer samples = sf.getSamples();
            short[] array = new short[samples.limit()];
            samples.get(array);
            String s = soundsManager.recognizeSound(array);
            Log.e(TAG, "FINAL RECOGNITION: "+ s);
            Toast.makeText(this, "FOUND " + s, Toast.LENGTH_SHORT).show();
            winnerMethod(soundsManager);
        }
    }

    private void winnerMethod(SoundsManager soundsManager) {
        Map<String, Integer> counter = soundsManager.getCounter();
        Set<Map.Entry<String, Integer>> entries = counter.entrySet();

        Log.e(TAG, "key new xD");
        for (Map.Entry entry : entries) {
            Log.e(TAG, "key " + entry.getKey() + " has " + entry.getValue() + " hits! xD");
        }
    }

    private File createRecordingsDir() {
        File file = new File(getExternalFilesDir(null) + "/recordings");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }
}
