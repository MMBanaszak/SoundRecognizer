package com.vr.record.recorder;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class ContinuousRecorder implements AudioRecord.OnRecordPositionUpdateListener {

    private static final String TAG = ContinuousRecorder.class.getSimpleName();

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLES_PER_FRAME = SAMPLE_RATE / 4;
    private static final int BYTES_PER_SAMPLE = 2;
    private final OnBufferReadyListener onBufferReadyListener;
    private AudioRecord audioRecord = null;
    private short[] buffer;

    public interface OnBufferReadyListener {
        void onBufferReady(short[] buffer);
    }

    public ContinuousRecorder(OnBufferReadyListener onBufferReadyListener) {
        this.onBufferReadyListener = onBufferReadyListener;
        buffer = new short[SAMPLES_PER_FRAME];
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        recorder.read(buffer, 0, buffer.length);
//        liveAnalysis.update(buffer);
        //Log.e(TAG, "onPeriodicNotification " + buffer);
        onBufferReadyListener.onBufferReady(buffer);
    }

    public void start() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                SAMPLES_PER_FRAME * BYTES_PER_SAMPLE * 4);
        audioRecord.setRecordPositionUpdateListener(this);
        audioRecord.setPositionNotificationPeriod(SAMPLES_PER_FRAME);
        audioRecord.startRecording();
    }

    public void stop() {
        audioRecord.setRecordPositionUpdateListener(null);
        if (AudioRecord.STATE_UNINITIALIZED != audioRecord.getState()) {
            audioRecord.stop();
        }
        audioRecord.release();
        audioRecord = null;
    }
}