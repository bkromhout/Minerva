package com.bkromhout.minerva.test;

import com.bkromhout.minerva.data.SuperBook;
import nl.siegmann.epublib.domain.Book;

/**
 * Extension of the {@link SuperBook} class which stores the number used to generate it.
 */
public class TestSuperBook extends SuperBook {
    private final long genNum;

    /**
     * Create a new {@link SuperBook} wrapped in a {@link TestSuperBook}
     * @param book The {@link Book} to hold.
     * @param path The path (relative to the library dir) to the book file.
     * @param hash The hash of the book file.
     */
    public TestSuperBook(Book book, String path, byte[] hash, long genNum) {
        super(book, path, hash);
        this.genNum = genNum;
    }

    public long getGenNum() {
        return genNum;
    }
}
