package com.vr.record.Recognizer;

import android.app.ProgressDialog;
import android.os.Vibrator;
import android.widget.TextView;

import com.vr.record.MainActivity;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Marek on 2016-04-10.
 */
public class ResultsManager {

    private static Vibrator vibratorRef;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm:ss");
    private static ProgressDialog progressBarRef;

    private static TextView textViewRef;
    public static void setTextViewReference(TextView textViewReference){
        textViewRef=textViewReference;
    }

    public static void setRecognizedSound(String sound)
    {
        if(textViewRef==null)
        {
            return;
        }
        textViewRef.setText(sound+ " at: "+getFormattedDate());

        if(vibratorRef!=null){
            vibratorRef.vibrate(100);
        }
    }

    private static String getFormattedDate() {
        return dateFormat.format(new Date());
    }

    public static void setVibrator(Vibrator vibrator)
    {
        vibratorRef=vibrator;
    }

    public static void setProgressBar(ProgressDialog progressBar){
        progressBarRef = progressBar;
    }

    public static void activateProgressbar()
    {
        if(progressBarRef!=null){
            progressBarRef.show();
        }
    }

    public static void deactivateProgressbar(){
        if(progressBarRef!=null){
            progressBarRef.dismiss();
        }
    }
}
