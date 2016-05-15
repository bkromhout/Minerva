package com.bkromhout.minerva.data;

import nl.siegmann.epublib.domain.Book;

/**
 * Holds a {@link nl.siegmann.epublib.domain.Book}, plus other data.
 */
public class SuperBook {
    private final Book book;
    private final String path;
    private final byte[] hash;

    /**
     * Create a new {@link SuperBook}.
     * @param book The {@link Book} to hold.
     * @param path The path (relative to the library dir) to the book file.
     * @param hash The hash of the book file.
     */
    public SuperBook(Book book, String path, byte[] hash) {
        if (book == null || book.getTitle() == null || book.getTitle().isEmpty() ||
                DataUtils.getFirstAuthor(book) == null || path == null || path.isEmpty())
            throw new IllegalArgumentException(path);

        this.book = book;
        this.path = path;
        this.hash = hash;
    }

    public Book getBook() {
        return book;
    }

    public String getPath() {
        return path;
    }

    public byte[] getHash() {
        return hash;
    }
}
