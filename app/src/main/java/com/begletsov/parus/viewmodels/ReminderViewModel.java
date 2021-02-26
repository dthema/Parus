package com.begletsov.parus.viewmodels;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.data.models.Reminder;
import com.begletsov.parus.viewmodels.repositories.ReminderRepository;

import java.util.HashMap;
import java.util.List;

public class ReminderViewModel extends ViewModel {

    public ReminderViewModel() {
        super();
    }

    private final ReminderRepository repository = ReminderRepository.getInstance();

    private LiveData<List<Reminder>> remindersList;

    public LiveData<List<Reminder>> getReminderData(String userId, String linkUserId, boolean isSupport) {
        remindersList = repository.reminderListening(userId, linkUserId, isSupport, true);
        return remindersList;
    }

    public LiveData<List<Reminder>> getReminderData(boolean recreateData) {
        remindersList = repository.reminderListening(recreateData);
        return remindersList;

    }

    public LiveData<Integer> deleteReminders(List<Reminder> deletingReminders) {
        return repository.deleteReminders(deletingReminders);
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
        repository.stopListening();
    }

    public LiveData<String> startCheckReminders() {
        return repository.reminderListener();
    }

    public void stopCheckReminders() {
        repository.stopCheckReminders();
    }

    public void setReminders(List<Reminder> reminders) {
        repository.setReminders(reminders);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (repository.reminderListening(false) != null)
            if (!repository.reminderListening(false).hasObservers()) {
                repository.stopListening();
                repository.stopCheckReminders();
                repository.destroy();
            }
    }
}
