package com.example.parus.ui.home.reminder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.view.MenuItemCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.parus.R;
import com.example.parus.databinding.ActivityRemindersBinding;
import com.example.parus.viewmodels.ReminderViewModel;
import com.example.parus.viewmodels.UserViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class RemindersActivity extends AppCompatActivity implements OnItemClickInterface {

    private final static String TAG = "ReminderActivity";
    private ReminderAdapter reminderAdapter;
    private int chosenItems;
    private ActivityRemindersBinding binding;


    @Override
    public void onItemClick(int choose) {
        chosenItems += choose;
        Objects.requireNonNull(getSupportActionBar()).setTitle("Выбрано: " + chosenItems);
    }

    @Override
    public void onDialogClick(AppCompatDialogFragment dialogFragment) {
        dialogFragment.show(getSupportFragmentManager(), "dialogReminder");
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
            if (reminderAdapter.getCurrentList().size() > 0) {
                reminderAdapter.setDeleting();
                binding.reminderView.setAdapter(reminderAdapter);
                chosenItems = 0;
                setActionBar(true);
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(true);
                menu.getItem(2).setVisible(true);
                binding.addReminder.setVisibility(View.GONE);
            } else {
                Toast.makeText(RemindersActivity.this, "Напоминаний нет", Toast.LENGTH_LONG).show();
            }
            return false;
        });
        menu.getItem(1).setOnMenuItemClickListener(item -> {
            reminderAdapter.setNoDeleting();
            binding.reminderView.setAdapter(reminderAdapter);
            setActionBar(false);
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            binding.addReminder.setVisibility(View.VISIBLE);
            return false;
        });
        menu.getItem(2).setOnMenuItemClickListener(item -> {
            reminderAdapter.delete().observe(this, result -> {
                switch (result) {
                    case 0:
                        Toast.makeText(RemindersActivity.this, "Напоминания не выбраны", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        break;
                    default:
                        Toast.makeText(RemindersActivity.this, "Произошла ошибка", Toast.LENGTH_LONG).show();
                        break;
                }
            });
            reminderAdapter.setNoDeleting();
            binding.reminderView.setAdapter(reminderAdapter);
            setActionBar(false);
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            binding.addReminder.setVisibility(View.VISIBLE);
            return false;
        });
        if (Build.VERSION.SDK_INT >= 26)
            menu.getItem(0).setContentDescription("Удаление напоминаний");
        else
            MenuItemCompat.setContentDescription(menu.getItem(0), "Удаление напоминаний");
        return super.onCreateOptionsMenu(menu);
    }

    private void setActionBar(boolean flag) {
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminders);
        setActionBar(false);
        binding.reminderView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.reminderView.setLayoutManager(linearLayoutManager);
        ReminderViewModel reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        reminderAdapter = new ReminderAdapter(new ReminderDiffCallback(), this, reminderViewModel);
        binding.reminderView.setAdapter(reminderAdapter);
        reminderViewModel.getReminderData(false).observe(this, reminders -> reminderAdapter.submitList(new ArrayList<>(reminders)));
        binding.addReminder.setOnClickListener(c -> {
            new DialogAddReminder();
            DialogAddReminder dialogAddReminder = new DialogAddReminder();
            dialogAddReminder.show(getSupportFragmentManager(), "ReminderDialog");
        });
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getShortUserData().observe(this, pair -> {
            if (pair.first == null)
                return;
            String userId = pair.first.first;
            String linkUserId = pair.first.second;
            Boolean isSupport = pair.second;
            if (isSupport && userId.equals(linkUserId))
                finish();
        });
    }
}

