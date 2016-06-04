package com.bkromhout.minerva.test;

import com.bkromhout.minerva.data.SuperBook;
import nl.siegmann.epublib.domain.*;

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
        metadata.addTitle(new DcmesElement(title));
        metadata.addAuthor(new Author("Author " + genNum));
        metadata.addDescription(new DcmesElement("Description " + genNum));
        metadata.setSubjects(Collections.singletonList(new DcmesElement("Subject " + genNum)));
        metadata.addType(new DcmesElement("Type " + genNum));
        metadata.setFormat("application/epub+zip");
        metadata.addPublisher(new DcmesElement("Publisher " + genNum));
        metadata.addDate(new Date("Create Date " + genNum, Date.Event.CREATION));
        metadata.addDate(new Date("Publish Date " + genNum, Date.Event.PUBLICATION));
        metadata.addDate(new Date("Modified Date " + genNum, Date.Event.MODIFICATION));
        return new TestSuperBook(book, "path/to/book_" + genNum, title.getBytes(), genNum);
    }
}
