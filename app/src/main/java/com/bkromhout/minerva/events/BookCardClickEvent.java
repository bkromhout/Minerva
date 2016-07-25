package com.bkromhout.minerva.events;

/**
 * Fired when some sort of click event happens for a book card.
 */
public class BookCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        NORMAL, LONG, QUICK_TAG
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
    private final int adapterPosition;
    /**
     * The layout position of the clicked view.
     */
    private final int layoutPosition;

    /**
     * Create a new {@link BookCardClickEvent}.
     * @param type            Type of click.
     * @param relPath         The value of {@link com.bkromhout.minerva.realm.RBook#relPath relPath} for the view's
     *                        corresponding {@link com.bkromhout.minerva.realm.RBook}
     * @param adapterPosition The adapter position of the clicked view.
     * @param layoutPosition  The layout position of the clicked view.
     */
    public BookCardClickEvent(Type type, String relPath, int adapterPosition, int layoutPosition) {
        this.type = type;
        this.relPath = relPath;
        this.adapterPosition = adapterPosition;
        this.layoutPosition = layoutPosition;
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
     * Get the adapter position of the clicked item.
     * @return Item adapter position.
     */
    public int getAdapterPosition() {
        return adapterPosition;
    }

    /**
     * Get the layout position of the clicked item.
     * @return Item layout position.
     */
    public int getLayoutPosition() {
        return layoutPosition;
    }
}
