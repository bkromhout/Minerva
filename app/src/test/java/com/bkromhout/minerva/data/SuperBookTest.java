package com.bkromhout.minerva.data;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import org.junit.Test;

/**
 * Tests the {@link SuperBook} class.
 */
public class SuperBookTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullBookThrows() {
        new SuperBook(null, "fake", "null book".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyBookThrows() {
        new SuperBook(new Book(), "fake", "empty book".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTitleBookThrows() {
        Book book = new Book();
        book.getMetadata().addTitle("");
        new SuperBook(book, "fake", "empty title book".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noAuthorBookThrows() {
        String title = "Book without author";
        Book book = new Book();
        book.getMetadata().addTitle(title);
        new SuperBook(book, "fake", title.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyAuthorBookThrows() {
        String title = "Book with empty author";
        Book book = new Book();
        book.getMetadata().addTitle(title);
        book.getMetadata().addAuthor(new Author(""));
        new SuperBook(book, "fake", title.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noPathBookThrows() {
        String title = "Book without path";
        Book book = new Book();
        book.getMetadata().addTitle(title);
        book.getMetadata().addAuthor(new Author("Book", "Author"));
        new SuperBook(book, null, title.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPathBookThrows() {
        String title = "Book with empty path";
        Book book = new Book();
        book.getMetadata().addTitle(title);
        book.getMetadata().addAuthor(new Author("Book", "Author"));
        new SuperBook(book, "", title.getBytes());
    }
}
