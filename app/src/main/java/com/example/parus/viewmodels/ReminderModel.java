package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.Reminder;
import com.example.parus.viewmodels.repositories.ReminderRepository;

import java.util.HashMap;
import java.util.List;

public class ReminderModel extends AndroidViewModel {

    public ReminderModel(@NonNull Application application) {
        super(application);
    }

    ReminderRepository repository = new ReminderRepository();

    private LiveData<List<Reminder>> remindersList;

    public LiveData<List<Reminder>> getProductList(String userId, String linkUserId, boolean isSupport) {
        remindersList = repository.productListening(userId, linkUserId, isSupport);
        return remindersList;
    }

    public LiveData<List<Reminder>> getProductList() {
        return repository.productListening();
    }

    public LiveData<List<Reminder>> getRemindersList() {
        return remindersList;
    }

    public void deleteReminders(List<Reminder> deletingReminders) {
        repository.deleteReminders(deletingReminders);
    }

    public void addReminder(HashMap<String, Object> hashMap) {
        repository.addReminder(hashMap);
    }

    public void changeReminder(String docId, HashMap<String, Object> hashMap) {
        repository.changeReminder(docId, hashMap);
    }

    public void removeObserver(LifecycleOwner owner) {
        if (remindersList != null)
            if (remindersList.hasObservers())
                remindersList.removeObservers(owner);

    }
}
