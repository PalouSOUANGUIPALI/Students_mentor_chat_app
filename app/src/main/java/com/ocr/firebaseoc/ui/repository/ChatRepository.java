package com.ocr.firebaseoc.ui.repository;

import android.net.Uri;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ocr.firebaseoc.ui.manager.UserManager;
import com.ocr.firebaseoc.ui.models.Message;

import java.util.UUID;

public final class ChatRepository {

    private static final String CHAT_COLLECTION = "chats";
    private static final String MESSAGE_COLLECTION = "messages";
    private static volatile ChatRepository instance;

    private UserManager userManager;

    private ChatRepository() { this.userManager = UserManager.getInstance(); }


    public static ChatRepository getInstance() {
        ChatRepository result = instance;
        if (result != null) {
            return result;
        }
        synchronized(ChatRepository.class) {
            if (instance == null) {
                instance = new ChatRepository();
            }
            return instance;
        }
    }

    public CollectionReference getChatCollection(){
        return FirebaseFirestore.getInstance().collection(CHAT_COLLECTION);
    }

    public Query getAllMessageForChat(String chat){
        return this.getChatCollection()
                .document(chat)
                .collection(MESSAGE_COLLECTION)
                .orderBy("dateCreated")
                .limit(50);
    }

    public void createMessageForChat(String textMessage, String chat){

        userManager.getUserData().addOnSuccessListener(user -> {
            // Create the Message object
            Message message = new Message(textMessage, user);

            // Store Message to Firestore
            this.getChatCollection()
                    .document(chat)
                    .collection(MESSAGE_COLLECTION)
                    .add(message);
        });

    }

    //Send an image to firestore through it's uri
    public UploadTask uploadImage(Uri imageUri, String chat){
        String uuid = UUID.randomUUID().toString(); // GENERATE UNIQUE STRING
        StorageReference mImageRef = FirebaseStorage.getInstance().getReference(chat + "/" + uuid);
        return mImageRef.putFile(imageUri);
    }

    // Send image in fireBase and return ilageUri
    public void createMessageWithImageForChat(String urlImage, String textMessage, String chat){
        userManager.getUserData().addOnSuccessListener(user -> {
            // Creating Message with the URL image
            Message message = new Message(textMessage, urlImage, user);

            // Storing Message on Firestore
            this.getChatCollection()
                    .document(chat)
                    .collection(MESSAGE_COLLECTION)
                    .add(message);

        });
    }


}
