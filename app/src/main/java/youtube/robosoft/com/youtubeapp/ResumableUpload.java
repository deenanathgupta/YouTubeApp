package youtube.robosoft.com.youtubeapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import youtube.robosoft.com.youtubeapp.util.Upload;

/**
 * Created by deena on 31/3/16.
 */
public class ResumableUpload {
    private static final String SUCCEEDED = "succeeded";
    private static final String TAG = "UploadingActivity";
    private static int UPLOAD_NOTIFICATION_ID = 1001;
    private static int PLAYBACK_NOTIFICATION_ID = 1002;
    /*
     * Global instance of the format used for the video being uploaded (MIME type).
     */
    private static String VIDEO_FILE_FORMAT = "video/*";

    public static String upload(YouTube youtube, final InputStream fileInputStream,
                                final long fileSize, final Uri mFileUri, final String path, final Context context) {
        final NotificationManager notifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setData(mFileUri);
        notificationIntent.setAction(Intent.ACTION_VIEW);
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentTitle(context.getString(R.string.youtube_upload))
                .setContentText(context.getString(R.string.youtube_upload_started))
                .setSmallIcon(R.drawable.ic_stat_device_access_video).setContentIntent(contentIntent).setStyle(new NotificationCompat.BigPictureStyle().bigPicture(thumbnail));
        notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
        Log.i("Upload", "Inside upload()");
        String videoId = null;
        try {
            // Add extra information to the video before uploading.
            Video videoObjectDefiningMetadata = new Video();

      /*
       * Set the video to public, so it is available to everyone (what most people want). This is
       * actually the default, but I wanted you to see what it looked like in case you need to set
       * it to "unlisted" or "private" via API.
       */
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);
            Log.i("Upload", "Inside upload()");
            // We set a majority of the metadata with the VideoSnippet object.
            VideoSnippet snippet = new VideoSnippet();

      /*
       * The Calendar instance is used to create a unique name and description for test purposes, so
       * you can see multiple files being uploaded. You will want to remove this from your project
       * and use your own standard names.
       */
            Calendar cal = Calendar.getInstance();
            snippet.setTitle("Test Upload via Java on " + cal.getTime());
            snippet.setDescription("Video uploaded via YouTube Data API V3 using the Java library "
                    + "on " + cal.getTime());

            // Set your keywords.
            snippet.setTags(Arrays.asList(Constants.DEFAULT_KEYWORD, Upload.generateKeywordFromPlaylistId(Constants.UPLOAD_PLAYLIST)));

            // Set completed snippet to the video object.
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent =
                    new InputStreamContent(VIDEO_FILE_FORMAT, new BufferedInputStream(fileInputStream));
            mediaContent.setLength(fileSize);
            Log.i("Upload", "Inside upload() filesize " + fileSize);
      /*
       * The upload command includes: 1. Information we want returned after file is successfully
       * uploaded. 2. Metadata we want associated with the uploaded video. 3. Video file itself.
       */
            YouTube.Videos.Insert videoInsert =
                    youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata,
                            mediaContent);
            Log.i("Upload", "Inside upload() Medialength " + mediaContent.getLength());

            // Set the upload type and add event listener.
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

      /*
       * Sets whether direct media upload is enabled or disabled. True = whole media content is
       * uploaded in a single request. False (default) = resumable media upload protocol to upload
       * in data chunks.
       */
            uploader.setDirectUploadEnabled(false);
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            Log.i("Upload", "Inside upload() INITIATION_STARTED ");
                            builder.setContentText(context.getString(R.string.initiation_started)).setProgress((int) fileSize,
                                    (int) uploader.getNumBytesUploaded(), false);
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
                            Log.i("Upload", "Inside upload() INITIATION_STARTED !!!!");
                            break;
                        case INITIATION_COMPLETE:
                            Log.i("Upload", "Inside upload() INITIATION_COMPLETE ");
                            builder.setContentText(context.getString(R.string.initiation_completed)).setProgress((int) fileSize,
                                    (int) uploader.getNumBytesUploaded(), false);
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
                            break;
                        case MEDIA_IN_PROGRESS:
                            Log.i("Upload", "Inside upload() MEDIA_IN_PROGRESS ");
                            builder
                                    .setContentTitle(context.getString(R.string.youtube_upload) +
                                            (int) (uploader.getProgress() * 100) + "%")
                                    .setContentText(context.getString(R.string.upload_in_progress))
                                    .setProgress((int) fileSize, (int) uploader.getNumBytesUploaded(), false);
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
                            break;
                        case MEDIA_COMPLETE:
                            Log.i("Upload", "Inside upload() MEDIA_COMPLETE ");
                            builder.setContentTitle(context.getString(R.string.yt_upload_completed))
                                    .setContentText(context.getString(R.string.upload_completed))
                                            // Removes the progress bar
                                    .setProgress(0, 0, false);
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
                        case NOT_STARTED:
                            Log.i("Upload", "Inside upload() NOT_STARTED ");
                            Log.d(this.getClass().getSimpleName(), context.getString(R.string.upload_not_started));
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);
            // Execute upload.
            Video returnedVideo = videoInsert.execute();
            Log.d(TAG, "Video upload completed");
            videoId = returnedVideo.getId();
            Log.d(TAG, String.format("videoId = [%s]", videoId));
        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            Log.e(TAG, "GooglePlayServicesAvailabilityIOException", availabilityException);
            notifyFailedUpload(context, context.getString(R.string.cant_access_play), notifyManager, builder);
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            Log.i(TAG, String.format("UserRecoverableAuthIOException: %s",
                    userRecoverableException.getMessage()));
            requestAuth(context, userRecoverableException);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            notifyFailedUpload(context, context.getString(R.string.please_try_again), notifyManager, builder);
        }
        return videoId;
    }

    private static void notifyFailedUpload(Context context, String message, NotificationManager notifyManager,
                                           NotificationCompat.Builder builder) {
        builder.setContentTitle(context.getString(R.string.yt_upload_failed))
                .setContentText(message);
        notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
        Log.e(ResumableUpload.class.getSimpleName(), message);
    }

    private static void requestAuth(Context context,
                                    UserRecoverableAuthIOException userRecoverableException) {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        Intent authIntent = userRecoverableException.getIntent();
        Intent runReqAuthIntent = new Intent(MainActivity.REQUEST_AUTHORIZATION_INTENT);
        runReqAuthIntent.putExtra(MainActivity.REQUEST_AUTHORIZATION_INTENT_PARAM, authIntent);
        manager.sendBroadcast(runReqAuthIntent);
        Log.d(TAG, String.format("Sent broadcast %s", MainActivity.REQUEST_AUTHORIZATION_INTENT));
    }

    public static void showSelectableNotification(String videoId, Context context) {
        Log.d(TAG, String.format("Posting selectable notification for video ID [%s]", videoId));
        final NotificationManager notifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, ReviewVideoActivity.class);
        notificationIntent.putExtra(MainActivity.YOUTUBE_ID, videoId);
        notificationIntent.setAction(Intent.ACTION_VIEW);

        URL url;
        try {
            url = new URL("https://i1.ytimg.com/vi/" + videoId + "/mqdefault.jpg");
            Bitmap thumbnail = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            PendingIntent contentIntent = PendingIntent.getActivity(context,
                    0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentTitle(context.getString(R.string.watch_your_video))
                    .setContentText(context.getString(R.string.see_the_newly_uploaded_video)).setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_stat_device_access_video).setStyle(new NotificationCompat.BigPictureStyle().bigPicture(thumbnail));
            notifyManager.notify(PLAYBACK_NOTIFICATION_ID, builder.build());
            Log.d(TAG, String.format("Selectable notification for video ID [%s] posted", videoId));
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static boolean checkIfProcessed(String videoId, YouTube youtube) {
        try {
            YouTube.Videos.List list = youtube.videos().list("processingDetails");
            list.setId(videoId);
            VideoListResponse listResponse = list.execute();
            List<Video> videos = listResponse.getItems();
            if (videos.size() == 1) {
                Video video = videos.get(0);
                String status = video.getProcessingDetails().getProcessingStatus();
                Log.e(TAG, String.format("Processing status of [%s] is [%s]", videoId, status));
                if (status.equals(SUCCEEDED)) {
                    return true;
                }
            } else {
                // can't find the video
                Log.e(TAG, String.format("Can't find video with ID [%s]", videoId));
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching video metadata", e);
        }
        return false;
    }

}
