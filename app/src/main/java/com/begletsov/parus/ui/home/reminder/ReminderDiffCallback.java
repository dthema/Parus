package com.begletsov.parus.ui.home.reminder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.begletsov.parus.viewmodels.data.models.Reminder;

class ReminderDiffCallback extends DiffUtil.ItemCallback<Reminder> {

    @Override
    public boolean areItemsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
        return oldItem.equals(newItem);
    }
}
