package com.bkp.minerva.epub;

import android.content.res.AssetManager;
import com.bkp.minerva.Minerva;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class Reader {
    // File names of ePub file assets.
    private static final String TEST_EPUB_1 = "Alice in Wonderland.epub";
    private static final String TEST_EPUB_2 = "IN THE YEAR 2889.epub";
    private static final String TEST_EPUB_3 = "The Man Who Would Be King.epub";

    private Reader() {
    }

    public static Reader get() {
        return new Reader();
    }

    public Book getTestBook1() {
        AssetManager assetManager = Minerva.getAppCtx().getAssets();
        try (InputStream in = assetManager.open(TEST_EPUB_1)) {
            return readEpubFile(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get an ePub Book object from an input stream.
     * @param in Input stream.
     * @return Book object, or null if there were issues.
     */
    private Book readEpubFile(InputStream in) {
        if (in == null) return null;
        try {
            return new EpubReader().readEpub(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
