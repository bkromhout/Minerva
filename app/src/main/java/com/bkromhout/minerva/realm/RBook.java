package com.bkromhout.minerva.realm;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.SuperBook;
import com.bkromhout.minerva.prefs.DBPrefs;
import com.bkromhout.minerva.util.BookUtils;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Represents a book in Realm.
 */
public class RBook extends RealmObject {
    public static final String SUBJECT_STR_SEP = ";SUBJECT_STR_SEP;";
    public static final String TYPE_STR_SEP = ";TYPE_STR_SEP;";
    /**
     * Path to book file (relative to library directory).
     */
    @PrimaryKey
    private String relPath;
    /**
     * Book title from epub. (We only get the first one for this)
     */
    @Index
    @Required
    private String title;
    /**
     * Book author from epub. (We only get the first one for this)
     */
    @Index
    @Required
    private String author;
    /**
     * Book description from epub.
     */
    private String desc;
    /**
     * Book subjects from epub. Comma separated.
     */
    private String subjects;
    /**
     * Book types from epub. Comma separated.
     */
    private String types;
    /**
     * Book format from epub.
     */
    private String format;
    /**
     * Book language from epub.
     */
    private String language;
    /**
     * Book publisher from epub. (We only get the first one for this)
     */
    private String publisher;
    /**
     * Book Identifier from epub. (We only get the first one for this)
     * <p>
     * Note that this is just simply whatever is set in the epub, we can't guarantee that it's unique!
     */
    private String bookId;
    /**
     * Date that the epub reports it was created.
     */
    private String createDate;
    /**
     * Date that the epub reports it was published.
     */
    private String pubDate;
    /**
     * Date that the epub reports it was modified.
     */
    private String modDate;
    /**
     * Number of chapters in the book. We get this number by getting the size of the TOC... epub is a weird standard, so
     * that might not work.
     */
    private int numChaps;
    /**
     * Whether or not the book has a cover image.
     */
    private boolean hasCoverImage;

    /* Below this are variables which hold data that are not read from the ePub file. */

    /**
     * Hash of file from the last time it was imported.
     */
    private String hash;
    /**
     * Date that the data for this book was last modified.
     */
    private Date lastModifiedDate;
    /**
     * Date that the epub file was last read.
     */
    private Date lastImportDate;
    /**
     * Date that the book was last opened to read.
     */
    private Date lastReadDate;
    /**
     * Whether or not the book should be shown in the {@link com.bkromhout.minerva.fragments.RecentFragment}.
     */
    private boolean isInRecents;
    /**
     * List of {@link RTag}s attached to this book.
     */
    private RealmList<RTag> tags;
    /**
     * Assigned rating.
     */
    @Index
    private int rating;
    /**
     * A unique long value.
     */
    @Index
    private long uniqueId;

    /**
     * Create a default {@link RBook}.
     * <p>
     * Note: This really shouldn't ever be called, it's only here because it has to be for Realm. If a new {@link RBook}
     * is created using this, it risks a situation where we have primary key collisions.
     */
    public RBook() {
        // VERY BAD!!
        this.relPath = "DEF_REL_PATH";
        this.title = "DEF_TITLE";
        this.author = "DEF_AUTHOR";
        this.rating = 0;
        this.lastModifiedDate = Calendar.getInstance().getTime();
        this.uniqueId = DBPrefs.get().getNextRBookUid();
    }

    /**
     * Create an {@link RBook} from a {@link SuperBook}.
     * <p>
     * {@link SuperBook}s are guaranteed to have a non-null, non-empty path, title, and author.
     * @param superBook The {@link SuperBook} to use.
     */
    public RBook(SuperBook superBook) {
        // Fill in data from SuperBook.
        this.relPath = superBook.getPath();
        this.hash = superBook.getHash();

        // Fill in data from Book file.
        Book book = superBook.getBook();
        this.title = book.getTitle();
        this.author = BookUtils.getFirstAuthor(book);
        this.desc = BookUtils.getFirstDesc(book);
        this.subjects = Util.listToString(book.getMetadata().getSubjects(), SUBJECT_STR_SEP);
        this.types = Util.listToString(book.getMetadata().getTypes(), TYPE_STR_SEP);
        this.format = book.getMetadata().getFormat();
        this.language = book.getMetadata().getLanguage();
        this.publisher = BookUtils.getFirstPublisher(book);
        this.bookId = Identifier.getBookIdIdentifier(book.getMetadata().getIdentifiers()).toString();
        this.createDate = BookUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.CREATION);
        this.pubDate = BookUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.PUBLICATION);
        this.modDate = BookUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.MODIFICATION);
        this.numChaps = book.getTableOfContents().getAllUniqueResources().size();
        this.hasCoverImage = book.getCoverImage() != null; // TODO test to make sure...

        // Fill in other data.
        this.lastImportDate = Calendar.getInstance().getTime();
        this.lastModifiedDate = lastImportDate;
        this.lastReadDate = null;
        this.isInRecents = false;
        this.rating = 0;
        this.uniqueId = DBPrefs.get().getNextRBookUid();
    }

    /**
     * Update this {@link RBook} with the given {@code otherBook}'s data. However, we only copy the data that was read
     * from the book's file, not the data which we've filled in.
     * <p>
     * IMPORTANT: Only call this method from within a Realm transaction. ({@code realm} will be checked to enforce
     * this!)
     * @param realm     The Realm instance handling the transaction that this call is within.
     * @param otherBook {@link RBook} to copy data from.
     */
    public void updateFromOtherRBook(Realm realm, RBook otherBook) {
        if (realm == null || otherBook == null) throw new IllegalArgumentException("realm, otherBook cannot be null.");
        if (!realm.isInTransaction())
            throw new IllegalStateException("You must call this method from within a Realm transaction.");

        setHash(otherBook.getHash());

        setTitle(otherBook.getTitle());
        setAuthor(otherBook.getAuthor());
        setDesc(otherBook.getDesc());
        setSubjects(otherBook.getSubjects());
        setTypes(otherBook.getTypes());
        setFormat(otherBook.getFormat());
        setLanguage(otherBook.getLanguage());
        setPublisher(otherBook.getPublisher());
        setBookId(otherBook.getBookId());
        setCreateDate(otherBook.getCreateDate());
        setPubDate(otherBook.getPubDate());
        setModDate(otherBook.getModDate());
        setNumChaps(otherBook.getNumChaps());
        setHasCoverImage(otherBook.isHasCoverImage()); // TODO Change the cover image.

        setLastImportDate(otherBook.getLastImportDate());
        setLastModifiedDate(otherBook.getLastModifiedDate());
    }

    /**
     * Open the underlying file in some application. This will also put the book at the top of the recents list.
     * @param context The context to use to open the file.
     */
    public void openFileUsingIntent(Context context) {
        File file = Util.getFileFromRelPath(getRelPath());
        // TODO make the user aware that the underlying file doesn't exist!
        if (file == null) return;

        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.fromFile(file),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(Util.getExtFromFName(file.getName())));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (Realm realm = Realm.getDefaultInstance()) {
            context.startActivity(newIntent);
            realm.executeTransaction(tRealm -> {
                this.setLastReadDate(Calendar.getInstance().getTime());
                this.setInRecents(true);
            });
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, C.getStr(R.string.toast_err_no_apps), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Removes the {@link RBook}s from Realm, and returns the relative paths from each one.
     * @param books List of {@link RBook}s to delete.
     * @return List of relative paths.
     */
    public static List<String> deleteBooks(List<RBook> books) {
        if (books == null) return null;
        if (books.isEmpty()) return new ArrayList<>();

        // Delete any RBookListItems which may exist for these books.
        RBookListItem.deleteAnyForBooks(books);

        List<String> relPaths = new ArrayList<>(books.size());
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                for (RBook book : books) {
                    relPaths.add(book.getRelPath());
                    book.removeFromRealm();
                }
            });
        }
        return relPaths;
    }

    public String getRelPath() {
        return relPath;
    }

    public void setRelPath(String relPath) {
        this.relPath = relPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getModDate() {
        return modDate;
    }

    public void setModDate(String modDate) {
        this.modDate = modDate;
    }

    public int getNumChaps() {
        return numChaps;
    }

    public void setNumChaps(int numChaps) {
        this.numChaps = numChaps;
    }

    public boolean isHasCoverImage() {
        return hasCoverImage;
    }

    public void setHasCoverImage(boolean hasCoverImage) {
        this.hasCoverImage = hasCoverImage;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getLastImportDate() {
        return lastImportDate;
    }

    public void setLastImportDate(Date lastImportDate) {
        this.lastImportDate = lastImportDate;
    }

    public Date getLastReadDate() {
        return lastReadDate;
    }

    public void setLastReadDate(Date lastReadDate) {
        this.lastReadDate = lastReadDate;
    }

    public boolean isInRecents() {
        return isInRecents;
    }

    public void setInRecents(boolean inRecents) {
        isInRecents = inRecents;
    }

    public RealmList<RTag> getTags() {
        return tags;
    }

    public void setTags(RealmList<RTag> tags) {
        this.tags = tags;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBook)) return false;

        RBook rBook = (RBook) o;

        if (getUniqueId() != rBook.getUniqueId()) return false;
        return getRelPath().equals(rBook.getRelPath());
    }

    @Override
    public int hashCode() {
        int result = getRelPath().hashCode();
        result = 31 * result + (int) (getUniqueId() ^ (getUniqueId() >>> 32));
        return result;
    }
}
