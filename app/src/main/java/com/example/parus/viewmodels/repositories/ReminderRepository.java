package com.example.parus.viewmodels.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.Reminder;
import com.example.parus.viewmodels.data.ReminderData;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ReminderRepository {

    private static ReminderRepository repository;
    private final String userId;
    private int count = 0;

    private ReminderRepository() {
        this.userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    public synchronized static ReminderRepository getInstance() {
        if (repository == null) repository = new ReminderRepository();
        return repository;
    }

    private ReminderData reminderData;

    public LiveData<List<Reminder>> reminderListening(String userId, String linkUserId, boolean isSupport, boolean recreateData) {
        if (reminderData == null && recreateData)
            reminderData = new ReminderData(userId, linkUserId, isSupport);
        return reminderData;
    }

    public LiveData<List<Reminder>> reminderListening(boolean recreateData) {
        if (reminderData == null && recreateData)
            reminderData = new ReminderData(userId);
        return reminderData;
    }

    public void stopListening() {
        reminderData = null;
    }

    public void destroy() {
        repository = null;
    }

    public LiveData<Integer> deleteReminders(List<Reminder> deletingReminders) {
        SingleLiveEvent<Integer> liveEvent = new SingleLiveEvent<>();
        if (deletingReminders.size() == 0) {
            liveEvent.setValue(0);
            return liveEvent;
        }
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User user = s.toObject(User.class);
                    if (user == null) {
                        liveEvent.setValue(1);
                        return;
                    }
                    CollectionReference colRef = null;
                    if (user.isSupport()) {
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        for (Reminder reminder : deletingReminders)
                            colRef.document(reminder.getId()).delete();
                        liveEvent.setValue(2);
                    } else
                        liveEvent.setValue(3);
                });
        return liveEvent;
    }


    public void addReminder(HashMap<String, Object> hashMap) {
        SingleLiveEvent<Boolean> add = new SingleLiveEvent<>();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    CollectionReference colRef = null;
                    if (user.isSupport()) {
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        colRef.add(hashMap).addOnSuccessListener(t -> add.setValue(true));
                    }
                });
    }


    public void changeReminder(String docId, HashMap<String, Object> hashMap) {
        SingleLiveEvent<Boolean> change = new SingleLiveEvent<>();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    CollectionReference colRef = null;
                    if (user.isSupport()) {
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        colRef.document(docId).update(hashMap).addOnSuccessListener(t -> change.setValue(true));
                    }
                });
    }

    private boolean isThreadActive;
    private Thread checkRemindersThread;
    private List<Reminder> reminders;

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
    }

    private final MutableLiveData<String> checkReminders = new MutableLiveData<>();

    public LiveData<String> reminderListener() {
        if (isThreadActive)
            return null;
        isThreadActive = true;
        checkRemindersThread = new Thread(() -> {
            while (isThreadActive) {
                try {
                    Log.d("ReminderCheck", "Checked");
                    if (reminders != null) {
                        Reminder nextReminder = null;
                        Calendar currentTime = Calendar.getInstance();
                        currentTime.set(Calendar.SECOND, 0);
                        Calendar nextReminderTime = Calendar.getInstance();
                        nextReminderTime.set(Calendar.SECOND, 59);
                        nextReminderTime.set(Calendar.MINUTE, 23);
                        nextReminderTime.set(Calendar.HOUR_OF_DAY, 59);
                        nextReminderTime.set(Calendar.DATE, nextReminderTime.get(Calendar.DATE) + 1);
                        if (reminders.size() == 0)
                            checkReminders.postValue("Напоминаний нет");
                        else
                            for (Reminder reminder : reminders) {
                                if (reminder == null)
                                    continue;
                                Calendar end = Calendar.getInstance();
                                if (reminder.getType() == 0) { // напоминание с интервалом
                                    end.setTime(reminder.getTimeEnd());
                                    end.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                            currentTime.get(Calendar.DATE), end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE), 0);
                                    Calendar start = Calendar.getInstance();
                                    start.setTime(reminder.getTimeStart());
                                    start.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                            currentTime.get(Calendar.DATE), start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), 0);
                                    if (currentTime.getTimeInMillis() > end.getTimeInMillis()) {
                                        start.set(Calendar.DATE, start.get(Calendar.DATE)+1);
                                        if (nextReminderTime.getTimeInMillis() > start.getTimeInMillis() &&
                                                currentTime.getTimeInMillis() < start.getTimeInMillis()) {
                                            nextReminder = reminder;
                                            nextReminderTime = start;
                                            nextReminderTime.set(Calendar.SECOND, 0);
                                        }
                                    } else {
                                        Calendar previousReminder = Calendar.getInstance();
                                        previousReminder.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                currentTime.get(Calendar.DATE), start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), 0);
                                        Calendar interval = Calendar.getInstance();
                                        interval.setTime(reminder.getTimeInterval());
                                        int hour = interval.get(Calendar.HOUR_OF_DAY);
                                        int minute = interval.get(Calendar.MINUTE);
                                        while (previousReminder.getTimeInMillis() <= currentTime.getTimeInMillis()) {
                                            previousReminder.set(Calendar.HOUR_OF_DAY, previousReminder.get(Calendar.HOUR_OF_DAY) + hour);
                                            previousReminder.set(Calendar.MINUTE, previousReminder.get(Calendar.MINUTE) + minute);
                                        }
                                        if (nextReminderTime.getTimeInMillis() > previousReminder.getTimeInMillis()) {
                                            nextReminder = reminder;
                                            nextReminderTime = previousReminder;
                                            nextReminderTime.set(Calendar.SECOND, 0);
                                        }
                                    }
                                } else { // напоминание с выбранным временем
                                    for (int i = 0; i < reminder.getTimers().size(); i++) {
                                        Date date = reminder.getTimers().get(i);
                                        Calendar dateTime = Calendar.getInstance();
                                        dateTime.setTime(date);
                                        dateTime.setTimeZone(currentTime.getTimeZone());
                                        dateTime.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                currentTime.get(Calendar.DATE), dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), 0);
                                        if (nextReminderTime.getTimeInMillis() > dateTime.getTimeInMillis() &&
                                                currentTime.getTimeInMillis() <= dateTime.getTimeInMillis()) {
                                            nextReminder = reminder;
                                            nextReminderTime = dateTime;
                                        }
                                        if (i == reminder.getTimers().size()-1 &&
                                                currentTime.getTimeInMillis() > dateTime.getTimeInMillis()) {
                                            dateTime.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                    currentTime.get(Calendar.DATE) + 1, dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), 0);
                                            if (nextReminderTime.getTimeInMillis() > dateTime.getTimeInMillis()) {
                                                nextReminder = reminder;
                                                nextReminderTime = dateTime;
                                            }
                                        }
                                    }
                                }
                            }
                        if (nextReminder != null) {
                            checkReminders.postValue("Следующее напоминание:\n" + nextReminder.getName() +
                                    " в " + convertDate(nextReminderTime.get(Calendar.HOUR_OF_DAY)) + ":" +
                                    convertDate(nextReminderTime.get(Calendar.MINUTE)));
                        }
                        if (count < 5) {
                            Thread.sleep(500);
                            count++;
                        } else
                            Thread.sleep(20000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        checkRemindersThread.start();
        return checkReminders;
    }

    public void stopCheckReminders() {
        if (checkRemindersThread != null)
            if (!checkRemindersThread.isInterrupted())
                checkRemindersThread.interrupt();
        isThreadActive = false;
    }

    private static String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

}


