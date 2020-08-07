package com.example.parus.viewmodels;

import android.annotation.SuppressLint;
import android.speech.tts.TextToSpeech;
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

    ReminderRepository repository = new ReminderRepository();

    public ObservableField<String> currentReminder = new ObservableField<>();

    public HomeData homeData = new HomeData();

    public void startCheckReminder(){
    }

    public void setData(HomeData homeData) {
        this.homeData = homeData;
    }

    public void showCurrentReminder(List<Reminder> reminder) {
        sortReminders(reminder);
    }

    // вывод ближайшего следущего напоминаний
    @SuppressLint("SetTextI18n")
    private void sortReminders(List<Reminder> reminders) {
        if (reminders.size() > 0) {
            List<Pair<Pair<String, String>, Date>> pairs = new ArrayList<>();
            for (Reminder reminder : reminders) {
                if (reminder.getType() == 0) {
                    Date start = reminder.getTimeStart();
                    Calendar s = Calendar.getInstance();
                    s.setTime(start);
                    Date end = reminder.getTimeEnd();
                    Calendar e1 = Calendar.getInstance();
                    e1.setTime(end);
                    Date interval = reminder.getTimeInterval();
                    Calendar i = Calendar.getInstance();
                    i.setTime(interval);
                    pairs.add(Pair.create(Pair.create(reminder.getId(), reminder.getName()), start));
                    while (s.getTime().compareTo(e1.getTime()) <= 0) {
                        int h = s.get(Calendar.HOUR_OF_DAY) + i.get(Calendar.HOUR_OF_DAY);
                        int m = s.get(Calendar.MINUTE) + i.get(Calendar.MINUTE);
                        if (m >= 60) {
                            m -= 60;
                            h++;
                        }
                        s.set(Calendar.HOUR_OF_DAY, h);
                        s.set(Calendar.MINUTE, m);
                        if (s.getTime().compareTo(e1.getTime()) <= 0) {
                            Date date = s.getTime();
                            pairs.add(Pair.create(Pair.create(reminder.getId(), reminder.getName()), date));
                        }
                    }
                } else if (reminder.getType() == 1) {
                    for (Date date : reminder.getTimers()) {
                        pairs.add(Pair.create(Pair.create(reminder.getId(), reminder.getName()), date));
                    }
                }
            }
            Collections.sort(pairs, (r1, r2) -> r1.second.compareTo(r2.second));
            Calendar c = Calendar.getInstance();
            for (int i = 0; i < pairs.size(); i++) {
                Date date = pairs.get(i).second;
                Calendar d = Calendar.getInstance();
                d.setTime(date);
                d.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), c.get(Calendar.SECOND));
                if ((c.get(Calendar.HOUR_OF_DAY) == d.get(Calendar.HOUR_OF_DAY) && c.get(Calendar.MINUTE) < d.get(Calendar.MINUTE)) || c.get(Calendar.HOUR_OF_DAY) < d.get(Calendar.HOUR_OF_DAY)) {
                    homeData.setCurrentReminder("Следующее напоминание:\n" + pairs.get(i).first.second + " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
                    return;
                }
            }
            Date date = pairs.get(0).second;
            Calendar d = Calendar.getInstance();
            d.setTime(date);
            homeData.setCurrentReminder("Следующее напоминание:\n" + pairs.get(0).first.second + " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
        }
        else
            homeData.setCurrentReminder("Напоминаний нет");
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

