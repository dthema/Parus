package com.begletsov.parus.ui.chat;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.begletsov.parus.databinding.ChatItemCenterBinding;
import com.begletsov.parus.databinding.ChatItemLeftBinding;
import com.begletsov.parus.databinding.ChatItemRightBinding;
import com.begletsov.parus.viewmodels.data.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class MessageAdapter extends ListAdapter<Chat, MessageAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private static final int MSG_TYPE_CENTER = 2;

    MessageAdapter(@NonNull ChatDiffCallback diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ViewHolder.from(parent, viewType);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getCurrentList().get(position));
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        if (!getCurrentList().get(position).isCalendar()) {
            if (getCurrentList().get(position).getSender().equals(user.getUid()))
                return MSG_TYPE_RIGHT;
            else
                return MSG_TYPE_LEFT;
        } else
            return MSG_TYPE_CENTER;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView message;
        private TextView params;

        private ViewHolder(@NonNull ChatItemLeftBinding binding) {
            super(binding.getRoot());
            message = binding.chatText;
            params = binding.chatItemParams;
            message.setOnLongClickListener(l -> {
                ClipboardManager clipboard = (ClipboardManager) binding.getRoot().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Сообщение", message.getText().toString());
                assert clipboard != null;
                clipboard.setPrimaryClip(clip);
                Toast.makeText(binding.getRoot().getContext(), "Сообщение скопировано", Toast.LENGTH_LONG).show();
                return true;
            });
        }

        private ViewHolder(@NonNull ChatItemRightBinding binding) {
            super(binding.getRoot());
            message = binding.chatText;
            params = binding.chatItemParams;
            message.setOnLongClickListener(l -> {
                ClipboardManager clipboard = (ClipboardManager) binding.getRoot().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Сообщение", message.getText().toString());
                assert clipboard != null;
                clipboard.setPrimaryClip(clip);
                Toast.makeText(binding.getRoot().getContext(), "Сообщение скопировано", Toast.LENGTH_LONG).show();
                return true;
            });
        }

        private ViewHolder(@NonNull ChatItemCenterBinding binding) {
            super(binding.getRoot());
            message = binding.chatText;
        }

        private static ViewHolder from(@NonNull ViewGroup parent, int viewType) {
            if (viewType == MSG_TYPE_RIGHT) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                ChatItemRightBinding binding = ChatItemRightBinding.inflate(inflater, parent, false);
                return new ViewHolder(binding);
            } else if (viewType == MSG_TYPE_LEFT) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                ChatItemLeftBinding binding = ChatItemLeftBinding.inflate(inflater, parent, false);
                return new ViewHolder(binding);
            } else {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                ChatItemCenterBinding binding = ChatItemCenterBinding.inflate(inflater, parent, false);
                return new ViewHolder(binding);
            }
        }

        @SuppressLint("SetTextI18n")
        private void bind(Chat chat) {
            if (!chat.isCalendar()) {
                message.setText(chat.getMessage());
                Calendar time = Calendar.getInstance();
                time.setTime(chat.getDate());
                if (getItemViewType() == MSG_TYPE_LEFT) {
                    params.setText(convertDate(time.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(time.get(Calendar.MINUTE)));
                } else {
                    params.setText(convertDate(time.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(time.get(Calendar.MINUTE)));
                }
            } else {
                Calendar current = Calendar.getInstance();
                Calendar time = Calendar.getInstance();
                time.setTime(chat.getDate());
                if (current.get(Calendar.YEAR) != time.get(Calendar.YEAR)) {
                    message.setText(convertDate(time.get(Calendar.DATE)) + " " + getMonth(time) + " " + time.get(Calendar.YEAR) + " г.");
                } else if (current.get(Calendar.MONTH) != time.get(Calendar.MONTH)) {
                    message.setText(convertDate(time.get(Calendar.DATE)) + " " + getMonth(time));
                } else if (current.get(Calendar.DATE) - time.get(Calendar.DATE) > 2) {
                    message.setText(convertDate(time.get(Calendar.DATE)) + " " + getMonth(time));
                } else {
                    switch (current.get(Calendar.DATE) - time.get(Calendar.DATE)) {
                        case 0:
                            message.setText("Сегодня");
                            break;
                        case 1:
                            message.setText("Вчера");
                            break;
                        case 2:
                            message.setText("Позавчера");
                            break;
                    }
                }
            }
        }

        private String convertDate(int input) {
            if (input >= 10) {
                return String.valueOf(input);
            } else {
                return "0" + input;
            }
        }

        private String getMonth(Calendar time) {
            String month;
            switch (time.get(Calendar.MONTH)+1) {
                case 1:
                    month = "Января";
                    break;
                case 2:
                    month = "Февраля";
                    break;
                case 3:
                    month = "Марта";
                    break;
                case 4:
                    month = "Апреля";
                    break;
                case 5:
                    month = "Мая";
                    break;
                case 6:
                    month = "Июня";
                    break;
                case 7:
                    month = "Июля";
                    break;
                case 8:
                    month = "Августа";
                    break;
                case 9:
                    month = "Сентября";
                    break;
                case 10:
                    month = "Октября";
                    break;
                case 11:
                    month = "Ноября";
                    break;
                case 12:
                    month = "Декабря";
                    break;
                default:
                    month = "";
                    break;
            }
            return month;
        }
    }
}
