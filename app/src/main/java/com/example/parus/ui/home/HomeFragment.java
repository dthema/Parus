package com.example.parus.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
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
import com.example.parus.viewmodels.TTSViewModel;
import com.example.parus.viewmodels.UserModel;
import com.example.parus.viewmodels.data.binding.HomeData;
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
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

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
    private HomeData homeData;
    private FragmentHomeBinding binding;
    private TTSViewModel TTS;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            TTS.stopSpeech();
            if (mStore != null)
                mStore.disconnectService();
            stopCheckInternetConnection();
            stopCheckReminders();
        } else {
            startCheckInternetConnection();
            startCheckReminders();
        }
    }

    private void InternetOff() {
        homeData.setLinkUserOnline(getString(R.string.not_internet));
    }

    @SuppressLint("SetTextI18n")
    private void InternetOn() {
        userModel.getSingleLinkUserData().observe(getViewLifecycleOwner(), linkUser -> {
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
        startCheckReminders();
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
        TTS = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TTSViewModel.class);
    }

    private void initObservers() {
        userModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            String userId = user.getUserId();
            String linkUserId = user.getLinkUserId();
            String fastAction = user.getFastAction();
            boolean isSupport = user.isSupport();
            HashMap<String, Object> SaySettings = user.getSaySettings();
            if (linkUserId == null || userId == null)
                return;
            if ((!isSupport || !userId.equals(linkUserId)) && reminderModel.getReminderData(false) == null) {
                observeReminders(userId, linkUserId, isSupport);
            }
            if (SaySettings.get("TTS_Speed") != null)
                TTS.setSpeed((Double) SaySettings.get("TTS_Speed"));
            if (SaySettings.get("TTS_Pitch") != null)
                TTS.setPitch((Double) SaySettings.get("TTS_Pitch"));
            // быстрое действие
            if (fastAction != null)
                setFastAction(fastAction);
            if (!isSupport) {
                if (!userId.equals(linkUserId)) {
                    binding.homeCallSupport.setVisibility(View.VISIBLE);
                    binding.homeCallSupport.setOnClickListener(l -> userModel.callSupport().observe(getViewLifecycleOwner(),
                            send -> Toast.makeText(getContext(), R.string.notification_send, Toast.LENGTH_LONG).show()));
                } else {
                    binding.homeCallSupport.setVisibility(View.GONE);
                }
                // отображение пульса
                healthModel.get().observe(getViewLifecycleOwner(), result -> {
                    switch (result) {
                        case NO_PERMISSION:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.no_pulse_rights));
                            break;
                        case NO_GOOGLE_ACCOUNT:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.google_account_not_connected));
                            break;
                        case SAMSUNG_NO_CONNECT:
                            binding.homePulse.setClickable(true);
                            homeData.setHeartRate(getString(R.string.samsung_not_connected));
                            break;
                        default:
                            binding.homePulse.setClickable(false);
                            if (user.isCheckHeartBPM()) {
                                homeData.setHeartRate(getString(R.string.no_pulse_data));
                                Long BPM = user.getPulse();
                                if (BPM != 0)
                                    homeData.setHeartRate("Пульс: " + BPM + " у/м");
                            } else
                                homeData.setHeartRate(getString(R.string.no_pulse_checked));
                            break;
                    }
                });
            } else {
                binding.homeCallSupport.setVisibility(View.GONE);
                binding.homePulse.setClickable(false);
                if (linkUserId.equals(userId)) {
                    stopCheckReminders();
                    homeData.setCurrentReminder(getString(R.string.no_reminders));
                    homeData.setHeartRate(getString(R.string.no_support_link));
                    binding.reminderButton.setClickable(false);
                    if (reminderModel.getReminderData(false) != null) {
                        reminderModel.removeObserver(getViewLifecycleOwner());
                        reminderModel.setReminders(null);
                    }
                    if (userModel.getOtherUserData() != null)
                        userModel.removeLinkObserver(getViewLifecycleOwner());
                } else {
                    startCheckReminders();
                    binding.reminderButton.setClickable(true);
                    if (userModel.getOtherUserData() == null)
                        observeLinkUserPulse(linkUserId);
                }
            }
        });
        reminderModel.setReminders(new ArrayList<>());
    }

    private void startCheckInternetConnection() {
        LiveData<Boolean> liveData = networkModel.getInternetConnection();
        if (liveData != null)
            liveData.observe(getViewLifecycleOwner(), isInternetConnected -> {
                if (isInternetConnected != null) {
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

    private void startCheckReminders() {
        LiveData<String> liveData = reminderModel.startCheckReminders();
        if (liveData != null) {
            userModel.getSingleLinkUserData().observe(getViewLifecycleOwner(), user -> {
                if (!user.isSupport() || !user.getLinkUserId().equals(user.getUserId())) {
                    liveData.observe(getViewLifecycleOwner(), s -> homeData.setCurrentReminder(s));
                }
            });
        }
    }

    private void stopCheckReminders() {
        reminderModel.stopCheckReminders();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeData = new HomeData();
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.setViewmodel(homeViewModel);
        binding.setData(homeData);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setFragment(this);
        binding.reminderButton.setOnClickListener(view ->
                userModel.getSingleShortUserData().observe(getViewLifecycleOwner(), pair -> {
                    if (pair.first == null)
                        return;
                    String userId = pair.first.first;
                    String linkUserId = pair.first.second;
                    Boolean isSupport = pair.second;
                    if (userId == null || linkUserId == null || isSupport == null)
                        return;
                    if (!userId.equals(linkUserId) || !isSupport) {
                        requireActivity().startActivity(new Intent(requireContext(), RemindersActivity.class));
                    }
                }));
        binding.homeMap.setOnClickListener(c -> startActivity(new Intent(getActivity(), MapActivity.class)));
        binding.homeLastOnline.setClickable(false);
        initModels();
        homeViewModel.setData(homeData);
        initObservers();
        if (Build.VERSION.SDK_INT >= 23) {
            mStore = new HealthDataStore(requireActivity(), mConnectionListener);
        }
        return binding.getRoot();
    }


    @SuppressLint("SetTextI18n")
    private void observeLinkUserPulse(String linkUserId) {
        userModel.getOtherUserData(linkUserId, true).observe(getViewLifecycleOwner(), user -> {
            if (user == null)
                return;
            Long BPM = user.getPulse();
            boolean check = user.isCheckHeartBPM();
            if (check) {
                homeData.setHeartRate(getString(R.string.no_pulse_data));
                if (BPM != null)
                    if (BPM != 0)
                        homeData.setHeartRate("Пульс:\n" + BPM + " у/м");
            } else
                homeData.setHeartRate(getString(R.string.no_link_pulse_checked));
        });
    }

    private void observeReminders(String userId, String linkUserId, boolean isSupport) {
        if (reminderModel.getReminderData(false) != null || isSupport && userId.equals(linkUserId))
            return;
        reminderModel.getReminderData(userId, linkUserId, isSupport).observe(getViewLifecycleOwner(), reminders -> {
            reminderModel.setReminders(reminders);
            homeViewModel.showCurrentReminder(reminders);
        });
    }

    @SuppressLint("SetTextI18n")
    private void setFastAction(String action) {
        switch (action) {
            case "0":
                homeData.setFastAction(getString(R.string.fast_action_no_choose));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "1":
                homeData.setFastAction(getString(R.string.detect_text));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "2":
                homeData.setFastAction(getString(R.string.detect_object));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            case "3":
                homeData.setFastAction(getString(R.string.start_listen));
                binding.homeFast.setOnLongClickListener(l -> true);
                break;
            default:
                homeData.setFastAction(getString(R.string.say_and_show) + " " + action);
                binding.homeFast.setOnLongClickListener(l -> {
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
        TTS.stopSpeech();
        stopCheckInternetConnection();
        stopCheckReminders();
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
                homeData.setHeartRate(getString(R.string.no_pulse_data));
                mStore.disconnectService();
                binding.homePulse.setClickable(false);
            } else {
                homeData.setHeartRate(getString(R.string.samsung_not_connected));
                requestPermission();
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
            String message = getString(R.string.cannot_connect_to_shealth);
            if (error.hasResolution()) {
                switch (error.getErrorCode()) {
                    case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                        message = getString(R.string.download_shealth);
                        break;
                    case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                        message = getString(R.string.update_shealth);
                        break;
                    case HealthConnectionErrorResult.PLATFORM_DISABLED:
                        message = getString(R.string.enable_shealth);
                        break;
                    case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                        message = getString(R.string.agree_with_shealth_policy);
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
        alert.setTitle(R.string.cannot_connect_to_shealth)
                .setMessage(R.string.shealth_dialog_message)
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
                            homeData.setHeartRate(getString(R.string.no_pulse_data));
                            binding.homePulse.setClickable(false);
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
        if (homeData.getFastAction().equals(getString(R.string.fast_action_no_choose))) {
            DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
            dialogChooseFastAction.show(requireActivity().getSupportFragmentManager(), "DialogFastAction");
        } else if (homeData.getFastAction().equals(getString(R.string.detect_text))) {
            Intent intent1 = new Intent(getActivity(), SeeActivity.class);
            intent1.putExtra("fastAction", 1);
            startActivity(intent1);
        } else if (homeData.getFastAction().equals(getString(R.string.detect_object))) {
            Intent intent2 = new Intent(getActivity(), SeeActivity.class);
            intent2.putExtra("fastAction", 2);
            startActivity(intent2);
        } else if (homeData.getFastAction().equals(getString(R.string.start_listen))) {
            Intent intent3 = new Intent(getActivity(), ListenActivity.class);
            intent3.putExtra("fastAction", true);
            startActivity(intent3);
        } else {
            TTS.speak(homeData.getFastAction().substring(16));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        healthModel.onRequestPermissionsResult(requestCode, permissions, grantResults).observe(getViewLifecycleOwner(), result -> {
            switch (result) {
                case NO_PERMISSION:
                    homeData.setHeartRate(getString(R.string.no_pulse_rights));
                    binding.homePulse.setClickable(true);
                    break;
                case NO_GOOGLE_ACCOUNT:
                    homeData.setHeartRate(getString(R.string.google_account_not_connected));
                    binding.homePulse.setClickable(true);
                    break;
                case SAMSUNG_NO_CONNECT:
                    homeData.setHeartRate(getString(R.string.samsung_not_connected));
                    binding.homePulse.setClickable(true);
                    break;
                default:
                    homeData.setHeartRate(getString(R.string.no_pulse_data));
                    binding.homePulse.setClickable(false);
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