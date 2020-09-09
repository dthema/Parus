package com.example.parus.ui.communication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.parus.databinding.FragmentCommunicationBinding;
import com.example.parus.ui.communication.listen.ListenActivity;
import com.example.parus.ui.communication.say.SayActivity;
import com.example.parus.ui.communication.see.SeeActivity;

public class CommunicationFragment extends Fragment {


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FragmentCommunicationBinding binding = FragmentCommunicationBinding.inflate(inflater, container, false);
        binding.buttonToSay.setOnClickListener(t -> startActivity(new Intent(getActivity(), SayActivity.class)));
        binding.buttonToSee.setOnClickListener(t -> startActivity(new Intent(getActivity(), SeeActivity.class)));
        binding.buttonToListen.setOnClickListener(t -> startActivity(new Intent(getActivity(), ListenActivity.class)));
        return binding.getRoot();
    }
}