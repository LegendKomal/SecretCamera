package com.example.secretcamera;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private Context context;
    private List<File> videoFiles;
    private OnVideoClickListener listener;
    private OnVideoDeleteListener deleteListener;

    public interface OnVideoClickListener {
        void onVideoClick(File videoFile);
    }

    public interface OnVideoDeleteListener {
        void onVideoDelete(File videoFile, int position);
    }

    public VideoAdapter(Context context, List<File> videoFiles, OnVideoClickListener listener, OnVideoDeleteListener deleteListener) {
        this.context = context;
        this.videoFiles = videoFiles;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        File videoFile = videoFiles.get(position);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getAbsolutePath());
            holder.thumbnail.setImageBitmap(retriever.getFrameAtTime());
        } catch (Exception e) {
            e.printStackTrace();
            holder.thumbnail.setImageResource(android.R.drawable.ic_media_play);
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        holder.videoName.setText(videoFile.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(videoFile);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Video")
                    .setMessage("Are you sure you want to delete this video?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (deleteListener != null) {
                            deleteListener.onVideoDelete(videoFile, position);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return videoFiles.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView videoName;
        ImageButton deleteButton;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.videoThumbnail);
            videoName = itemView.findViewById(R.id.videoName);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public void updateVideos(List<File> newVideoFiles) {
        this.videoFiles = newVideoFiles;
        notifyDataSetChanged();
    }
}