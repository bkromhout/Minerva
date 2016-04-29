package com.bkromhout.minerva.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

/**
 * Checkbox which supports a third, "partial", state.
 */
public class TriStateCheckBox extends AppCompatCheckBox {
    public TriStateCheckBox(Context context) {
        super(context);
    }

    public TriStateCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TriStateCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
