package com.bkp.minerva.events;

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
     * Value of {@link com.bkp.minerva.realm.RBook#relPath relPath} for the {@link com.bkp.minerva.realm.RBook} that the
     * clicked card is showing data for.
     */
    private final String relPath;

    /**
     * Create a new {@link BookCardClickEvent}.
     * @param type    Type of click.
     * @param relPath The value of {@link com.bkp.minerva.realm.RBook#relPath relPath} for the view's corresponding
     *                {@link com.bkp.minerva.realm.RBook}
     */
    public BookCardClickEvent(Type type, String relPath) {
        this.type = type;
        this.relPath = relPath;
    }

    /**
     * Get the type of click this event represents.
     * @return Click type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the value of {@code relPath} so that the exact {@link com.bkp.minerva.realm.RBook} can be retrieved.
     * @return {@code relPath} value.
     */
    public String getRelPath() {
        return relPath;
    }
}
