package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.data.SuperBook;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import com.bkromhout.ruqus.Queryable;
import com.bkromhout.ruqus.VisibleAs;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Represents a book in Realm.
 */
@Queryable(name = "Books")
public class RBook extends RealmObject implements UIDModel {
    public static final String LIST_SEP = ", ";
    /**
     * A unique long value.
     */
    @Index
    @Hide
    public long uniqueId;
    /**
     * Path to book file (relative to library directory).
     */
    @PrimaryKey
    @Required
    @VisibleAs(string = "Relative Path")
    public String relPath;
    /**
     * Book title from epub. (We only get the first one for this)
     */
    @Index
    @Required
    public String title;
    /**
     * Book author from epub. (We only get the first one for this)
     */
    @Index
    @Required
    public String author;
    /**
     * Book description from epub.
     */
    @VisibleAs(string = "Description")
    public String desc;
    /**
     * Book subjects from epub. Comma separated.
     */
    public String subjects;
    /**
     * Book types from epub. Comma separated.
     */
    public String types;
    /**
     * Book format from epub.
     */
    public String format;
    /**
     * Book language from epub.
     */
    public String language;
    /**
     * Book publisher from epub. (We only get the first one for this)
     */
    public String publisher;
    /**
     * Book Identifier from epub. (We only get the first one for this)
     * <p>
     * Note that this is just simply whatever is set in the epub, we can't guarantee that it's unique!
     */
    @VisibleAs(string = "Book ID")
    public String bookId;
    /**
     * Date that the epub reports it was created.
     */
    @Hide
    public String createDate;
    /**
     * Date that the epub reports it was published.
     */
    @VisibleAs(string = "Date Book Published (String)")
    public String pubDate;
    /**
     * Date that the epub reports it was modified.
     */
    @Hide
    public String modDate;
    /**
     * Number of chapters in the book. We get this number by getting the size of the TOC... epub is a weird standard, so
     * that might not work.
     */
    @VisibleAs(string = "Chapter Count")
    public int numChaps;
    /**
     * Whether or not the book has a cover image.
     */
    public boolean hasCoverImage;

    /* Below this are variables which hold data that are not read from the ePub file. */

    /**
     * Hash of file from the last time it was imported.
     */
    public byte[] hash;
    /**
     * Date that the data for this book was last modified.
     */
    public Date lastModifiedDate;
    /**
     * Date that the epub file was last read.
     */
    public Date lastImportDate;
    /**
     * Date that the book was last opened to read.
     */
    public Date lastReadDate;
    /**
     * Whether or not the book should be shown in the {@link com.bkromhout.minerva.fragments.RecentFragment}.
     */
    public boolean isInRecents;
    /**
     * List of {@link RTag}s attached to this book.
     */
    public RealmList<RTag> tags;
    /**
     * Assigned rating.
     */
    @Index
    public int rating;
    /**
     * Whether or not this book is marked as "new". A book is automatically marked as new the first time it's imported.
     */
    public boolean isNew;
    /**
     * Whether or not this book is marked as "updated". A book is automatically marked as updated if its hash changes
     * after being re-imported.
     */
    public boolean isUpdated;

    public RBook() {
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
        this.title = book.getTitle().getValue();
        this.author = DataUtils.getFirstAuthor(book);
        this.desc = DataUtils.getFirstDesc(book);
        this.subjects = DataUtils.concatDEList(book.getMetadata().getSubjects(), LIST_SEP);
        this.types = DataUtils.concatDEList(book.getMetadata().getTypes(), LIST_SEP);
        this.format = book.getMetadata().getFormat();
        this.language = DataUtils.concatDEList(book.getMetadata().getLanguages(), LIST_SEP);
        this.publisher = DataUtils.getFirstPublisher(book);
        Identifier identifier = Identifier.getBookIdIdentifier(book.getMetadata().getIdentifiers());
        this.bookId = identifier == null ? null : identifier.toString();
        this.createDate = DataUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.CREATION);
        this.pubDate = DataUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.PUBLICATION);
        this.modDate = DataUtils.getFirstBookDate(book, nl.siegmann.epublib.domain.Date.Event.MODIFICATION);
        this.numChaps = book.getTableOfContents().getAllUniqueResources().size();
        if (book.getCoverImage() != null) {
            try {
                // Get the cover image and store it.
                DataUtils.saveStreamAsCoverImage(book.getCoverImage().getInputStream(), relPath);
                hasCoverImage = true;
            } catch (IOException e) {
                e.printStackTrace();
                hasCoverImage = false;
            }
        } else hasCoverImage = false;

        // Fill in other data.
        this.lastImportDate = Calendar.getInstance().getTime();
        this.lastModifiedDate = lastImportDate;
        this.lastReadDate = null;
        this.isInRecents = false;
        this.rating = 0;
        this.isNew = true;
        this.isUpdated = false;
        this.uniqueId = UniqueIdFactory.getInstance().nextId(RBook.class);
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

        // Always update the last import date.
        this.lastImportDate = otherBook.lastImportDate;
        // Check if the hashes are different, and if they aren't then we're done. If they are, copy the new one.
        if (Arrays.equals(this.hash, otherBook.hash)) return;
        this.hash = otherBook.hash;
        this.title = otherBook.title;
        this.author = otherBook.author;
        this.desc = otherBook.desc;
        this.subjects = otherBook.subjects;
        this.types = otherBook.types;
        this.format = otherBook.format;
        this.language = otherBook.language;
        this.publisher = otherBook.publisher;
        this.bookId = otherBook.bookId;
        this.createDate = otherBook.createDate;
        this.pubDate = otherBook.pubDate;
        this.modDate = otherBook.modDate;
        this.numChaps = otherBook.numChaps;
        // Delete the cover image if the other book doesn't have one.
        if (this.hasCoverImage && !otherBook.hasCoverImage) DataUtils.deleteCoverImage(this.relPath);
        // We would have already gotten/replaced the cover image file if the other book has one, so just set the flag.
        this.hasCoverImage = otherBook.hasCoverImage;
        // As long as this book isn't still marked as new, mark it as updated.
        if (!this.isNew) this.isUpdated = true;
        this.lastModifiedDate = otherBook.lastModifiedDate;
    }

    @Override
    public Object getUID() {
        return uniqueId;
    }

    // TODO Workaround for an issue where the realm transformer doesn't work on androidTest sources :(
    void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    // TODO Workaround for an issue where the realm transformer doesn't work on androidTest sources :(
    boolean isNew() {
        return this.isNew;
    }

    // TODO Workaround for an issue where the realm transformer doesn't work on androidTest sources :(
    boolean isUpdated() {
        return this.isUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBook)) return false;
        RBook rBook = (RBook) o;
        return uniqueId == rBook.uniqueId && relPath.equals(rBook.relPath);
    }

    @Override
    public int hashCode() {
        int result = relPath.hashCode();
        result = 31 * result + (int) (uniqueId ^ (uniqueId >>> 32));
        return result;
    }
}
