package com.begletsov.parus.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.data.models.Chat;
import com.begletsov.parus.viewmodels.repositories.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository = new ChatRepository();

    public ChatViewModel() { super(); }

    public LiveData<List<Chat>> getMessageData(){
        return repository.getMessageData();
    }

    public void setLinkUser(String linkUserId, boolean isSupport){
        repository.setLinkUserId(linkUserId, isSupport);
    }

    public void sendMessage(String message, boolean fromSupport){
        repository.sendMessage(message, fromSupport);
    }
}
