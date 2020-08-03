package com.example.parus.ui.home.reminder;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.R;
import com.example.parus.viewmodels.ReminderModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class DialogChangeReminder extends AppCompatDialogFragment {

    private EditText time;
    private EditText name;
    private EditText timeStart;
    private EditText timeEnd;
    private EditText timeInterval;
    private LinearLayout scroll;
    private List<TextView> timers;
    private ReminderModel reminderModel;

    static DialogChangeReminder newInstance(String id, String name, String start, String end, String interval) {
        DialogChangeReminder fragment = new DialogChangeReminder();
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putString("name", name);
        bundle.putInt("type", 0);
        bundle.putString("start", start);
        bundle.putString("end", end);
        bundle.putString("interval", interval);
        fragment.setArguments(bundle);
        return fragment;
    }

    static DialogChangeReminder newInstance(String id, String name, String[] timers) {
        DialogChangeReminder fragment = new DialogChangeReminder();
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putString("name", name);
        bundle.putInt("type", 1);
        bundle.putStringArray("timers", timers);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);
        reminderModel = new ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(ReminderModel.class);
        LinearLayout type1 = dialogView.findViewById(R.id.dialogReminderType1);
        LinearLayout type2 = dialogView.findViewById(R.id.dialogReminderType2);
        time = dialogView.findViewById(R.id.reminderDialogTime);
        name = dialogView.findViewById(R.id.reminderDialogName);
        Button add = dialogView.findViewById(R.id.dialogAddReminder);
        Button delete = dialogView.findViewById(R.id.dialogDeleteReminder);
        timeStart = dialogView.findViewById(R.id.reminderDialogTimeStart);
        timeEnd = dialogView.findViewById(R.id.reminderDialogTimeEnd);
        timeInterval = dialogView.findViewById(R.id.reminderDialogTimeInterval);
        scroll = dialogView.findViewById(R.id.reminderDialogScroll);
        timers = new ArrayList<>();
        Spinner spinner = dialogView.findViewById(R.id.dialogReminderSpinner);
        spinner.setVisibility(View.GONE);
        name.setText(getArguments().getString("name"));
        if (getArguments().getInt("type") == 0) {
            type1.setVisibility(View.GONE);
            timeStart.setText(getArguments().getString("start"));
            timeEnd.setText(getArguments().getString("end"));
            timeInterval.setText(getArguments().getString("interval"));
        } else if (getArguments().getInt("type") == 1) {
            type2.setVisibility(View.GONE);
            if (Objects.requireNonNull(requireArguments().getStringArray("timers")).length > 0) {
                scroll.removeAllViews();
                for (String s : Objects.requireNonNull(requireArguments().getStringArray("timers"))) {
                    time.setText(s);
                    addTime();
                }
                time.setText("");
            }
        }
        addTimePickerDialogs();
        // добавление определённого времени
        add.setOnClickListener(l -> {
            boolean flag = true;
            if (time.getText().toString().length() == 5)
                if (textCount == 0) {
                    scroll.removeAllViews();
                    addTime();
                } else {
                    for (TextView t : timers) {
                        if (t.getText().toString().equals(time.getText().toString()))
                            flag = false;
                    }
                    if (flag)
                        addTime();
                }
        });
        // удаление определённого времени
        delete.setOnClickListener(l -> {
            if (!(textCount == 0))
                deleteTime();
        });
        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, id) -> {
            if (getArguments().getInt("type") == 0) {
                if (timeStart.getText().toString().equals(getArguments().getString("start")) &&
                        timeEnd.getText().toString().equals(getArguments().getString("end")) &&
                        timeInterval.getText().toString().equals(getArguments().getString("interval")) &&
                        name.getText().toString().equals(getArguments().getString("name"))) {
                    Toast.makeText(dialogView.getContext(), "Нет изменений", Toast.LENGTH_LONG).show();
                } else {
                    addIntervalTimer(dialogView, dialog);
                }
            } else if (getArguments().getInt("type") == 1) {
                String[] strings = new String[timers.size()];
                if (Objects.requireNonNull(requireArguments().getStringArray("timers")).length == strings.length && name.getText().toString().equals(getArguments().getString("name"))) {
                    boolean flag = false;
                    for (int i = 0; i < strings.length; i++) {
                        strings[i] = timers.get(i).getText().toString();
                        if (!strings[i].equals(Objects.requireNonNull(requireArguments().getStringArray("timers"))[i])) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        addTimers(dialogView, dialog);
                    } else {
                        Toast.makeText(dialogView.getContext(), "Нет изменений", Toast.LENGTH_LONG).show();
                    }
                } else {
                    addTimers(dialogView, dialog);
                }
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }

    private String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

    private int textCount = 0;

    private void addTimers(View dialogView, DialogInterface dialog) {
        if (!name.getText().toString().trim().equals("")) {
            if (timers.size() > 0) {
                HashMap<String, Object> hashMap = new HashMap<>();
                List<Date> dates = new ArrayList<>();
                for (int i = 0; i < timers.size(); i++) {
                    String t = timers.get(i).getText().toString();
                    Calendar c = Calendar.getInstance();
                    c.set(2010, 10, 10, Integer.parseInt(t.split(":")[0]), Integer.parseInt(t.split(":")[1]), 0);
                    dates.add(c.getTime());
                }
                if (timers.size() == dates.size()) {
                    Collections.sort(dates, Date::compareTo);
                    hashMap.put("timers", dates);
                    hashMap.put("name", name.getText().toString());
                    String docId = getArguments().getString("id");
                    reminderModel.changeReminder(docId, hashMap);
                } else
                    Toast.makeText(dialogView.getContext(), "Ошибка", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(dialogView.getContext(), "Время не выбрано", Toast.LENGTH_LONG).show();
            }
        } else
            Toast.makeText(dialogView.getContext(), "Поля не заполнены", Toast.LENGTH_LONG).show();
    }

    private void addIntervalTimer(View dialogView, DialogInterface dialog) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (timeInterval.length() == 5 && timeStart.length() == 5 && timeEnd.length() == 5 && !name.getText().toString().trim().equals("")) {
            Calendar s = Calendar.getInstance();
            s.set(2010, 10, 10, Integer.parseInt(timeStart.getText().toString().split(":")[0]), Integer.parseInt(timeStart.getText().toString().split(":")[1]), 0);
            Calendar e = Calendar.getInstance();
            e.set(2010, 10, 10, Integer.parseInt(timeEnd.getText().toString().split(":")[0]), Integer.parseInt(timeEnd.getText().toString().split(":")[1]), 0);
            Calendar i = Calendar.getInstance();
            i.set(2010, 10, 10, Integer.parseInt(timeInterval.getText().toString().split(":")[0]), Integer.parseInt(timeInterval.getText().toString().split(":")[1]), 0);
            if (s.getTime().getTime() >= e.getTime().getTime()) {
                Toast.makeText(dialogView.getContext(), "Начало напоминаний должно быть раньше конца", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                if ((i.get(Calendar.HOUR_OF_DAY) < 1 && i.get(Calendar.MINUTE) < 15) || (i.get(Calendar.HOUR_OF_DAY) >= 12 && i.get(Calendar.MINUTE) > 0)) {
                    Toast.makeText(dialogView.getContext(), "Интервал должен быть от 15 минут и до 12 часов", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else if ((e.get(Calendar.HOUR_OF_DAY) - s.get(Calendar.HOUR_OF_DAY) > i.get(Calendar.HOUR_OF_DAY)) ||
                        (e.get(Calendar.HOUR_OF_DAY) - s.get(Calendar.HOUR_OF_DAY) == i.get(Calendar.HOUR_OF_DAY) &&
                                e.get(Calendar.MINUTE) - s.get(Calendar.MINUTE) > i.get(Calendar.MINUTE))) {
                    hashMap.put("timeStart", s.getTime());
                    hashMap.put("timeEnd", e.getTime());
                    hashMap.put("timeInterval", i.getTime());
                    hashMap.put("name", name.getText().toString());
                    String docId = getArguments().getString("id");
                    reminderModel.changeReminder(docId, hashMap);
                } else {
                    Toast.makeText(dialogView.getContext(), "Интервал должен быть меньше времени действия напоминаний", Toast.LENGTH_LONG).show();
                }
            }
        } else
            Toast.makeText(dialogView.getContext(), "Поля не заполнены", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("ResourceType")
    private void addTime() {
        TextView txt = new TextView(getContext());
        txt.setId(10001231 + textCount);
        txt.setTextSize(20);
        txt.setTextColor(Color.BLACK);
        txt.setVisibility(View.VISIBLE);
        txt.setText(time.getText().toString());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 10;
        txt.setLayoutParams(params);
        scroll.addView(txt);
        timers.add(txt);
        textCount++;
    }

    @SuppressLint("ResourceType")
    private void deleteTime() {
        if (timers.size() > 0) {
            scroll.removeView(timers.get(textCount - 1));
            timers.remove(textCount - 1);
            textCount--;
            if (textCount == 0) {
                TextView txt = new TextView(getContext());
                txt.setId(10001230);
                txt.setTextSize(20);
                txt.setTextColor(Color.BLACK);
                txt.setVisibility(View.VISIBLE);
                txt.setText("Время не добавлено");
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.leftMargin = 10;
                txt.setLayoutParams(params);
                scroll.addView(txt);
            }
        }
    }

    private void addTimePickerDialogs() {
        time.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                            time.setText(convertDate(hourOfDay) + ":" + convertDate(minute));
                            time.clearFocus();
                        }, 0, 0, true);
                        timePickerDialog.setOnCancelListener(dialog -> time.clearFocus());
                        timePickerDialog.show();
                    }
                });
        timeStart.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                            timeStart.setText(convertDate(hourOfDay) + ":" + convertDate(minute));
                            timeStart.clearFocus();
                        }, 0, 0, true);
                        timePickerDialog.setOnCancelListener(dialog -> timeStart.clearFocus());
                        timePickerDialog.show();
                    }
                });
        timeEnd.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                            timeEnd.setText(convertDate(hourOfDay) + ":" + convertDate(minute));
                            timeEnd.clearFocus();
                        }, 0, 0, true);
                        timePickerDialog.setOnCancelListener(dialog -> timeEnd.clearFocus());
                        timePickerDialog.show();
                    }
                });
        timeInterval.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                            timeInterval.setText(convertDate(hourOfDay) + ":" + convertDate(minute));
                            timeInterval.clearFocus();
                        }, 0, 0, true);
                        timePickerDialog.setOnCancelListener(dialog -> timeInterval.clearFocus());
                        timePickerDialog.show();
                    }
                });
    }
}
