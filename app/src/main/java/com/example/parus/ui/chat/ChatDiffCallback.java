package com.example.parus.ui.chat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.example.parus.viewmodels.data.models.Chat;

public class ChatDiffCallback extends DiffUtil.ItemCallback<Chat> {

    @Override
    public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
        return oldItem.equals(newItem);
    }
}
