package com.example.parus.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.R;
import com.example.parus.data.User;
import com.example.parus.services.HeartRateService;
import com.example.parus.ui.communication.listen.ListenActivity;
import com.example.parus.ui.communication.say.SayShowActivity;
import com.example.parus.ui.communication.see.SeeActivity;
import com.example.parus.ui.home.map.MapActivity;
import com.example.parus.ui.home.reminder.RemindersActivity;
import com.example.parus.viewmodels.NetworkModel;
import com.example.parus.viewmodels.ReminderModel;
import com.example.parus.viewmodels.UserModel;
import com.example.parus.viewmodels.data.Reminder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private Button pulse;
    private GoogleSignInAccount account;
    private Button toReminders;
    private User user;
    private TextView lastOnline;
    private Button fastAction;
    private ImageButton callSupport;
    private List<Reminder> reminders;
    private static final String TAG = "HomeFragment";
    private boolean isCheckThreadActive = false;
    final private static int CHECK_INTERVAL = 30000;
    private UserModel userModel;
    private UserModel linkUserModel;
    private ReminderModel reminderModel;
    private NetworkModel networkModel;
    private Double speed = 1.;
    private Double pitch = 1.;
    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    private TextToSpeech tts;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopCheckReminders();
            if (networkModel != null)
                stopCheckInternetConnection();
        } else {
            if (checkReminders != null)
                if (!checkReminders.isAlive())
                    startCheckReminders();
            if (networkModel != null)
                startCheckInternetConnection();
        }
    }

    private void InternetOff() {
        lastOnline.setText("Нет подключения к сети");
    }

    @SuppressLint("SetTextI18n")
    private void InternetOn() {
        userModel.getUploadLinkUser().observe(getViewLifecycleOwner(), linkUser -> {
            if (linkUser != null) {
                String userId = linkUser.getUserId();
                String linkUserId = linkUser.getLinkUserId();
                String name = linkUser.getName();
                boolean isSupport = linkUser.isSupport();
                Timestamp lastOnline = linkUser.getLastOnline();
                if (userId != null && linkUserId != null) {
                    if (userId.equals(linkUserId)) {
                        if (isSupport)
                            this.lastOnline.setText("Нет связи с подопечным");
                        else
                            this.lastOnline.setText("Нет связи с помощником");
                    } else {
                        if (lastOnline != null) {
                            Date date = lastOnline.toDate();
                            Calendar c = Calendar.getInstance();
                            Calendar d = Calendar.getInstance();
                            d.setTime(date);
                            if (c.getTimeInMillis() - d.getTimeInMillis() < 120000)
                                this.lastOnline.setText(name + " онлайн");
                            else
                                this.lastOnline.setText(name + " был(-a) в сети " +
                                        convertDate(d.get(Calendar.DAY_OF_MONTH)) + "." + convertDate(d.get(Calendar.MONTH)) +
                                        " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
                        } else
                            this.lastOnline.setText("Неизвестно о последней активности " + name);
                    }
                }
            }
        });
    }

    private void InternetOnUI() {
        requireActivity().runOnUiThread(this::InternetOn);
    }

    private void InternetOffUI() {
        requireActivity().runOnUiThread(this::InternetOff);
    }

    private Thread checkReminders;

    private void startCheckReminders() {
        if (!isCheckThreadActive) {
            isCheckThreadActive = true;
            checkReminders = new Thread(() -> {
                while (isCheckThreadActive) {
                    if (isVisible()) {
                        Log.d(TAG, "start");
                        requireActivity().runOnUiThread(this::sortReminders);
                        try {
                            Thread.sleep(CHECK_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            checkReminders.setName("Check Thread");
            checkReminders.start();
        }
    }

    private void stopCheckReminders() {
        isCheckThreadActive = false;
        if (checkReminders != null)
            if (!checkReminders.isInterrupted())
                checkReminders.interrupt();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCheckReminders();
        startCheckInternetConnection();
    }

    private void initModels() {
        userModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(UserModel.class);
        linkUserModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(UserModel.class);
        reminderModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(ReminderModel.class);
        networkModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(NetworkModel.class);
    }

    private void initObservers() {
        userModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            String userId = user.getUserId();
            String linkUserId = user.getLinkUserId();
            String fastAction = user.getFastAction();
            HashMap<String, Object> SaySettings = user.getSaySettings();
            boolean isSupport = user.isSupport();
            if (linkUserId == null || userId == null)
                return;
            observeReminders(userId, linkUserId, isSupport);
            if (SaySettings.get("TTS_Speed") != null)
                speed = (Double) SaySettings.get("TTS_Speed");
            if (SaySettings.get("TTS_Pitch") != null)
                pitch = (Double) SaySettings.get("TTS_Pitch");
            // быстрое действие
            if (fastAction != null)
                setFastAction(fastAction);
            if (!isSupport) {
                if (!userId.equals(linkUserId)) {
                    callSupport.setVisibility(View.VISIBLE);
                    callSupport.setOnClickListener(l -> userModel.callSupport());
                } else {
                    callSupport.setVisibility(View.GONE);
                    lastOnline.setText("Нет связи с помощником");
                }
                // отображение пульса
                startCheckHeartBPM();
            } else {
                callSupport.setVisibility(View.GONE);
                pulse.setClickable(false);
                if (linkUserId.equals(userId)) {
                    toReminders.setClickable(false);
                    pulse.setText("Нет связи\nс подопечным");
                    pulse.setTextSize(17);
                    reminders.clear();
                    sortReminders();
                    lastOnline.setText("Нет связи с подопечным");
                    if (userModel.getLinkUserData() != null)
                        userModel.removeLinkObserver(getViewLifecycleOwner());
                } else {
                    toReminders.setClickable(true);
                    if (userModel.getLinkUserData() == null)
                        observeLinkUser(linkUserId);
                }
            }
        });
    }

    private void startCheckInternetConnection() {
        MutableLiveData<Boolean> liveData = networkModel.getInternetConnection();
        if (liveData != null)
            liveData.observe(getViewLifecycleOwner(), isInternetConnected -> {
                if (isInternetConnected != null) {
                    Log.d("InternetLiveData", isInternetConnected.toString());
                    if (isInternetConnected)
                        InternetOnUI();
                    else
                        InternetOffUI();
                }
            });
    }

    private void stopCheckInternetConnection() {
        networkModel.stopCheckInternetConnection();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        tts = new TextToSpeech(getContext(), status -> {
        });
        pulse = root.findViewById(R.id.homePulse);
        Button map = root.findViewById(R.id.homeMap);
        map.setOnClickListener(c -> startActivity(new Intent(getActivity(), MapActivity.class)));
        user = new User();
        lastOnline = root.findViewById(R.id.homeLastOnline);
        lastOnline.setClickable(false);
        reminders = new ArrayList<>();
        toReminders = root.findViewById(R.id.reminderButton);
        toReminders.setOnClickListener(l -> startActivity(new Intent(getContext(), RemindersActivity.class)));
        toReminders.setText("Напоминаний нет");
        fastAction = root.findViewById(R.id.homeFast);
        callSupport = root.findViewById(R.id.homeCallSupport);
        initModels();
        initObservers();
        if (Build.VERSION.SDK_INT >= 23) {
            mStore = new HealthDataStore(requireActivity(), mConnectionListener);
        }
        return root;
    }

    @SuppressLint("SetTextI18n")
    private void observeLinkUser(String linkUserId) {
        linkUserModel.getLinkUserData(linkUserId).observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                return;
            Long BPM = user.getPulse();
            boolean check = user.isCheckHeartBPM();
            if (check) {
                pulse.setText("Пульс: Данные не обнаружены");
                pulse.setTextSize(17);
                if (BPM != null)
                    if (BPM != 0) {
                        pulse.setText("Пульс:\n" + BPM + " у/м");
                        pulse.setTextSize(30);
                    }
            } else {
                pulse.setText("Пульс: У подопечного отключено отслеживание");
                pulse.setTextSize(17);
            }
        });
    }

    private void observeReminders(String userId, String linkUserId, boolean isSupport) {
        reminderModel.getProductList(userId, linkUserId, isSupport).observe(getViewLifecycleOwner(), reminder -> {
            if (isSupport && userId.equals(linkUserId)) {
                reminders.clear();
                sortReminders();
                reminderModel.removeObserver(getViewLifecycleOwner());
                return;
            }
            reminders = reminder;
            sortReminders();
        });
    }

    // вывод ближайшего следущего напоминаний
    @SuppressLint("SetTextI18n")
    private void sortReminders() {
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
                    toReminders.setText("Следующее напоминание:\n" + pairs.get(i).first.second + " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
                    return;
                }
            }
            Date date = pairs.get(0).second;
            Calendar d = Calendar.getInstance();
            d.setTime(date);
            toReminders.setText("Следующее напоминание:\n" + pairs.get(0).first.second + " в " + convertDate(d.get(Calendar.HOUR_OF_DAY)) + ":" + convertDate(d.get(Calendar.MINUTE)));
        } else
            toReminders.setText("Напоминаний нет");
    }


    private String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

    @SuppressLint("SetTextI18n")
    private void setFastAction(String action) {
        switch (action) {
            case "0":
                fastAction.setText("Быстрое действие не выбрано");
                fastAction.setOnClickListener(l -> {
                    DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
                    dialogChooseFastAction.show(requireActivity().getSupportFragmentManager(), "DialogFastAction");
                });
                break;
            case "1":
                fastAction.setText("Распознать текст");
                fastAction.setOnClickListener(l -> {
                    Intent intent = new Intent(getActivity(), SeeActivity.class);
                    intent.putExtra("fastAction", 1);
                    startActivity(intent);
                });
                break;
            case "2":
                fastAction.setText("Распознать объект");
                fastAction.setOnClickListener(l -> {
                    Intent intent = new Intent(getActivity(), SeeActivity.class);
                    intent.putExtra("fastAction", 2);
                    startActivity(intent);
                });
                break;
            case "3":
                fastAction.setText("Начать слушать");
                fastAction.setOnClickListener(l -> {
                    Intent intent = new Intent(getActivity(), ListenActivity.class);
                    intent.putExtra("fastAction", true);
                    startActivity(intent);
                });
                break;
            default:
                fastAction.setText("Сказать/Показать " + action);
                tts.setSpeechRate(Float.parseFloat(String.valueOf(speed)));
                tts.setPitch(Float.parseFloat(String.valueOf(pitch)));
                fastAction.setOnClickListener(l -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(action, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        tts.speak(action, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
                fastAction.setOnLongClickListener(l -> {
                    Intent intent = new Intent(getActivity(), SayShowActivity.class);
                    intent.putExtra("word", action);
                    startActivity(intent);
                    return true;
                });
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void startCheckHeartBPM() {
        if (Build.VERSION.SDK_INT < 23) {
            FitnessOptions fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                    .build();
            account = GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions);
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED) {
                pulse.setText("Пульс: Нет прав");
                pulse.setTextSize(17);
                pulse.setClickable(true);
            } else if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                pulse.setText("Пульс: Google аккаунт не подключён");
                pulse.setTextSize(17);
                pulse.setClickable(true);
            } else {
                pulse.setTextSize(17);
                pulse.setClickable(false);
                checkPulse();
            }
            pulse.setOnClickListener(c -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BODY_SENSORS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 100);
                } else if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                    GoogleSignIn.requestPermissions(
                            this,
                            1000,
                            account,
                            fitnessOptions);
                }
            });
        } else {
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED) {
                pulse.setText("Пульс: Нет прав");
                pulse.setTextSize(17);
                pulse.setClickable(true);
                pulse.setOnClickListener(l -> requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 100));
            } else {
                checkPulse();
                mStore.connectService();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mStore != null)
            mStore.disconnectService();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        stopCheckReminders();
        stopCheckInternetConnection();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (!isPermissionAcquired()) {
                pulse.setText("Пульс: Samsung Health не подключён");
                pulse.setTextSize(17);
                pulse.setClickable(true);
                pulse.setOnClickListener(l -> requestPermission());
            } else {
                pulse.setTextSize(17);
                pulse.setClickable(false);
                checkPulse();
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
        }
    };

    @SuppressLint("SetTextI18n")
    private void checkPulse() {
        user.getDatabase().collection("users").document(user.getUser().getUid()).get()
                .addOnSuccessListener(l -> {
                    Boolean check = l.getBoolean("checkHeartBPM");
                    if (check != null)
                        if (check) {
                            pulse.setText("Пульс: Данные не обнаружены");
                            pulse.setTextSize(17);
                            if (l.getLong("pulse") != null)
                                if (l.getLong("pulse") != 0) {
                                    pulse.setText("Пульс:\n" + l.getLong("pulse") + " у/м");
                                    pulse.setTextSize(30);
                                }
                        } else {
                            pulse.setText("Пульс: Отслеживание отключено");
                            pulse.setTextSize(17);
                        }
                });
    }

    private void showPermissionAlarmDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());
        alert.setTitle("Не удалось подключиться к Samsung Health")
                .setMessage("Для отслеживания пульса из Samsung Health необходимо выдать нужные разрешения\n" +
                        "На данный момент приложение не является партнёром Samsung Health, " +
                        "поэтому включите режим разработчика в Samsung Health, чтобы приложение могло считывать данные о пульсе\n" +
                        "Чтобы его включить перейдите во вкладку 'О Samsung Health' в настройках приложения и несколько раз нажите на версию приложения")
                .setPositiveButton("Ok", null)
                .show();
    }

    private boolean isPermissionAcquired() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(Collections.singleton(permKey));
            return resultMap.get(permKey);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            pmsManager.requestPermissions(Collections.singleton(permKey), getActivity())
                    .setResultListener(result -> {
                        Log.d(TAG, "Permission callback is received.");
                        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();
                        if (resultMap.containsValue(Boolean.FALSE)) {
                            showPermissionAlarmDialog();
                        } else {
                            pulse.setText("Пульс: Данные не обнаружены");
                            pulse.setTextSize(17);
                            pulse.setClickable(false);
                            requireActivity().startService(new Intent(getActivity(), HeartRateService.class).setAction("action"));
                            FirebaseFirestore.getInstance().collection("users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).update("checkHeartBPM", true);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

        AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());
        mConnError = error;
        String message = "Не удалось подключиться к Samsung Health";
        if (mConnError.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    message = "Установите Samsung Health";
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    message = "Обновите Samsung Health";
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    message = "Включите Samsung Health";
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    message = "Пожалуйста, согласитесь с политикой Samsung Health";
                    break;
            }
        }

        alert.setMessage(message);

        alert.setPositiveButton("OK", (dialog, id) -> {
            if (mConnError.hasResolution()) {
                mConnError.resolve(requireActivity());
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton("Cancel", null);
        }

        alert.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pulse.setText("Аккаунт\nне подключён");
                pulse.setTextSize(17);
                FitnessOptions fitnessOptions = FitnessOptions.builder()
                        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                        .build();
                if (GoogleSignIn.hasPermissions(account, fitnessOptions))
                    pulse.setText("Данные не\nобнаружены");
            }
        }
        Log.d(TAG + "_request", String.valueOf(requestCode));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                requireActivity().startService(new Intent(getActivity(), HeartRateService.class).setAction("action"));
            }
            Log.d(TAG + "_acResult", String.valueOf(resultCode));
        }
        Log.d(TAG + "_acRequest", String.valueOf(requestCode));
    }
}