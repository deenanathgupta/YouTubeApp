package youtube.robosoft.com.youtubeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class ReviewVideoActivity extends AppCompatActivity {
    VideoView mVideoView;
    MediaController mc;
    private String mChosenAccountName;
    private Uri mFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_video);
        mVideoView = (VideoView) findViewById(R.id.videoview);
        Intent intent = getIntent();
        mFileUri = intent.getData();
        loadAccount();
        reviewVideo(mFileUri);

    }
    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(MainActivity.ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }
    private void reviewVideo(Uri mFileUri) {
        try {
            mc = new MediaController(this);
            mVideoView.setMediaController(mc);
            mVideoView.setVideoURI(mFileUri);
            mc.show();
            mVideoView.start();
        } catch (Exception e) {
            Log.e(this.getLocalClassName(), e.toString());
        }
    }
    public void uploadVideo(View view) {
        Toast.makeText(this,"Clicked",Toast.LENGTH_SHORT).show();
        mChosenAccountName="deenanathgupta@gmail.com";
        if (mChosenAccountName == null) {
            Log.i("Upload",mChosenAccountName+"  Inside IF");
            return;
        }
        // if a video is picked or recorded.
        if (mFileUri != null) {
            Log.i("Upload","Start");
            Log.i("Upload",mChosenAccountName+"  Inside else");
            Toast.makeText(this,"Start",Toast.LENGTH_SHORT).show();
            Intent uploadIntent = new Intent(this, UploadService.class);
            uploadIntent.setData(mFileUri);
            uploadIntent.putExtra(MainActivity.ACCOUNT_KEY, mChosenAccountName);
            startService(uploadIntent);
            Toast.makeText(this, R.string.youtube_upload_started,
                    Toast.LENGTH_LONG).show();
            // Go back to MainActivity after upload
            finish();
            Log.i("Upload", "end");
        }
    }
}
