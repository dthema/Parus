package com.example.parus.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.R;
import com.example.parus.databinding.FragmentHomeBinding;
import com.example.parus.services.HeartRateService;
import com.example.parus.ui.communication.listen.ListenActivity;
import com.example.parus.ui.communication.say.SayShowActivity;
import com.example.parus.ui.communication.see.SeeActivity;
import com.example.parus.ui.home.map.MapActivity;
import com.example.parus.ui.home.reminder.RemindersActivity;
import com.example.parus.viewmodels.HealthModel;
import com.example.parus.viewmodels.HomeViewModel;
import com.example.parus.viewmodels.NetworkModel;
import com.example.parus.viewmodels.ReminderModel;
import com.example.parus.viewmodels.UserModel;
import com.example.parus.viewmodels.data.binding.HomeData;
import com.example.parus.viewmodels.data.models.Reminder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private List<Reminder> reminders;
    private static final String TAG = "HomeFragment";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
    private static final int SAMSUNG_NO_CONNECT = 3;
    private UserModel userModel;
    private ReminderModel reminderModel;
    private NetworkModel networkModel;
    private HealthModel healthModel;
    private HomeViewModel homeViewModel;
    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    private HomeData homeData;
    private FragmentHomeBinding homeBinding;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            homeViewModel.stopSpeech();
            if (mStore != null)
                mStore.disconnectService();
            if (networkModel != null)
                stopCheckInternetConnection();
        } else {
            if (networkModel != null)
                startCheckInternetConnection();
        }
    }

    private void InternetOff() {
        homeData.setLinkUserOnline("Нет подключения к сети");
    }

    @SuppressLint("SetTextI18n")
    private void InternetOn() {
        userModel.getUploadLinkUser().observe(getViewLifecycleOwner(), linkUser -> {
            if (linkUser != null) {
                homeViewModel.setLastOnline(linkUser);
            }
        });
    }

    private void InternetOnUI() {
        requireActivity().runOnUiThread(this::InternetOn);
    }

    private void InternetOffUI() {
        requireActivity().runOnUiThread(this::InternetOff);
    }

    @Override
    public void onResume() {
        super.onResume();
        startCheckInternetConnection();
    }

    private void initModels() {
        userModel = new ViewModelProvider(this).get(UserModel.class);
        reminderModel = new ViewModelProvider(this).get(ReminderModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        networkModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(NetworkModel.class);
        healthModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(HealthModel.class);
    }

    private void initObservers() {
        userModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            homeViewModel.setUserUI(user);
            String userId = user.getUserId();
            String linkUserId = user.getLinkUserId();
            String fastAction = user.getFastAction();
            HashMap<String, Object> SaySettings = user.getSaySettings();
            boolean isSupport = user.isSupport();
            if (linkUserId == null || userId == null)
                return;
            observeReminders(userId, linkUserId, isSupport);
            if (SaySettings.get("TTS_Speed") != null)
                homeViewModel.setSpeed((Double) SaySettings.get("TTS_Speed"));
            if (SaySettings.get("TTS_Pitch") != null)
                homeViewModel.setPitch((Double) SaySettings.get("TTS_Pitch"));
            // быстрое действие
            if (fastAction != null)
                setFastAction(fastAction);
            if (!isSupport) {
                if (!userId.equals(linkUserId)) {
                    homeBinding.homeCallSupport.setVisibility(View.VISIBLE);
                    homeBinding.homeCallSupport.setOnClickListener(l -> userModel.callSupport().observe(getViewLifecycleOwner(),
                            send -> Toast.makeText(getContext(), "Уведомление отправлено помощнику", Toast.LENGTH_LONG).show()));
                } else {
                    homeBinding.homeCallSupport.setVisibility(View.GONE);
                }
                // отображение пульса
                healthModel.get().observe(getViewLifecycleOwner(), result -> {
                    switch (result) {
                        case NO_PERMISSION:
                            homeBinding.homePulse.setClickable(true);
                            homeData.setHeartRate("Пульс: Нет прав");
                            break;
                        case NO_GOOGLE_ACCOUNT:
                            homeBinding.homePulse.setClickable(true);
                            homeData.setHeartRate("Пульс: Google Аккаунт не подключён");
                            break;
                        case SAMSUNG_NO_CONNECT:
                            homeBinding.homePulse.setClickable(true);
                            homeData.setHeartRate("Пульс: Samsung Health не подключён");
                            break;
                        default:
                            homeBinding.homePulse.setClickable(false);
                            if (user.isCheckHeartBPM()) {
                                homeData.setHeartRate("Пульс: Данные не обнаружены");
                                Long BPM = user.getPulse();
                                if (BPM != 0)
                                    homeData.setHeartRate("Пульс: " + BPM + " у/м");
                            } else
                                homeData.setHeartRate("Пульс: Отслеживание отключено");
                            break;
                    }
                });
            } else {
                homeBinding.homeCallSupport.setVisibility(View.GONE);
                homeBinding.homePulse.setClickable(false);
                if (linkUserId.equals(userId)) {
                    homeData.setHeartRate("Нет связи\nс подопечным");
                    homeBinding.reminderButton.setClickable(false);
                    reminders.clear();
                    if (reminderLiveData != null) {
                        reminderModel.removeObserver(getViewLifecycleOwner());
                        reminderLiveData = null;
                    }
                    if (userModel.getUserDataById() != null)
                        userModel.removeLinkObserver(getViewLifecycleOwner());
                } else {
                    homeBinding.reminderButton.setClickable(true);
                    if (userModel.getUserDataById() == null)
                        observeLinkUser(linkUserId);
                }
            }
        });
    }

    private void startCheckInternetConnection() {
        MutableLiveData<Boolean> liveData = networkModel.getInternetConnection();
        if (liveData != null) {
            liveData.observe(getViewLifecycleOwner(), isInternetConnected -> {
                if (isInternetConnected != null) {
                    if (isInternetConnected)
                        InternetOnUI();
                    else
                        InternetOffUI();
                }
            });
        }
    }

    private void stopCheckInternetConnection() {
        networkModel.stopCheckInternetConnection();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeData = new HomeData();
        homeBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false);
        homeBinding.setViewmodel(homeViewModel);
        homeBinding.setData(homeData);
        homeBinding.setLifecycleOwner(getViewLifecycleOwner());
        homeBinding.setFragment(this);
        homeBinding.reminderButton.setOnClickListener(view ->
                userModel.getSingleUserData().observe(getViewLifecycleOwner(), pair -> {
                    if (pair.first == null)
                        return;
                    String userId = pair.first.first;
                    String linkUserId = pair.first.second;
                    Boolean isSupport = pair.second;
                    if (!userId.equals(linkUserId) || !isSupport) {
                        requireActivity().startActivity(new Intent(requireContext(), RemindersActivity.class));
                    }
                }));
        homeBinding.homeMap.setOnClickListener(c -> startActivity(new Intent(getActivity(), MapActivity.class)));
        initModels();
        homeViewModel.setData(homeData);
        initObservers();
        if (Build.VERSION.SDK_INT >= 23) {
            mStore = new HealthDataStore(requireActivity(), mConnectionListener);
        }
        reminders = new ArrayList<>();
        homeViewModel.setTTS(new TextToSpeech(requireContext(), status -> {}));
        return homeBinding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    private void observeLinkUser(String linkUserId) {
        userModel.getUserDataById(linkUserId).observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                return;
            Long BPM = user.getPulse();
            boolean check = user.isCheckHeartBPM();
            if (check) {
                homeData.setHeartRate("Пульс: Данные не обнаружены");
                if (BPM != null)
                    if (BPM != 0)
                        homeData.setHeartRate("Пульс:\n" + BPM + " у/м");
            } else
                homeData.setHeartRate("Пульс: У подопечного отключено отслеживание");
        });
    }

    private LiveData<List<Reminder>> reminderLiveData;

    private void observeReminders(String userId, String linkUserId, boolean isSupport) {
        if (reminderLiveData != null)
            return;
        reminderLiveData = reminderModel.getProductList(userId, linkUserId, isSupport);
        reminderLiveData.observe(getViewLifecycleOwner(), reminder -> {
            if (isSupport && userId.equals(linkUserId)) {
                reminders.clear();
                reminderModel.removeObserver(getViewLifecycleOwner());
            } else
                reminders = reminder;
            homeViewModel.showCurrentReminder(reminder);
        });
    }

    @SuppressLint("SetTextI18n")
    private void setFastAction(String action) {
        switch (action) {
            case "0":
                homeData.setFastAction("Быстрое действие не выбрано");
                homeBinding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "1":
                homeData.setFastAction("Распознать текст");
                homeBinding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "2":
                homeData.setFastAction("Распознать объект");
                homeBinding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "3":
                homeData.setFastAction("Начать слушать");
                homeBinding.homeFast.setOnLongClickListener(l -> true);
                break;
            default:
                homeData.setFastAction("Сказать/Показать: " + action);
                homeBinding.homeFast.setOnLongClickListener(l -> {
                    Intent intent = new Intent(getActivity(), SayShowActivity.class);
                    intent.putExtra("word", action);
                    startActivity(intent);
                    return true;
                });
                break;
        }
    }

    @Override
    public void onPause() {
        homeViewModel.stopSpeech();
        stopCheckInternetConnection();
        if (mStore != null)
            mStore.disconnectService();
        super.onPause();
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (healthModel.isPermissionAcquired(mStore)) {
                homeData.setHeartRate("Пульс: Samsung Health не подключён");
                requestPermission();
            } else {
                homeData.setHeartRate("Данные не обнаружены");
                mStore.disconnectService();
                homeBinding.homePulse.setClickable(false);
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
            String message = "Не удалось подключиться к Samsung Health";
            if (error.hasResolution()) {
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
                if (error.hasResolution()) {
                    error.resolve(requireActivity());
                }
            });
            if (error.hasResolution()) {
                alert.setNegativeButton("Cancel", null);
            }
            alert.show();
            mStore.disconnectService();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
        }
    };

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
                            homeData.setHeartRate("Пульс: Данные не обнаружены");
                            homeBinding.homePulse.setClickable(false);
                            requireActivity().startService(new Intent(getActivity(), HeartRateService.class).setAction("action"));
                            FirebaseFirestore.getInstance().collection("users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).update("checkHeartBPM", true);
                        }
                        mStore.disconnectService();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
            mStore.disconnectService();
        }
    }


    public void onHealthClick() {
        healthModel.get().observe(getViewLifecycleOwner(), result -> {
            switch (result) {
                case 0:
                    requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 100);
                    break;
                case 1:
                    FitnessOptions fitnessOptions = FitnessOptions.builder()
                            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                            .build();
                    GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions);
                    GoogleSignIn.requestPermissions(
                            this,
                            1000,
                            account,
                            fitnessOptions);
                    break;
                case 3:
                    mStore.connectService();
                    break;
            }
        });
    }

    public void onFastActionClick() {
        switch (homeData.getFastAction()) {
            case "Быстрое действие не выбрано":
                DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
                dialogChooseFastAction.show(requireActivity().getSupportFragmentManager(), "DialogFastAction");
                break;
            case "Распознать текст":
                Intent intent1 = new Intent(getActivity(), SeeActivity.class);
                intent1.putExtra("fastAction", 1);
                startActivity(intent1);
                break;
            case "Распознать объект":
                Intent intent2 = new Intent(getActivity(), SeeActivity.class);
                intent2.putExtra("fastAction", 2);
                startActivity(intent2);
                break;
            case "Начать слушать":
                Intent intent3 = new Intent(getActivity(), ListenActivity.class);
                intent3.putExtra("fastAction", true);
                startActivity(intent3);
                break;
            default:
                homeViewModel.speak();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        healthModel.onRequestPermissionsResult(requestCode, permissions, grantResults).observe(getViewLifecycleOwner(), result -> {
            switch (result) {
                case NO_PERMISSION:
                    homeData.setHeartRate("Пульс: Нет прав");
                    homeBinding.homePulse.setClickable(true);
                    break;
                case NO_GOOGLE_ACCOUNT:
                    homeData.setHeartRate("Пульс: Google Аккаунт не подключён");
                    homeBinding.homePulse.setClickable(true);
                    break;
                case SAMSUNG_NO_CONNECT:
                    homeData.setHeartRate("Пульс: Samsung Health не подключён");
                    homeBinding.homePulse.setClickable(true);
                    break;
                default:
                    homeData.setHeartRate("Пульс: Данные не обнаружены");
                    homeBinding.homePulse.setClickable(false);
                    break;
            }
        });
        Log.d(TAG + "_request", String.valueOf(requestCode));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        healthModel.onActivityResult(requestCode, resultCode, data);
    }
}