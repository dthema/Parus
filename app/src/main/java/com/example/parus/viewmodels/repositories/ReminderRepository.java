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

public class ReminderRepository {

    private static ReminderRepository repository;
    private String userId;

    private ReminderRepository() {
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public synchronized static ReminderRepository getInstance() {
        if (repository == null) repository = new ReminderRepository();
        return repository;
    }

    ReminderData reminderData;

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

    public void stopListening(){
        reminderData = null;
    }

    public void destroy(){
        if (reminderData == null && checkReminders == null)
            repository = null;
    }

    public LiveData<Integer> deleteReminders(List<Reminder> deletingReminders) {
        SingleLiveEvent<Integer> liveEvent = new SingleLiveEvent<>();
        if (deletingReminders.size() == 0){
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


    public LiveData<Boolean> addReminder(HashMap<String, Object> hashMap) {
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
        return add;
    }


    public LiveData<Boolean> changeReminder(String docId, HashMap<String, Object> hashMap) {
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
        return change;
    }

    private boolean isThreadActive;
    private Thread checkRemindersThread;
    private List<Reminder> reminders;

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
    }

    private MutableLiveData<String> checkReminders = new MutableLiveData<>();

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
                                if (reminder.getType() == 0) {
                                    end.setTime(reminder.getTimeEnd());
                                    end.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                            currentTime.get(Calendar.DATE), end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE), 0);
                                    Calendar start = Calendar.getInstance();
                                    start.setTime(reminder.getTimeStart());
                                    start.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                            currentTime.get(Calendar.DATE) + 1, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), 0);
                                    if (currentTime.getTimeInMillis() > end.getTimeInMillis()) {
                                        if (nextReminderTime.getTimeInMillis() - 15 > start.getTimeInMillis() &&
                                                currentTime.getTimeInMillis() < start.getTimeInMillis()) {
                                            nextReminder = reminder;
                                            nextReminderTime = start;
                                            nextReminderTime.set(Calendar.SECOND, 0);
                                        }
                                    } else if (currentTime.getTimeInMillis() <= end.getTimeInMillis()) {
                                        Calendar previousReminder = Calendar.getInstance();
                                        previousReminder.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                currentTime.get(Calendar.DATE), start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), 0);
                                        Calendar interval = Calendar.getInstance();
                                        interval.setTime(reminder.getTimeInterval());
                                        int hour = interval.get(Calendar.HOUR_OF_DAY);
                                        int minute = interval.get(Calendar.MINUTE);
                                        while (previousReminder.getTimeInMillis() - 15 <= currentTime.getTimeInMillis()) {
                                            previousReminder.set(Calendar.HOUR_OF_DAY, previousReminder.get(Calendar.HOUR_OF_DAY) + hour);
                                            previousReminder.set(Calendar.MINUTE, previousReminder.get(Calendar.MINUTE) + minute);
                                        }
                                        if (nextReminderTime.getTimeInMillis() > previousReminder.getTimeInMillis()) {
                                            nextReminder = reminder;
                                            nextReminderTime = previousReminder;
                                            nextReminderTime.set(Calendar.SECOND, 0);
                                        }
                                    }
                                } else {
                                    for (int i = reminder.getTimers().size() - 1; i >= 0; i--) {
                                        Date date = reminder.getTimers().get(i);
                                        Calendar dateTime = Calendar.getInstance();
                                        dateTime.setTime(date);
                                        dateTime.setTimeZone(currentTime.getTimeZone());
                                        dateTime.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                currentTime.get(Calendar.DATE), dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE));
                                        if (currentTime.getTimeInMillis() >= dateTime.getTimeInMillis()) {
                                            if (i == reminder.getTimers().size() - 1) {
                                                dateTime.setTime(reminder.getTimers().get(0));
                                                dateTime.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),
                                                        currentTime.get(Calendar.DATE), dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE));
                                                if (nextReminderTime.getTimeInMillis() > dateTime.getTimeInMillis()) {
                                                    nextReminder = reminder;
                                                    nextReminderTime = dateTime;
                                                }
                                            }
                                            break;
                                        }
                                        nextReminder = reminder;
                                        nextReminderTime = dateTime;
                                        nextReminderTime.set(Calendar.SECOND, 0);
                                    }
                                }
                            }
                        if (nextReminder != null) {
                            checkReminders.postValue("Следующее напоминание:\n" + nextReminder.getName() +
                                    " в " + convertDate(nextReminderTime.get(Calendar.HOUR_OF_DAY)) + ":" +
                                    convertDate(nextReminderTime.get(Calendar.MINUTE)));
                        }
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


