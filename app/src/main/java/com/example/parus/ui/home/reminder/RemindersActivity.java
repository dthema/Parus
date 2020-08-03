package com.example.parus.ui.home.reminder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.parus.R;
import com.example.parus.viewmodels.ReminderModel;
import com.example.parus.viewmodels.data.Reminder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RemindersActivity extends AppCompatActivity implements ClickInterface {

    private final static String TAG = "ReminderActivity";
    private static final int ADD = 1;
    private static final int CHANGE = 2;
    private static final int DELETE = 3;
    private List<Reminder> reminders;
    private RecyclerView recyclerView;
    private com.google.android.material.floatingactionbutton.FloatingActionButton addReminder;
    private ReminderAdapter reminderAdapter;
    private ClickInterface listener;
    private int chosenItems;

    @Override
    public void recyclerviewOnClick(int choose) {
        chosenItems += choose;
        Objects.requireNonNull(getSupportActionBar()).setTitle("Выбрано: " + chosenItems);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.delete_menu, menu);
        menu.getItem(1).setVisible(false);
        menu.getItem(2).setVisible(false);
        menu.getItem(0).setOnMenuItemClickListener(item -> {
            if (reminders.size() > 0) {
                reminderAdapter = new ReminderAdapter(reminders, true, RemindersActivity.this, listener);
                recyclerView.setAdapter(reminderAdapter);
                chosenItems = 0;
                setActionBar(true);
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(true);
                menu.getItem(2).setVisible(true);
                addReminder.setVisibility(View.GONE);
            } else {
                Toast.makeText(RemindersActivity.this, "Напоминаний нет", Toast.LENGTH_LONG).show();
            }
            return false;
        });
        menu.getItem(1).setOnMenuItemClickListener(item -> {
            reminderAdapter = new ReminderAdapter(reminders, false, RemindersActivity.this, listener);
            recyclerView.setAdapter(reminderAdapter);
            setActionBar(false);
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            addReminder.setVisibility(View.VISIBLE);
            return false;
        });
        menu.getItem(2).setOnMenuItemClickListener(item -> {
            reminderAdapter.delete();
            setActionBar(false);
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            addReminder.setVisibility(View.VISIBLE);
            return false;
        });
        if (Build.VERSION.SDK_INT >= 26)
            menu.getItem(0).setContentDescription("Удаление напоминаний");
        else
            MenuItemCompat.setContentDescription(menu.getItem(0), "Удаление напоминаний");
        return super.onCreateOptionsMenu(menu);
    }

    private void setActionBar(boolean flag){
        if (flag) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setDisplayShowHomeEnabled(false);
                getSupportActionBar().setTitle("Выбрано: 0");
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Напоминания");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        listener = this;
        setActionBar(false);
        reminders = new ArrayList<>();
        recyclerView = findViewById(R.id.reminderView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        addReminder = findViewById(R.id.addReminder);
        addReminder.setOnClickListener(c -> {
            new DialogAddReminder();
            DialogAddReminder dialogAddReminder = new DialogAddReminder();
            dialogAddReminder.show(getSupportFragmentManager(), "ReminderDialog");
        });
        ReminderModel reminderModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(ReminderModel.class);
        reminderModel.getProductList().observe(this, reminders -> {
            RemindersActivity.this.reminders = reminders;
            reminderAdapter = new ReminderAdapter(reminders, RemindersActivity.this, listener);
            recyclerView.setAdapter(reminderAdapter);
        });
    }
}

