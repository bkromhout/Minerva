package com.bkromhout.minerva.ui;

import android.widget.Checkable;

/**
 * Extended Checkable state which allows for a third state.
 */
public interface TriCheckable extends Checkable {
    void setState(Boolean state);

    Boolean getState();
}
