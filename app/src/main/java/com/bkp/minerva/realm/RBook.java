package com.bkp.minerva.realm;

import com.bkp.minerva.internal.SuperBook;
import com.bkp.minerva.util.BookUtils;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a book in Realm.
 */
public class RBook extends RealmObject {
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
     * Tags for book. Comma separated like so: {@code "tag1,tag2,tag3,tag4"}.
     */
    @Index
    private String tags;
    /**
     * Assigned rating.
     */
    @Index
    private int rating;

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
        this.tags = "";
        this.rating = 0;
        this.lastModifiedDate = Calendar.getInstance().getTime();
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

        // Fill in data from Book.
        Book book = superBook.getBook();
        this.title = book.getTitle();
        this.author = BookUtils.getFirstAuthor(book);
        this.desc = BookUtils.getFirstDesc(book);
        this.subjects = BookUtils.listToString(book.getMetadata().getSubjects());
        this.types = BookUtils.listToString(book.getMetadata().getTypes());
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
        this.tags = "";
        this.rating = 0;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}