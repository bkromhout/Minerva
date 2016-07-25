package com.bkromhout.minerva.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.ViewDebug;
import com.bkromhout.minerva.R;

/**
 * Checkbox which supports a third, "partial", state.
 */
public class TriStateCheckBox extends AppCompatCheckBox implements TriCheckable {

    private static final int[] EXTRA_STATE_SET = {R.attr.state_partially_checked};

    private boolean isPartiallyChecked = false;

    public TriStateCheckBox(Context context) {
        this(context, null, 0);
    }

    public TriStateCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkboxStyle);
    }

    public TriStateCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // This actually doesn't achieve anything color-wise, but for some odd reason it does prevent issues
            // related to animations lagging...To be honest, I'm not about to question it.
            setButtonTintMode(PorterDuff.Mode.SRC_IN);
            setButtonTintList(ContextCompat.getColorStateList(context, R.color.tri_checkable_color));
        }
        setButtonDrawable(R.drawable.tri_state_checkmark);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TriStateCheckBox);
        setPartiallyChecked(a.getBoolean(R.styleable.TriStateCheckBox_partially_checked, false));
        a.recycle();
    }

    /**
     * Return whether this {@link TriStateCheckBox} is partially checked or not.
     * @return Whether partially checked or not.
     */
    @ViewDebug.ExportedProperty
    public final boolean isPartiallyChecked() {
        return isPartiallyChecked;
    }

    /**
     * Set this view as partially checked. Doesn't change the checked state.
     * @param isPartiallyChecked True to set partially checked, otherwise false.
     */
    public final void setPartiallyChecked(boolean isPartiallyChecked) {
        if (this.isPartiallyChecked != isPartiallyChecked) {
            this.isPartiallyChecked = isPartiallyChecked;
            refreshDrawableState();
        }
    }

    /**
     * Change the checked state of the view to the inverse of its current state.
     * <p>
     * If currently partially checked, will become unchecked.
     */
    @Override
    public void toggle() {
        if (isPartiallyChecked) setChecked(false);
        else super.toggle();
    }

    /**
     * Change the checked state of the view.
     * <p>
     * Sets state as not partially checked no matter what is passed for {@code checked}.
     * @param checked The new checked state.
     */
    @Override
    public void setChecked(boolean checked) {
        if (isPartiallyChecked) isPartiallyChecked = false;
        super.setChecked(checked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (getState() == null) mergeDrawableStates(drawableState, EXTRA_STATE_SET);
        return drawableState;
    }

    @ViewDebug.ExportedProperty
    @Override
    public Boolean getState() {
        return isPartiallyChecked ? null : isChecked();
    }

    @Override
    public void setState(Boolean state) {
        if (state != null) setChecked(state);
        else setPartiallyChecked(true);
    }

    static class SavedState extends BaseSavedState {
        boolean isPartiallyChecked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isPartiallyChecked = (boolean) in.readValue(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(isPartiallyChecked);
        }

        @Override
        public String toString() {
            return "TriStateCheckBox.SavedState{" + Integer.toHexString(System.identityHashCode(this))
                    + " isPartiallyChecked=" + isPartiallyChecked + "}";
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.isPartiallyChecked = getState();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setState(ss.isPartiallyChecked);
        requestLayout();
    }
}
