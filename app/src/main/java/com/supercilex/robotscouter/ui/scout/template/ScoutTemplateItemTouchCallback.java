package com.supercilex.robotscouter.ui.scout.template;

import android.annotation.SuppressLint;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.template.ScoutTemplateViewHolder;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

import java.util.List;

public class ScoutTemplateItemTouchCallback<T, VH extends RecyclerView.ViewHolder> extends ItemTouchHelper.SimpleCallback {
    private View mRootView;
    private FirebaseRecyclerAdapter<T, VH> mAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private int mScrollToPosition;
    private boolean mIsItemMoving;

    public ScoutTemplateItemTouchCallback(View rootView) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
        mRootView = rootView;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        mItemTouchHelper = itemTouchHelper;
    }

    public void setAdapter(FirebaseRecyclerAdapter<T, VH> adapter) {
        mAdapter = adapter;
    }

    public void updateDragStatus(final RecyclerView.ViewHolder viewHolder, int position) {
        viewHolder.itemView.findViewById(R.id.reorder)
                .setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                            viewHolder.itemView.clearFocus(); // Saves data
                            mItemTouchHelper.startDrag(viewHolder);
                        }
                        return false;
                    }
                });

        if (position == mScrollToPosition) {
            ((ScoutTemplateViewHolder) viewHolder).requestFocus();
            mScrollToPosition = -1;
        }
    }

    public void addItemToScrollQueue(int position) {
        mScrollToPosition = position;
    }

    public boolean onChildChanged(ChangeEventListener.EventType type, int index) {
        if (mIsItemMoving) {
            return false;
        } else if (type == ChangeEventListener.EventType.ADDED && index == mScrollToPosition) {
            ((RecyclerView) mRootView.findViewById(R.id.list)).scrollToPosition(mScrollToPosition);
        }
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        mIsItemMoving = true;
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        mAdapter.notifyItemMoved(fromPos, toPos);

        List<DatabaseReference> refs = FirebaseAdapterHelper.getRefs(mAdapter);
        refs.add(toPos, refs.remove(fromPos));
        for (int i = 0; i < refs.size(); i++) {
            refs.get(i).getRef().setPriority(i);
        }

        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
        final T deletedObject = mAdapter.getItem(position);
        final DatabaseReference deletedRef = mAdapter.getRef(position);

        viewHolder.itemView.clearFocus(); // Needed to prevent the item from being re-added
        deletedRef.removeValue();

        Snackbar.make(mRootView, R.string.deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deletedRef.setValue(deletedObject, position);
                    }
                })
                .show();
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mIsItemMoving = false;
    }
}
