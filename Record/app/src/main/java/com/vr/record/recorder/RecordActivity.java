package com.vr.record.recorder;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vr.record.R;
import com.vr.record.list.Recording;
import com.vr.record.list.RecordingsDatabase;

import java.io.File;
import java.io.IOException;

public class RecordActivity extends AppCompatActivity implements WaveformView.WaveformListener,
        MarkerView.MarkerListener, EditNameDialog.EditNameDialogListener {

    private static final String TAG = RecordActivity.class.getSimpleName();

    public static final String ACTION_EDIT = "actionEdit";
    public static final String RECORDING = "recording";
    private static final String RECORDING_TIME_FORMAT = "%02d:%02d";
    private static final Object WAV_EXTENSION = ".wav";

    private long recordingLastUpdateTime;
    private boolean mRecordingKeepGoing;
    private double mRecordingTime;

    private int mWidth;
    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;
    private boolean mStartVisible;
    private boolean mEndVisible;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private int mOffset;
    private int mOffsetGoal;
    private int mFlingVelocity;
    private int mPlayStartMsec;
    private int mPlayEndMsec;
    private boolean mKeyDown;

    private String filename;

    private SoundFile soundFile;
    private File file;
    private Thread loadSoundFileThread;
    private Thread recordAudioThread;
    private Thread saveSoundFileThread;

    private Handler handler;
    private TextView timerView;

    private SamplePlayer mPlayer;

    private RecordingsDatabase recordingsDatabase;
    private WaveformView waveformView;

    private boolean mIsPlaying;
    private boolean mTouchDragging;
    private float mTouchStart;
    private int mTouchInitialOffset;
    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    private long mWaveformTouchStartMsec;
    private float mDensity;

    private int mMarkerLeftInset;
    private int mMarkerRightInset;
    private int mMarkerTopOffset;
    private int mMarkerBottomOffset;

    private MarkerView mStartMarker;
    private MarkerView mEndMarker;
    private ImageButton play;
    private ProgressDialog mProgressDialog;
    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;

    enum State {
        READY, RECORDING, STOPPED, SAVING, SAVED
    }

    private State state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        handler = new Handler();
        recordingsDatabase = new RecordingsDatabase(this);

        initLayout();
        updateState(State.READY);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (action != null && ACTION_EDIT.equals(action)){
            final Recording recording = (Recording) intent.getSerializableExtra(RECORDING);
            filename = createRecordingsDir() + "/" +  recording.getTimestamp() + WAV_EXTENSION;
            Log.d(TAG, "action edit, filename " + filename);
            loadFromFile();
            updateState(State.STOPPED);
        }
    }

    private void initLayout() {
        timerView = (TextView) findViewById(R.id.record_audio_timer);
        timerView.setText(String.format(RECORDING_TIME_FORMAT, 0, 0));

        play = (ImageButton) findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                play(view);
            }
        });
        play.setVisibility(View.GONE);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        mMarkerLeftInset = (int)(46 * mDensity);
        mMarkerRightInset = (int)(48 * mDensity);
        mMarkerTopOffset = (int)(10 * mDensity);
        mMarkerBottomOffset = (int)(10 * mDensity);

        enableDisableButtons();

        waveformView = (WaveformView)findViewById(R.id.waveform_view);
        waveformView.setListener(this);

        mMaxPos = 0;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        if (soundFile != null && !waveformView.hasSoundFile()) {
            waveformView.setSoundFile(soundFile);
            waveformView.recomputeHeights(mDensity);
            mMaxPos = waveformView.maxPos();
        }

        mStartMarker = (MarkerView)findViewById(R.id.startmarker);
        mStartMarker.setListener(this);
        mStartMarker.setAlpha(1f);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);
        mStartVisible = true;

        mEndMarker = (MarkerView)findViewById(R.id.endmarker);
        mEndMarker.setListener(this);
        mEndMarker.setAlpha(1f);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        mEndVisible = true;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFABClick();
            }
        });
    }

    private void onFABClick() {
        switch(state) {

            case READY:
                recordAudio();
                break;
            case RECORDING:
                stopRecording();
                break;
            case STOPPED:
                showSaveDialog();
                break;
            case SAVING:
                break;
            case SAVED:
                break;
        }
    }

    private void showSaveDialog() {
        FragmentManager fm = getSupportFragmentManager();
        EditNameDialog editNameDialog = new EditNameDialog();
        editNameDialog.show(fm, "fragment_edit_name");
    }

    @Override
    public void onFinishEditDialog(String inputText) {
        saveRecording(inputText);
    }

    private void play(final View v) {
        mPlayer.start();
        mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                mPlayer.stop();
//                v.setEnabled(true);
            }
        });
    }

    private void loadFromFile() {
        file = new File(filename);

        mLoadingLastUpdateTime = getCurrentTime();
        mLoadingKeepGoing = true;
        mProgressDialog = new ProgressDialog(RecordActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(R.string.progress_dialog_loading);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mLoadingKeepGoing = false;
                    }
                });
        mProgressDialog.show();

        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double fractionComplete) {
                        long now = getCurrentTime();
                        if (now - mLoadingLastUpdateTime > 100) {
                            mProgressDialog.setProgress(
                                    (int) (mProgressDialog.getMax() * fractionComplete));
                            mLoadingLastUpdateTime = now;
                        }
                        return mLoadingKeepGoing;
                    }
                };

        // Load the sound file in a background thread
        loadSoundFileThread = new Thread() {
            public void run() {
                try {
                    soundFile = SoundFile.create(file.getAbsolutePath(), listener);

                    if (soundFile == null) {
                        mProgressDialog.dismiss();
                        Log.e(TAG, "bad file " + file);
                        return;
                    }
                    mPlayer = new SamplePlayer(soundFile);
                } catch (final Exception e) {
                    mProgressDialog.dismiss();
                    Log.e(TAG, "Exception loading file " + file, e);
                    return;
                }
                mProgressDialog.dismiss();
                if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                        }
                    };
                    handler.post(runnable);
                }
            }
        };
        loadSoundFileThread.start();
    }

    private void finishOpeningSoundFile() {
        if (soundFile == null) {
            return;
        }
        waveformView.setSoundFile(soundFile);
        waveformView.recomputeHeights(mDensity);

        mMaxPos = waveformView.maxPos();
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        resetPositions();
        if (mEndPos > mMaxPos)
            mEndPos = mMaxPos;

        updateDisplay();
    }

    private void resetPositions() {
        mStartPos = waveformView.secondsToPixels(0.0);
        mEndPos = waveformView.secondsToPixels(15.0);
    }

    private void recordAudio() {
        updateState(State.RECORDING);
        file = null;

        recordingLastUpdateTime = getCurrentTime();
        mRecordingKeepGoing = true;

        recordAudioThread = new Thread() {
            public void run() {
                try {
                    soundFile = SoundFile.record(createListener());
                if (soundFile == null) {
                    return;
                }
                    mPlayer = new SamplePlayer(soundFile);
                    loadSoundFileToWaveFormView();
                } catch (final Exception e) {
                    Log.e(TAG, "Record audio thread", e);
//                mAlertDialog.dismiss();
//                e.printStackTrace();
//                    mInfoContent = e.toString();
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            mInfo.setText(mInfoContent);
//                        }
//                    });

//                    Runnable runnable = new Runnable() {
//                        public void run() {
//                            showFinalAlert(e, getResources().getText(R.string.record_error));
//                        }
//                    };
//                    handler.post(runnable);
                    return;
                }
//                Runnable runnable = new Runnable() {
//                    public void run() {
//                        finishOpeningSoundFile();
//                    }
//                };
//                handler.post(runnable);
            }
        };
        recordAudioThread.start();
    }

    private SoundFile.ProgressListener createListener() {
        return new SoundFile.ProgressListener() {
            public boolean reportProgress(double elapsedTime) {
                long now = getCurrentTime();
                if (now - recordingLastUpdateTime > 5) {
                    mRecordingTime = elapsedTime;
                    handler.post(new Runnable() {
                        public void run() {
                            int min = (int) (mRecordingTime / 60);
                            int sec = (int) (mRecordingTime - 60 * min);
                            timerView.setText(String.format(RECORDING_TIME_FORMAT, min, sec));
                        }
                    });
                    recordingLastUpdateTime = now;
                }
                return mRecordingKeepGoing;
            }
        };
    }

    private void stopRecording() {
        mRecordingKeepGoing = false;
        updateState(State.STOPPED);
    }

    private void saveRecording(String name) {
        file = createRecordingsDir();
        final long currentTimeNano = System.currentTimeMillis();
        double startTime = waveformView.pixelsToSeconds(mStartPos);
        double endTime = waveformView.pixelsToSeconds(mEndPos);
        final int startFrame = waveformView.secondsToFrames(startTime);
        final int endFrame = waveformView.secondsToFrames(endTime);
        try {
            soundFile.WriteWAVFile(new File(file + "/" + currentTimeNano + WAV_EXTENSION), startFrame,
                    endFrame - startFrame);
            Toast.makeText(this, R.string.recording_saved, Toast.LENGTH_SHORT).show();
            recordingsDatabase.saveRecording(new Recording(name, currentTimeNano));
            updateState(State.SAVED);
        } catch (IOException e) {
            Log.e(TAG, "saveRecording IOException", e);
        }

        setResult(RESULT_OK);
        finish();
    }

    private File createRecordingsDir() {
        File file = new File(getExternalFilesDir(null) + "/recordings");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    private void updateState(State newState) {
        state = newState;
        switch (newState) {

            case READY:
            case SAVED:
                mStartMarker.setVisibility(View.GONE);
                mEndMarker.setVisibility(View.GONE);
                break;
            case RECORDING:
                break;
            case STOPPED:
                mStartMarker.setVisibility(View.VISIBLE);
                mEndMarker.setVisibility(View.VISIBLE);
                play.setVisibility(View.VISIBLE);
                loadSoundFileToWaveFormView();
                break;
            case SAVING:
                break;
        }

        updateFAB(newState);
    }

    private void loadSoundFileToWaveFormView() {
        Log.e(TAG, "loadSoundFileToWaveFormView " + soundFile);
        Runnable runnable = new Runnable() {
            public void run() {
                finishOpeningSoundFile();
            }
        };
        handler.post(runnable);
    }

    private void updateFAB(State newState) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        switch (newState) {

            case READY:
            case SAVED:
                fab.setEnabled(true);
                fab.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                break;
            case RECORDING:
                fab.setImageResource(R.drawable.ic_stop_white_24dp);
                break;
            case STOPPED:
                fab.setImageResource(R.drawable.ic_save_white_24dp);
                break;
            case SAVING:
                fab.setEnabled(false);
                break;
        }
    }

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    @Override
    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
        mWaveformTouchStartMsec = getCurrentTime();
    }

    @Override
    public void waveformTouchMove(float x) {
        mOffset = trap((int)(mTouchInitialOffset + (mTouchStart - x)));
        updateDisplay();
    }

    @Override
    public void waveformTouchEnd() {
        mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = waveformView.pixelsToMillisecs(
                        (int)(mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMsec) {
                    mPlayer.seekTo(seekMsec);
                } else {
                    handlePause();
                }
            } else {
                onPlay((int)(mTouchStart + mOffset));
            }
        }
    }

    @Override
    public void waveformFling(float vx) {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        mFlingVelocity = (int)(-vx);
        updateDisplay();
    }

    @Override
    public void waveformDraw() {
        mWidth = waveformView.getMeasuredWidth();
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    @Override
    public void waveformZoomIn() {
        waveformView.zoomIn();
        mStartPos = waveformView.getStart();
        mEndPos = waveformView.getEnd();
        mMaxPos = waveformView.maxPos();
        mOffset = waveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    @Override
    public void waveformZoomOut() {
        waveformView.zoomOut();
        mStartPos = waveformView.getStart();
        mEndPos = waveformView.getEnd();
        mMaxPos = waveformView.maxPos();
        mOffset = waveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    private int trap(int pos) {
        if (pos < 0)
            return 0;
        if (pos > mMaxPos)
            return mMaxPos;
        return pos;
    }

    private synchronized void onPlay(int startPosition) {
        if (mIsPlaying) {
            handlePause();
            return;
        }

        if (mPlayer == null) {
            // Not initialized yet
            return;
        }

        try {
            mPlayStartMsec = waveformView.pixelsToMillisecs(startPosition);
            if (startPosition < mStartPos) {
                mPlayEndMsec = waveformView.pixelsToMillisecs(mStartPos);
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = waveformView.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMsec = waveformView.pixelsToMillisecs(mEndPos);
            }
            mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
                @Override
                public void onCompletion() {
                    handlePause();
                }
            });
            mIsPlaying = true;

            mPlayer.seekTo(mPlayStartMsec);
            mPlayer.start();
            updateDisplay();
            enableDisableButtons();
        } catch (Exception e) {
//            showFinalAlert(e, R.string.play_error);
            Log.e(TAG, "onPlay", e);
            return;
        }
    }

    private synchronized void handlePause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        waveformView.setPlayback(-1);
        mIsPlaying = false;
        enableDisableButtons();
    }

    private synchronized void updateDisplay() {
        if (mIsPlaying) {
            int now = mPlayer.getCurrentPosition();
            int frames = waveformView.millisecsToPixels(now);
            waveformView.setPlayback(frames);
            setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMsec) {
                handlePause();
            }
        }

        if (!mTouchDragging) {
            int offsetDelta;

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30;
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80;
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80;
                } else {
                    mFlingVelocity = 0;
                }

                mOffset += offsetDelta;

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2;
                    mFlingVelocity = 0;
                }
                if (mOffset < 0) {
                    mOffset = 0;
                    mFlingVelocity = 0;
                }
                mOffsetGoal = mOffset;
            } else {
                offsetDelta = mOffsetGoal - mOffset;

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta > 0)
                    offsetDelta = 1;
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta < 0)
                    offsetDelta = -1;
                else
                    offsetDelta = 0;

                mOffset += offsetDelta;
            }
        }

        waveformView.setParameters(mStartPos, mEndPos, mOffset);
        waveformView.invalidate();

        int startX = mStartPos - mOffset - mMarkerLeftInset;
        if (startX + mStartMarker.getWidth() >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mStartVisible = true;
                        mStartMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mStartVisible) {
                mStartMarker.setAlpha(0f);
                mStartVisible = false;
            }
            startX = 0;
        }

        int endX = mEndPos - mOffset - mEndMarker.getWidth() + mMarkerRightInset;
        if (endX + mEndMarker.getWidth() >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mEndVisible = true;
                        mEndMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mEndVisible) {
                mEndMarker.setAlpha(0f);
                mEndVisible = false;
            }
            endX = 0;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                startX,
                mMarkerTopOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mStartMarker.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                endX,
                waveformView.getMeasuredHeight() - mEndMarker.getHeight() - mMarkerBottomOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mEndMarker.setLayoutParams(params);
    }

    private void enableDisableButtons() {
        if (mIsPlaying) {
            play.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            play.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }

    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        setOffsetGoalNoUpdate(offset);
        updateDisplay();
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;
    }

    @Override
    public void markerDraw() {
    }

    @Override
    public void markerTouchStart(MarkerView marker, float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
    }

    @Override
    public void markerTouchMove(MarkerView marker, float x) {
        float delta = x - mTouchStart;

        if (marker == mStartMarker) {
            mStartPos = trap((int)(mTouchInitialStartPos + delta));
            mEndPos = trap((int)(mTouchInitialEndPos + delta));
        } else {
            mEndPos = trap((int)(mTouchInitialEndPos + delta));
            if (mEndPos < mStartPos)
                mEndPos = mStartPos;
        }

        updateDisplay();
    }

    @Override
    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == mStartMarker) {
            setOffsetGoalStart();
        } else {
            setOffsetGoalEnd();
        }
    }

    @Override
    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerEnter(MarkerView marker) {
    }

    @Override
    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    @Override
    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == mStartMarker) {
            setOffsetGoalStartNoUpdate();
        } else {
            setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDisplay();
            }
        }, 100);
    }
}
