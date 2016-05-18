package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.Prefs;

/**
 * Different types of marks an {@link com.bkromhout.minerva.realm.RBook} can have on it.
 */
public enum MarkType {
    NEW("isNew"),
    UPDATED("isUpdated");

    private final String fieldName;

    MarkType(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Get the name of the associated {@link com.bkromhout.minerva.realm.RBook} field.
     * @return Field name.
     */
    public final String getFieldName() {
        return fieldName;
    }

    /**
     * Get the name of the associated {@link com.bkromhout.minerva.realm.RTag}.
     * @return Tag name, or {@code null} if there isn't one.
     */
    public final String getTagName() {
        switch (fieldName) {
            case "isNew":
                return Prefs.get().getNewBookTag(null);
            case "isUpdated":
                return Prefs.get().getUpdatedBookTag(null);
            default:
                return null;
        }
    }
}
