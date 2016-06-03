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
    /**
     * Number currently being used to help generate a {@link TestSuperBook}.
     */
    private long genNum;

    public TestBookFactory() {
        this.genNum = 0L;
    }

    /**
     * Create a new {@link SuperBook} wrapped in a {@link TestSuperBook}.
     * @return A {@link TestSuperBook} guaranteed to be different than the one returned by the last call to this method
     * for the current instance of {@link TestBookFactory}.
     */
    public final TestSuperBook generate() {
        genNum += 1L;
        String title = "Test Book " + genNum;
        Book book = new Book();
        Metadata metadata = book.getMetadata();
        metadata.addTitle(title);
        metadata.addAuthor(new Author("Author " + genNum));
        metadata.addDescription("Description " + genNum);
        metadata.setSubjects(Collections.singletonList("Subject " + genNum));
        metadata.addType("Type " + genNum);
        metadata.setFormat("application/epub+zip");
        metadata.setLanguage("en");
        metadata.addPublisher("Publisher " + genNum);
        metadata.addDate(new Date("Create Date " + genNum, Date.Event.CREATION));
        metadata.addDate(new Date("Publish Date " + genNum, Date.Event.PUBLICATION));
        metadata.addDate(new Date("Modified Date " + genNum, Date.Event.MODIFICATION));
        return new TestSuperBook(book, "path/to/book_" + genNum, title.getBytes(), genNum);
    }
}
