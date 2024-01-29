package com.ocr.firebaseoc.ui;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.ocr.firebaseoc.R;
import com.ocr.firebaseoc.databinding.ActivityMentorChatBinding;
import com.ocr.firebaseoc.ui.chat.MentorChatAdapter;
import com.ocr.firebaseoc.ui.manager.ChatManager;
import com.ocr.firebaseoc.ui.manager.UserManager;
import com.ocr.firebaseoc.ui.models.Message;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MentorChatActivity extends BaseActivity<ActivityMentorChatBinding> implements MentorChatAdapter.Listener {

    private MentorChatAdapter mentorChatAdapter;
    private String currentChatName;

    private static final String CHAT_NAME_ANDROID = "android";
    private static final String CHAT_NAME_BUG = "bug";
    private static final String CHAT_NAME_FIREBASE = "firebase";

    private static final String PERMS = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final int RC_IMAGE_PERMS = 100;
    private static final int RC_CHOOSE_PHOTO = 200;
    private Uri uriImageSelected;

    private UserManager userManager = UserManager.getInstance();
    private ChatManager chatManager = ChatManager.getInstance();

    @Override
    protected ActivityMentorChatBinding getViewBinding() {
        return ActivityMentorChatBinding.inflate(getLayoutInflater());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureRecyclerView(CHAT_NAME_ANDROID);
        setupListeners();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.handleResponse(requestCode, resultCode, data);
    }

    private void setupListeners(){
        // Send button
        binding.addFileButton.setOnClickListener(view -> { addFile(); });
        binding.sendButton.setOnClickListener(view -> { sendMessage(); });

        // Chat buttons
        binding.androidChatButton.setOnClickListener(view -> {
            this.configureRecyclerView(CHAT_NAME_ANDROID); });
        binding.firebaseChatButton.setOnClickListener(view -> {
            this.configureRecyclerView(CHAT_NAME_FIREBASE); });
        binding.bugChatButton.setOnClickListener(view -> {
            this.configureRecyclerView(CHAT_NAME_BUG); });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions,
                grantResults, this);
    }

    @AfterPermissionGranted(RC_IMAGE_PERMS)
    private void addFile(){
        if (!EasyPermissions.hasPermissions(this, PERMS)) {
            EasyPermissions.requestPermissions(this,
                    getString(
                    R.string.popup_title_permission_files_access),
                    RC_IMAGE_PERMS, PERMS);
            return;
        }
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RC_CHOOSE_PHOTO);
    }

    // Handle activity response (after user has chosen or not a picture)
    private void handleResponse(int requestCode, int resultCode, Intent data){
        if (requestCode == RC_CHOOSE_PHOTO) {
            if (resultCode == RESULT_OK) { //SUCCESS
                this.uriImageSelected = data.getData();
                Glide.with(this) //SHOWING PREVIEW OF IMAGE
                        .load(this.uriImageSelected)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.imagePreview);
            } else {
                Toast.makeText(this, getString(R.string.toast_title_no_image_chosen), Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Configure RecyclerView
    private void configureRecyclerView(String chatName){
        //Track current chat name
        this.currentChatName = chatName;
        //Configure Adapter & RecyclerView
        this.mentorChatAdapter = new MentorChatAdapter(
                generateOptionsForAdapter(chatManager.getAllMessageForChat(this.currentChatName)),
                Glide.with(this), this);

        mentorChatAdapter.registerAdapterDataObserver(
                new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        // Scroll to bottom on new messages
                        binding.chatRecyclerView.smoothScrollToPosition(mentorChatAdapter.getItemCount());
                    }
                });

        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.chatRecyclerView.setAdapter(this.mentorChatAdapter);
    }

    // Create options for RecyclerView from a Query
    private FirestoreRecyclerOptions<Message> generateOptionsForAdapter(Query query){
        return new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .setLifecycleOwner(this)
                .build();
    }

    public void onDataChanged() {
        // Show TextView in case RecyclerView is empty
        binding.emptyRecyclerView.setVisibility(
                this.mentorChatAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void sendMessage(){
        // Check if user can send a message (Text not null + user logged)
        boolean canSendMessage = !TextUtils.isEmpty(binding.chatEditText.getText()) && userManager.isCurrentUserLogged();

        if (canSendMessage){
            String messageText = binding.chatEditText.getText().toString();
            // Check if there is an image to add with the message
            if(binding.imagePreview.getDrawable() == null){
                // Create a new message for the chat
                chatManager.createMessageForChat(messageText, this.currentChatName);
            }else {
                // Create a new message with an image for the chat
                chatManager.sendMessageWithImageForChat(messageText, this.uriImageSelected, this.currentChatName);
                binding.imagePreview.setImageDrawable(null);
            }
            // Reset text field
            binding.chatEditText.setText("");

        }
    }

}