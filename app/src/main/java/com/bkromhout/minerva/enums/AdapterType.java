package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import io.realm.RealmModel;
import io.realm.RealmObject;

/**
 * Enum representation of realm classes which we can put into adapters.
 */
public enum AdapterType {
    BOOK(RBook.class),
    BOOK_LIST(RBookList.class),
    BOOK_LIST_ITEM(RBookListItem.class);

    private final String associatedClass;

    AdapterType(Class<? extends RealmObject> associatedClass) {
        this.associatedClass = associatedClass.getCanonicalName();
    }

    public static AdapterType fromRealmClass(Class<? extends RealmModel> clazz) {
        String clazzName = clazz.getCanonicalName();
        for (AdapterType adapterType : AdapterType.values())
            if (adapterType.associatedClass.equals(clazzName)) return adapterType;
        throw new IllegalArgumentException("Invalid clazz");
    }
}
