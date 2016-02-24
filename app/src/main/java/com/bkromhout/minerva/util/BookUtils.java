package com.bkromhout.minerva.util;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;

/**
 * Utility methods for working with {@link nl.siegmann.epublib.domain.Book} objects.
 * @see Book
 */
public class BookUtils {

    /**
     * Get the first non-empty author from the given {@link Book}.
     * @param book Book.
     * @return Author string as "FirstName LastName" (or just first or last name, if only one is filled in). If {@code
     * book} is null, or has no authors, or author names are empty strings, returns null.
     */
    public static String getFirstAuthor(Book book) {
        if (book == null || book.getMetadata().getAuthors().isEmpty()) return null;

        // Loop through authors to get first non-empty one.
        for (Author author : book.getMetadata().getAuthors()) {
            String fName = author.getFirstname();
            String lName = author.getLastname();
            // Skip this author now if it doesn't have a non-null, non-empty name.
            if ((fName == null || fName.isEmpty()) && (lName == null || lName.isEmpty())) continue;

            // Return the name, which might only use one of the strings.
            if (fName == null || fName.isEmpty()) return lName;
            if (lName == null || lName.isEmpty()) return fName;
            return fName + " " + lName;
        }
        return null;
    }

    /**
     * Get the first non-empty description from the given {@link Book}.
     * @param book Book.
     * @return Description string, or null if the {@code book} is null or has no non-empty description.
     */
    public static String getFirstDesc(Book book) {
        if (book == null || book.getMetadata().getDescriptions().isEmpty()) return null;

        for (String desc : book.getMetadata().getDescriptions()) if (!desc.isEmpty()) return desc;
        return null;
    }

    /**
     * Get the first non-empty publisher from the given {@link Book}.
     * @param book Book.
     * @return Publisher string, or null if the {@code book} is null or has no non-empty publisher.
     */
    public static String getFirstPublisher(Book book) {
        if (book == null || book.getMetadata().getPublishers().isEmpty()) return null;

        for (String pub : book.getMetadata().getPublishers()) if (!pub.isEmpty()) return pub;
        return null;
    }

    /**
     * Get the first non-empty {@link nl.siegmann.epublib.domain.Date} from the given {@link Book}.
     * @param book Book.
     * @param type {@link nl.siegmann.epublib.domain.Date.Event} type.
     * @return Date string, or null if {@code book} is null or doesn't have a non-empty date of the given {@code type}.
     */
    public static String getFirstBookDate(Book book, Date.Event type) {
        if (book == null || book.getMetadata().getDates().isEmpty()) return null;

        for (Date bookDate : book.getMetadata().getDates()) {
            if (bookDate.getEvent() != type || bookDate.getValue().isEmpty()) continue;
            return bookDate.getValue();
        }
        return null;
    }
}
