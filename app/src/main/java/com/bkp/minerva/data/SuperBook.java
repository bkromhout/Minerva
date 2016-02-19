package com.bkp.minerva.data;

import com.bkp.minerva.util.BookUtils;
import nl.siegmann.epublib.domain.Book;

/**
 * Holds a {@link nl.siegmann.epublib.domain.Book}, plus other data.
 */
public class SuperBook {

    private final Book book;
    private final String path;
    private final String hash;

    /**
     * Create a new {@link SuperBook}.
     * @param book The {@link Book} to hold.
     * @param path The path (relative to the library dir) to the book file.
     * @param hash The hash of the book file.
     */
    public SuperBook(Book book, String path, String hash) {
        if (book == null || book.getTitle() == null || book.getTitle().isEmpty() ||
                BookUtils.getFirstAuthor(book) == null || path == null || path.isEmpty())
            throw new IllegalArgumentException(path);

        this.book = book;
        this.path = path;
        this.hash = hash;
    }

    /**
     * Create a new {@link SuperBook}.
     * @param book The {@link Book} to hold.
     * @param path The path (relative to the library dir) to the book file.
     */
    public SuperBook(Book book, String path) {
        this(book, path, null);
    }

    public Book getBook() {
        return book;
    }

    public String getPath() {
        return path;
    }

    public String getHash() {
        return hash;
    }
}
