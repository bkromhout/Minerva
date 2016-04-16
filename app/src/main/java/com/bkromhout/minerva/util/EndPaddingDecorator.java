package com.bkromhout.minerva.util;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;

/**
 * Decorator which adds padding the the end of a view.
 */
public class EndPaddingDecorator extends RecyclerView.ItemDecoration {
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom += Minerva.getAppCtx().getResources().getDimensionPixelSize(R.dimen.rv_end_padding);
        }
    }
}
