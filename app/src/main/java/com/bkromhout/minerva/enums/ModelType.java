package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.realm.RTag;
import io.realm.RealmModel;
import io.realm.RealmObject;

/**
 * Enum representation of realm classes which we can put into adapters.
 */
public enum ModelType {
    BOOK(RBook.class),
    BOOK_LIST(RBookList.class),
    BOOK_LIST_ITEM(RBookListItem.class),
    TAG(RTag.class);

    private final String associatedClass;

    ModelType(Class<? extends RealmObject> associatedClass) {
        this.associatedClass = associatedClass.getCanonicalName();
    }

    public static ModelType fromRealmClass(Class<? extends RealmModel> clazz) {
        String clazzName = clazz.getCanonicalName();
        for (ModelType modelType : ModelType.values())
            if (modelType.associatedClass.equals(clazzName)) return modelType;
        throw new IllegalArgumentException("Invalid clazz.");
    }
}
