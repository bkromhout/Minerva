package com.bkromhout.minerva.test;

import com.bkromhout.minerva.data.SuperBook;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.domain.Metadata;

import java.util.Collections;

/**
 * Utility class which creates SuperBooks for testing.
 */
public class TestBookFactory {
    private long count;

    public TestBookFactory() {
        this.count = 0L;
    }

    /**
     * Create a new {@link SuperBook}.
     * @return A {@link SuperBook} guaranteed to be different than the one returned by the last call to this method for
     * the current instance of {@link TestBookFactory}.
     */
    public final SuperBook produceSuperBook() {
        count += 1L;
        String title = "Test Book " + count;
        Book book = new Book();
        Metadata metadata = book.getMetadata();
        metadata.addTitle(title);
        metadata.addAuthor(new Author("Author " + count));
        metadata.addDescription("Description " + count);
        metadata.setSubjects(Collections.singletonList("Subject " + count));
        metadata.addType("Type " + count);
        metadata.setFormat("application/epub+zip");
        metadata.setLanguage("en");
        metadata.addPublisher("Publisher " + count);
        metadata.addDate(new Date("Create Date " + count, Date.Event.CREATION));
        metadata.addDate(new Date("Publish Date " + count, Date.Event.PUBLICATION));
        metadata.addDate(new Date("Modified Date " + count, Date.Event.MODIFICATION));
        return new SuperBook(book, "path/to/book_" + count, title.getBytes());
    }
}
