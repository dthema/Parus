package com.example.parus.viewmodels;

import android.annotation.SuppressLint;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.example.parus.viewmodels.data.binding.HomeData;
import com.example.parus.viewmodels.data.models.Reminder;
import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.repositories.ReminderRepository;
import com.example.parus.viewmodels.repositories.TTSRepository;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HomeViewModel extends ViewModel {

    public HomeViewModel() {
        super();
    }

    private HomeData homeData = new HomeData();

    public void setData(HomeData homeData) {
        this.homeData = homeData;
    }

    public void showCurrentReminder(List<Reminder> reminder) {
        sortReminders(reminder);
    }

    // вывод ближайшего следущего напоминаний
    @SuppressLint("SetTextI18n")
    private void sortReminders(List<Reminder> reminders) {
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
                homeData.setCurrentReminder("Напоминаний нет");
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
                homeData.setCurrentReminder("Следующее напоминание:\n" + nextReminder.getName() +
                        " в " + convertDate(nextReminderTime.get(Calendar.HOUR_OF_DAY)) + ":" +
                        convertDate(nextReminderTime.get(Calendar.MINUTE)));
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

    public void setLastOnline(User linkUser) {
        String userId = linkUser.getUserId();
        String linkUserId = linkUser.getLinkUserId();
        String name = linkUser.getName();
        boolean isSupport = linkUser.isSupport();
        Timestamp lastOnline = linkUser.getLastOnline();
        if (userId != null && linkUserId != null) {
            if (userId.equals(linkUserId)) {
                if (isSupport)
                    homeData.setLinkUserOnline("Нет связи с подопечным");
                else
                    homeData.setLinkUserOnline("Нет связи с помощником");
            } else {
                if (lastOnline != null) {
                    Date date = lastOnline.toDate();
                    Calendar c = Calendar.getInstance();
                    Calendar d = Calendar.getInstance();
                    d.setTime(date);
                    if (c.getTimeInMillis() - d.getTimeInMillis() < 120000)
                        homeData.setLinkUserOnline(name + " онлайн");
                    else
                        homeData.setLinkUserOnline(name + " был(-a) в сети " +
                                convertDate(d.get(Calendar.DAY_OF_MONTH)) + "." + convertDate(d.get(Calendar.MONTH)) +
                                " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
                } else
                    homeData.setLinkUserOnline("Неизвестно о последней активности " + name);
            }
        }
    }

    public void setUserUI(User user) {
    }

    private TTSRepository ttsRepository = new TTSRepository();

    public void setTTS(TextToSpeech tts){
        ttsRepository.setTTS(tts);
    }

    public void setSpeed(Double speed) {
        ttsRepository.setSpeed(speed);
    }

    public void setPitch(Double pitch) {
        ttsRepository.setPitch(pitch);
    }

    public void speak(){
        ttsRepository.speak(homeData.getFastAction().substring(16));
    }

    public void stopSpeech(){
        ttsRepository.stop();
    }

    @Override
    protected void onCleared() {
        ttsRepository.destroy();
        super.onCleared();
    }
}

