package com.bkromhout.minerva.events;

/**
 * Fired when some sort of click event happens for a book card.
 */
public class BookCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        NORMAL, LONG, INFO, QUICK_TAG
    }

    /**
     * Which click type this event is reporting.
     */
    private final Type type;
    /**
     * Value of {@link com.bkromhout.minerva.realm.RBook#relPath relPath} for the {@link
     * com.bkromhout.minerva.realm.RBook} that the clicked card is showing data for.
     */
    private final String relPath;
    /**
     * The position of the clicked view.
     */
    private final int position;

    /**
     * Create a new {@link BookCardClickEvent}.
     * @param type     Type of click.
     * @param relPath  The value of {@link com.bkromhout.minerva.realm.RBook#relPath relPath} for the view's
     *                 corresponding {@link com.bkromhout.minerva.realm.RBook}
     * @param position The position of the clicked view in the adapter.
     */
    public BookCardClickEvent(Type type, String relPath, int position) {
        this.type = type;
        this.relPath = relPath;
        this.position = position;
    }

    /**
     * Get the type of click this event represents.
     * @return Click type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the value of {@code relPath} so that the exact {@link com.bkromhout.minerva.realm.RBook} can be retrieved.
     * @return {@code relPath} value.
     */
    public String getRelPath() {
        return relPath;
    }

    /**
     * Get the position of the clicked item in the adapter.
     * @return Clicked item position.
     */
    public int getPosition() {
        return position;
    }
}
