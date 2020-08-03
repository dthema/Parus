package com.example.parus.ui.home.reminder;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parus.R;
import com.example.parus.viewmodels.ReminderModel;
import com.example.parus.viewmodels.data.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    private List<Reminder> reminders;
    private boolean delete;
    private List<Reminder> deletingReminders;
    private AppCompatActivity mContext;
    private final ClickInterface listener;
    private ReminderModel reminderModel;

    ReminderAdapter(List<Reminder> reminders, AppCompatActivity mContext, ClickInterface listener) {
        this.reminders = reminders;
        this.listener = listener;
        this.delete = false;
        this.mContext = mContext;
        deletingReminders = new ArrayList<>();
        reminderModel = new ViewModelProvider(mContext, ViewModelProvider.AndroidViewModelFactory.getInstance(mContext.getApplication())).get(ReminderModel.class);
    }

    ReminderAdapter(List<Reminder> reminders, boolean delete, AppCompatActivity mContext, ClickInterface listener) {
        this.reminders = reminders;
        this.delete = delete;
        this.mContext = mContext;
        this.listener = listener;
        deletingReminders = new ArrayList<>();
        reminderModel = new ViewModelProvider(mContext, ViewModelProvider.AndroidViewModelFactory.getInstance(mContext.getApplication())).get(ReminderModel.class);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reminder_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.checkBox.setVisibility(View.GONE);
        holder.checkBox.setClickable(false);
        holder.name.setText(reminder.getName());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                deletingReminders.add(reminder);
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.background.setBackground(ContextCompat.getDrawable(mContext, R.drawable.reminder_background));
            } else {
                deletingReminders.remove(reminder);
                holder.checkBox.setVisibility(View.GONE);
                holder.background.setBackground(ContextCompat.getDrawable(mContext, R.drawable.btn_link_location));
            }
        });
        holder.background.setOnClickListener(l -> {
            if (!delete) {
                DialogChangeReminder dialogChangeReminder;
                if (reminder.getType() == 0) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(reminder.getTimeStart());
                    StringBuilder start = new StringBuilder();
                    start.append(convertDate(c.get(Calendar.HOUR_OF_DAY))).append(":").append(convertDate(c.get(Calendar.MINUTE)));
                    c.setTime(reminder.getTimeEnd());
                    StringBuilder end = new StringBuilder();
                    end.append(convertDate(c.get(Calendar.HOUR_OF_DAY))).append(":").append(convertDate(c.get(Calendar.MINUTE)));
                    c.setTime(reminder.getTimeInterval());
                    StringBuilder interval = new StringBuilder();
                    interval.append(convertDate(c.get(Calendar.HOUR_OF_DAY))).append(":").append(convertDate(c.get(Calendar.MINUTE)));
                    new DialogChangeReminder();
                    dialogChangeReminder = DialogChangeReminder.newInstance(reminder.getId(), reminder.getName(), start.toString(), end.toString(), interval.toString());
                    dialogChangeReminder.show(mContext.getSupportFragmentManager(), "dialogChangeReminder");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Date date : reminder.getTimers()) {
                        Calendar d = Calendar.getInstance();
                        d.setTime(date);
                        stringBuilder.append(convertDate(d.get(Calendar.HOUR_OF_DAY))).append(":").append(convertDate(d.get(Calendar.MINUTE))).append(" ");
                    }
                    new DialogChangeReminder();
                    dialogChangeReminder = DialogChangeReminder.newInstance(reminder.getId(), reminder.getName(), stringBuilder.toString().split(" "));
                    dialogChangeReminder.show(mContext.getSupportFragmentManager(), "dialogChangeReminder");
                }
            } else {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
                if (holder.checkBox.isChecked()) {
                    listener.recyclerviewOnClick(1);
                    holder.background.setContentDescription("Напоминание " + holder.name.getText().toString() + " выбрано для удаления");
                    Log.d("ABCD", "+");
                } else {
                    listener.recyclerviewOnClick(-1);
                    holder.background.setContentDescription("Напоминание " + holder.name.getText().toString() + " не выбрано для удаления");
                    Log.d("ABCD", "-");
                }
            }
        });
        if (reminder.getType() == 1) {
            for (Date d : reminder.getTimers()) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                String t = convertDate(c.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(c.get(Calendar.MINUTE));
                holder.time.setText(holder.time.getText() + t + ", ");
            }
            holder.time.setText(holder.time.getText().toString().substring(0, holder.time.getText().toString().length() - 2));
        } else if (reminder.getType() == 0) {
            Calendar s = Calendar.getInstance();
            s.setTime(reminder.getTimeStart());
            Calendar e = Calendar.getInstance();
            e.setTime(reminder.getTimeEnd());
            Calendar i = Calendar.getInstance();
            i.setTime(reminder.getTimeInterval());
            holder.time.setText("С " + convertDate(s.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(s.get(Calendar.MINUTE)) +
                    " по " + convertDate(e.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(e.get(Calendar.MINUTE)) + "\n");
            if (i.get(Calendar.HOUR_OF_DAY) == 0)
                holder.time.setText(holder.time.getText().toString() + "Каждые " + i.get(Calendar.MINUTE) + " минут(-ы)");
            else
                holder.time.setText(holder.time.getText().toString() + "Каждые " + convertDate(i.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(i.get(Calendar.MINUTE)) + " час(-ов)");
        }
        if (delete) {
            holder.background.setContentDescription("Напоминание " + holder.name.getText().toString() + " не выбрано для удаления");
        } else {
            holder.background.setContentDescription("Напоминание " + holder.name.getText().toString() + ", " + holder.time.getText().toString());
        }
    }

    void delete() {
        if (deletingReminders.size() > 0)
            reminderModel.deleteReminders(deletingReminders);
        else
            Toast.makeText(mContext, "Напоминания не выбраны", Toast.LENGTH_LONG).show();
    }


    @Override
    public int getItemCount() {
        if (reminders == null)
            return 0;
        else
            return reminders.size();
    }

    private String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView background;
        TextView name;
        TextView time;
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            background = itemView.findViewById(R.id.reminderBackgroung);
            name = itemView.findViewById(R.id.reminderName);
            time = itemView.findViewById(R.id.reminderTime);
            checkBox = itemView.findViewById(R.id.reminderCheck);
        }
    }
}
