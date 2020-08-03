package com.example.parus.ui.chat;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<Chat> chats;
    private String linkUserName;

    MessageAdapter(Context context, List<Chat> chats, String linkUserName) {
        this.context = context;
        this.chats = chats;
        this.linkUserName = linkUserName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_item_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chats.get(position);
        holder.message.setText(chat.getMessage());
        Calendar time = Calendar.getInstance();
        time.setTime(chat.getDate());
        if (holder.getItemViewType() == MSG_TYPE_LEFT) {
            holder.params.setText(convertDate(time.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(time.get(Calendar.MINUTE)) + "," + " " + linkUserName + ":");
        } else {
            holder.params.setText(convertDate(time.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(time.get(Calendar.MINUTE)) + "," + " Вы:");
        }
    }

    private String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

    @Override
    public int getItemCount() {
        if (chats == null)
            return 0;
        else
            return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView message;
        TextView params;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.chatText);
            params = itemView.findViewById(R.id.chatItemParams);
            message.setOnLongClickListener(l -> {
                ClipboardManager clipboard = (ClipboardManager) itemView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Сообщение", message.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(itemView.getContext(), "Сообщение скопировано", Toast.LENGTH_LONG).show();
                return true;
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        if (chats.get(position).getSender().equals(user.getUid()))
            return MSG_TYPE_RIGHT;
        else
            return MSG_TYPE_LEFT;

    }
}
