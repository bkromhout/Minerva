package com.bkromhout.minerva.data;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.enums.ModelType;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.ruqus.RealmUserQuery;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.realm.Realm;
import io.realm.RealmResults;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.epub.EpubReader;
import rx.Observable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of methods used when operating on our data sets.
 */
public class DataUtils {
    /**
     * Extension to append to cover image files.
     */
    private static final String COVER_EXT = ".cover";

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
     * Take a string which may or may not contain HTML tags and changes it into a spanned string which respects HTML
     * tags.
     * @param s String to convert to a Spanned.
     * @return Spanned string, or {@code null} if {@code s} is {@code null} or empty.
     */
    public static Spanned toSpannedHtml(String s) {
        if (s == null || s.isEmpty()) return null;
        return Html.fromHtml(s);
    }

    /**
     * Take a string which may or may not contain HTML tags and remove them. Makes no attempts to do any other
     * cleaning.
     * @param s String to strip of HTML tags.
     * @return Stripped string, or {@code null} if {@code s} is {@code null} or empty.
     */
    public static String stripHtmlTags(String s) {
        if (s == null || s.isEmpty()) return null;
        return Html.fromHtml(s).toString();
    }

    /**
     * Save an input stream to a file with the ".cover" extension.
     * @param in      Input stream.
     * @param relPath Relative path to use to create cover file path.
     */
    public static void saveStreamAsCoverImage(InputStream in, String relPath) {
        File coverFile = new File(Minerva.get().getFilesDir(), relPath + COVER_EXT);
        try {
            Files.createParentDirs(coverFile);
            try (FileOutputStream out = new FileOutputStream(coverFile)) {
                ByteStreams.copy(in, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete the cover image saved using the given relative path, if there is one.
     * @param relPath Relative path which was used to create cover file path.
     */
    public static void deleteCoverImage(String relPath) {
        File coverFile = Util.getFileFromRelPath(Minerva.get().getFilesDir(), relPath + COVER_EXT);
        if (coverFile == null || !coverFile.exists()) return;
        //noinspection ResultOfMethodCallIgnored
        coverFile.delete();
    }

    /**
     * Get the cover file saved using the given relative path, if there is one.
     * @param relPath Relative path which was used to create cover file path.
     * @return Cover file, or null.
     */
    public static File getCoverImageFile(String relPath) {
        return Util.getFileFromRelPath(Minerva.get().getFilesDir(), relPath + COVER_EXT);
    }

    /**
     * Get the default cover image as a bitmap so that the system can scale it easier than it otherwise could as a
     * LayerDrawable.
     * @return Bitmap created using LayerDrawable.
     */
    public static Bitmap getDefaultCoverImage() {
        Drawable d = Minerva.get().getDrawable(R.drawable.default_cover);
        if (d == null) throw new IllegalStateException("Couldn't get default cover drawable");
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        // TODO may want to cache this somehow so we don't have to do this each time?
        return bitmap;
    }

    /**
     * Get names of {@link RBookList}s which {@code book} is in.
     * @param book  Book to search for.
     * @param realm Instance of Realm to use.
     * @return List of list names.
     */
    public static List<String> listsBookIsIn(RBook book, Realm realm) {
        List<String> listNames = new ArrayList<>();
        if (book != null) {
            // Check all list items which contain this book (only the set of all normal lists will be represented here).
            RealmResults<RBookListItem> listItems = realm.where(RBookListItem.class)
                                                         .equalTo("book.relPath", book.relPath)
                                                         .findAll();
            // For each RBookListItem, add the name of the owning RBookList.
            for (int i = listItems.size() - 1; i >= 0; i--) listNames.add(listItems.get(i).owningList.name);

            // Search through smart lists too, but do it a bit differently.
            RealmResults<RBookList> smartLists = realm.where(RBookList.class)
                                                      .equalTo("isSmartList", true)
                                                      .findAllSorted("name");
            // For each smart list:
            for (RBookList smartList : smartLists) {
                if (smartList.smartListRuqString == null || smartList.smartListRuqString.isEmpty()) continue;
                // Get the query and its type.
                RealmUserQuery ruq = new RealmUserQuery(smartList.smartListRuqString);
                ModelType at = ModelType.fromRealmClass(ruq.getQueryClass());

                if (at == ModelType.BOOK) {
                    // For RBook-type queries, we can simply check if the results contain the book.
                    if (ruq.execute(realm).contains(book)) listNames.add(smartList.name);
                } else if (at == ModelType.BOOK_LIST_ITEM) {
                    // For RBookListItem-type queries, we have to do another Realm query to see if there's an
                    // RBookListItem for the book.
                    if (ruq.execute(realm).where().equalTo("book.relPath", book.relPath).findFirst() != null)
                        listNames.add(smartList.name);
                }
            }
        }
        return listNames;
    }

    /**
     * Convert the given {@code listItems} to a list of {@link RBook}s.
     * @param listItems A list of {@link RBookListItem}s.
     * @return List of {@link RBook}s.
     */
    public static List<RBook> booksFromBookListItems(List<RBookListItem> listItems) {
        if (listItems == null) throw new IllegalArgumentException("listItems may not be null.");
        if (listItems.isEmpty()) return new ArrayList<>();

        ArrayList<RBook> books = new ArrayList<>(listItems.size());
        for (RBookListItem listItem : listItems) books.add(listItem.book);
        return books;
    }

    /**
     * Gets an {@link RTag} with the given {@code name}.
     * @param name            Tag name.
     * @param makeNonexistent If true, an {@link RTag} will be made if one doesn't exist with the given {@code name}.
     * @return {@link RTag} with {@code name}, or null if there isn't one and {@code makeNonexistent} is false.
     */
    public static RTag getRTag(String name, boolean makeNonexistent) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name must not be null or empty.");
        try (Realm realm = Realm.getDefaultInstance()) {
            // Try to find existing tag with name.
            RTag tag = realm.where(RTag.class).equalTo("name", name).findFirst();
            if (tag != null || !makeNonexistent) return tag;

            // If we didn't have an existing tag, we'll need to create a new one.
            realm.beginTransaction();
            tag = realm.copyToRealm(new RTag(name));
            realm.commitTransaction();
            return tag;
        }
    }

    /**
     * Creates a list of {@link RTag}s from a list of strings.
     * @param strings         List of strings.
     * @param makeNonexistent If true, strings which aren't the name of any existing {@link RTag}s will cause new {@link
     *                        RTag}s to be made.
     * @return List of {@link RTag}s, or {@code null} if {@code strings} is null.
     */
    public static List<RTag> stringListToTagList(List<String> strings, boolean makeNonexistent) {
        if (strings == null) return null;
        return Observable.from(strings)
                         .map(string -> getRTag(string, makeNonexistent))
                         .filter(tag -> tag != null)
                         .toList()
                         .toBlocking()
                         .single();
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
