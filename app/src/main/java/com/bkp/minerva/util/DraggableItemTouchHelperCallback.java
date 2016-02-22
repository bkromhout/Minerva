package com.bkp.minerva.util;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Implementation of {@link ItemTouchHelper.Callback} to support drag and drop.
 */
public class DraggableItemTouchHelperCallback extends ItemTouchHelper.Callback {
    /**
     * The implementer of our interface, which will be notified of events.
     */
    private final Adapter adapter;

    public DraggableItemTouchHelperCallback(Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // We support dragging vertically.
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // Does nothing, we don't support swiping.
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) return false;

        // Notify the adapter of the move.
        adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    /**
     * Implementers will be notified of certain events.
     */
    public interface Adapter {
        /**
         * Called whenever an item has moved whilst being dragged.
         * <p>
         * Note that this occurs while dragging, NOT when the "drop" happens.
         * @param sourcePos The position of the source.
         * @param targetPos The target position.
         */
        void onItemMove(int sourcePos, int targetPos);
    }
}
