package com.begletsov.parus.ui.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.begletsov.parus.LoginActivity;
import com.begletsov.parus.R;
import com.begletsov.parus.databinding.FragmentAccountBinding;
import com.begletsov.parus.services.OnlineService;
import com.begletsov.parus.viewmodels.ServiceViewModel;
import com.begletsov.parus.viewmodels.UserViewModel;

import java.util.Objects;

public class AccountFragment extends Fragment {

    private UserViewModel userViewModel;
    private ServiceViewModel serviceViewModel;
    private FragmentAccountBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.btnExitAccount.setOnClickListener(e -> {
            // отключение уведомлений(token) и всех служб перед выходом из аккаунта
            userViewModel.exit();
            serviceViewModel.stopAllServices();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.putExtra("del", true);
            startActivity(intent);
            requireActivity().finish();
        });
        binding.accountSettings.setOnClickListener(l -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        initViewModels();
        initObservers();
        return binding.getRoot();
    }

    private void initViewModels(){
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        serviceViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(ServiceViewModel.class);
    }

    private void initObservers(){
        userViewModel.getSingleUserData().observe(getViewLifecycleOwner(), user -> {
            binding.setUser(user);
            // копирование ID при долгом нажатии
            binding.infoId.setOnLongClickListener(l -> {
                ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ID пользователя", user.getUserId());
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                Toast.makeText(getActivity(), "ID скопирован в буфер обмена", Toast.LENGTH_LONG).show();
                return true;
            });
            if (user.isSupport()) {
                binding.infoLinkNameLabel.setText("Имя подопечного:");
            } else {
                binding.infoLinkNameLabel.setText("Имя помощника:");
            }
        });
        userViewModel.getShortUserData().observe(getViewLifecycleOwner(), pair -> {
            if (pair.first == null)
                return;
            String userId = pair.first.first;
            String linkUserId = pair.first.second;
            Boolean isSupport = pair.second;
            if (userId == null || linkUserId == null || isSupport == null)
                return;
            if (userId.equals(linkUserId)) {
                // привязывание другого пользователя
                binding.accountLink.setOnClickListener(l -> {
                    DialogLinkUser dialogLinkUser = DialogLinkUser.newInstance(isSupport);
                    dialogLinkUser.show(requireActivity().getSupportFragmentManager(), "DialogLinkUser");
                });
                if (isSupport) {
                    binding.infoLinkName.setText(getString(R.string.no_support_link));
                    binding.accountLink.setText("Связать аккаунт с подопечным");
                } else {
                    binding.infoLinkName.setText(getString(R.string.no_disabled_link));
                    binding.accountLink.setText("Связать аккаунт с помощником");
                }
            } else {
                // отвязывание от другого пользователя
                binding.accountLink.setOnClickListener(l -> {
                    requireActivity().stopService(new Intent(requireActivity(), OnlineService.class));
                    userViewModel.removeLinkUser();
                    if (isSupport)
                        WorkManager.getInstance(requireContext()).cancelAllWork();
                });
                binding.accountLink.setText("Разорвать связь");
                userViewModel.getSingleLinkUserData().observe(getViewLifecycleOwner(), linkUser ->
                        binding.setLinkUser(linkUser));
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.three_dop_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_filter){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
