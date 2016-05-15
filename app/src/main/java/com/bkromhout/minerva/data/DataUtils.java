package com.bkromhout.minerva.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.epub.EpubReader;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of methods used when operating on our data sets.
 */
public class DataUtils {
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

    /**
     * Get a {@link SuperBook} object from a file object.
     * @param file The file to try and read as an ePub
     * @return Book object, or null if there were issues.
     */
    public static SuperBook readEpubFile(File file, String relPath) {
        if (file == null || !file.exists() || !file.isFile()) return null;

        try (HashingInputStream in = new HashingInputStream(Hashing.sha256(), new FileInputStream(file))) {
            Book book = new EpubReader().readEpub(in);
            return new SuperBook(book, relPath, in.hash().asBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Take a list of strings and concatenate them, separated by {@code separator}.
     * @param list      List of strings.
     * @param separator What string to use as separators in the output string.
     * @return Concatenated string, or null if the list is null or empty.
     */
    public static String listToString(List<String> list, String separator) {
        if (list == null || list.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next());
            if (iterator.hasNext()) builder.append(separator);
        }
        return builder.toString();
    }

    /**
     * Take a string and split it into a list of strings, splitting after each {@code separator}.
     * @param string    String to split.
     * @param separator Separator to split on.
     * @return List of strings, might be empty.
     */
    public static List<String> stringToList(String string, String separator) {
        List<String> strings = Arrays.asList(string.split("\\Q" + separator + "\\E"));
        if (strings.size() == 1 && strings.get(0).trim().equals("")) return new ArrayList<>();
        return strings;
    }

    /**
     * Takes a String Observable and concatenates its emissions into a single String using StringBuilder, then returns
     * that String.
     * @param stringObservable String observable.
     * @return Concatenated string.
     */
    public static String rxToString(Observable<String> stringObservable) {
        return stringObservable.reduce(new StringBuilder(), StringBuilder::append)
                               .toBlocking()
                               .single()
                               .toString();
    }
}
