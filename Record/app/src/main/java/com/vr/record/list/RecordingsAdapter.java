package com.vr.record.list;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.vr.record.R;
import com.vr.record.recorder.RecordActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {

    private static final String TAG = RecordingsAdapter.class.getSimpleName();

    private final Activity activity;
    private final RecordingsDatabase recordingsDatabase;
    private final SimpleDateFormat dateFormat;
    private final OnRemovedAllListener onRemovedAllListener;

    private List<Recording> recordingList;
    private int requestCodeEdit;

    public interface OnRemovedAllListener {
        void onRemovedAll();
    }

    public RecordingsAdapter(Activity activity, OnRemovedAllListener onRemovedAllListener,
                             int requestCodeEdit) {
        this.activity = activity;
        this.onRemovedAllListener = onRemovedAllListener;
        this.requestCodeEdit = requestCodeEdit;
        this.recordingsDatabase = new RecordingsDatabase(activity);
        this.dateFormat = new SimpleDateFormat("kk:mm:ss dd.MM.yyyy");

        loadRecordings();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recording_item, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        final Recording recording = recordingList.get(position);
        viewHolder.getNameView().setText(recording.getName());
        final Long timestamp = recording.getTimestamp();
        viewHolder.getDateView().setText(getFormattedDate(timestamp));
        viewHolder.getDeleteButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "remove recording + " + timestamp);

                recordingsDatabase.deleteRecording(timestamp);
                removeFile(timestamp);

                loadRecordings();

                notifyDataSetChanged();
            }
        });

        viewHolder.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(activity, RecordActivity.class);
                intent.setAction(RecordActivity.ACTION_EDIT);
                intent.putExtra(RecordActivity.RECORDING, recording);
                activity.startActivityForResult(intent, requestCodeEdit);
            }
        });
    }

    private void removeFile(Long timestamp) {
        File file = new File(activity.getExternalFilesDir(null) + "/recordings/" + timestamp + ".wav");
        final boolean delete = file.delete();
        Log.e(TAG, "deleted file " + file + " " + delete);
    }

    private String getFormattedDate(Long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    public void loadRecordings() {
        recordingList = recordingsDatabase.getAllRecordings();
        if (recordingList.size() == 0 && onRemovedAllListener != null) {
            onRemovedAllListener.onRemovedAll();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final View view;
        private final TextView name;
        private final TextView date;
        private final ImageButton delete;

        public ViewHolder(View view) {
            super(view);

            this.view = view;
            this.name = (TextView) view.findViewById(R.id.name);
            this.date = (TextView) view.findViewById(R.id.date);
            this.delete = (ImageButton) view.findViewById(R.id.delete);
        }

        public View getView() {
            return view;
        }

        public TextView getNameView() {
            return name;
        }

        public TextView getDateView() {
            return date;
        }

        public ImageButton getDeleteButton() {
            return delete;
        }
    }
}