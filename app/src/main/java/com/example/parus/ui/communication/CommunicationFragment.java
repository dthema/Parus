package com.example.parus.ui.communication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.parus.R;
import com.example.parus.ui.communication.listen.ListenActivity;
import com.example.parus.ui.communication.say.SayActivity;
import com.example.parus.ui.communication.see.SeeActivity;

public class CommunicationFragment extends Fragment {


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_communication, container , false);

        Button say = root.findViewById(R.id.buttonToSay);
        say.setOnClickListener(t -> startActivity(new Intent(getActivity(), SayActivity.class)));
        Button see = root.findViewById(R.id.buttonToSee);
        see.setOnClickListener(t -> startActivity(new Intent(getActivity(), SeeActivity.class)));
        Button listen = root.findViewById(R.id.buttonToListen);
        listen.setOnClickListener(t -> startActivity(new Intent(getActivity(), ListenActivity.class)));
        return root;

    }
}