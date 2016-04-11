package com.bkromhout.minerva.data;

import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.util.Util;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class containing methods to help with storing/retrieving cover images.
 */
public class CoverHelper {
    /**
     * Extension to append to cover image files.
     */
    private static final String COVER_EXT = ".cover";
    /**
     * Instance.
     */
    private static CoverHelper INSTANCE = null;
    /**
     * Directory where cover image files are stored.
     */
    private File dir = null;

    public static CoverHelper get() {
        if (INSTANCE == null) INSTANCE = new CoverHelper();
        return INSTANCE;
    }

    // Private constructor.
    private CoverHelper() {
        dir = Minerva.getAppCtx().getFilesDir();
    }

    /**
     * Save an input stream to a file with the ".cover" extension.
     * @param in      Input stream.
     * @param relPath Relative path to use to create cover file path.
     */
    public void saveStreamAsCoverImage(InputStream in, String relPath) {
        File coverFile = new File(dir, relPath + COVER_EXT);
        try {
            Files.createParentDirs(coverFile);
            try (FileOutputStream out = new FileOutputStream(coverFile)) {
                ByteStreams.copy(in, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete the cover image saved using the given relative path, if there is one.
     * @param relPath Relative path which was used to create cover file path.
     */
    public void deleteCoverImage(String relPath) {
        File coverFile = Util.getFileFromRelPath(dir, relPath + COVER_EXT);
        if (coverFile == null || !coverFile.exists()) return;
        coverFile.delete();
    }

    /**
     * Get the cover file saved using the given relative path, if there is one.
     * @param relPath Relative path which was used to create cover file path.
     * @return Cover file, or null.
     */
    public File getCoverImageFile(String relPath) {
        return Util.getFileFromRelPath(dir, relPath + COVER_EXT);
    }
}
