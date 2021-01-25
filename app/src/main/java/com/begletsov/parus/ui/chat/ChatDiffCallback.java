package com.begletsov.parus.ui.chat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.begletsov.parus.viewmodels.data.models.Chat;

class ChatDiffCallback extends DiffUtil.ItemCallback<Chat> {

    @Override
    public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
        return oldItem.equals(newItem);
    }
}
