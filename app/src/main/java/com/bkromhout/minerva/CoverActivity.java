package com.bkromhout.minerva;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.realm.RBook;
import com.bumptech.glide.Glide;
import uk.co.senab.photoview.PhotoView;

import java.io.File;

public class CoverActivity extends AppCompatActivity {
    /**
     * Start the {@link CoverActivity} to display the cover image for an {@link RBook}.
     * @param context Context to use to start the activity.
     * @param relPath Relative path from an {@link RBook} which will be used to get the cover image file.
     */
    public static void start(Context context, String relPath) {
        if (relPath == null || relPath.isEmpty())
            throw new IllegalArgumentException("Must supply non-null, non-empty cover image file path.");
        context.startActivity(new Intent(context, CoverActivity.class).putExtra(C.REL_PATH, relPath));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        // Get PhotoView.
        PhotoView coverImage = (PhotoView) findViewById(R.id.full_cover_image);
        if (coverImage == null) throw new IllegalStateException("Couldn't get PhotoView.");

        // Get cover image file.
        File coverImageFile = DataUtils.getCoverImageFile(getIntent().getStringExtra(C.REL_PATH));
        if (coverImageFile == null) throw new IllegalStateException("Couldn't get cover image file.");

        // Load image into PhotoView.
        Glide.with(this)
             .load(coverImageFile)
             .into(coverImage);
    }
}
