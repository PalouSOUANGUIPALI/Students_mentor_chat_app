package com.ocr.firebaseoc.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseUser;
import com.ocr.firebaseoc.R;
import com.ocr.firebaseoc.databinding.ActivityProfileBinding;
import com.ocr.firebaseoc.ui.manager.UserManager;

public class ProfileActivity extends BaseActivity<ActivityProfileBinding> {

    private UserManager userManager = UserManager.getInstance();
    @Override
    protected ActivityProfileBinding getViewBinding() {
        return ActivityProfileBinding.inflate(getLayoutInflater());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupListeners();
        updateUIWithUserData();
    }

    private void setupListeners(){
        // Mentor Checkbox
        binding.isMentorCheckBox.setOnCheckedChangeListener((compoundButton, checked) -> {
            userManager.updateIsMentor(checked);
        });

        // Update button
        binding.updateButton.setOnClickListener(view -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            userManager.updateUsername(binding.usernameEditText.getText().toString())
                    .addOnSuccessListener(aVoid -> {
                        binding.progressBar.setVisibility(View.INVISIBLE);
                    });
        });

        // Sign out button
        binding.signOutButton.setOnClickListener(view -> {
            userManager.signOut(this).addOnSuccessListener(aVoid -> {
                finish();
            });
        });

        // Delete button
        binding.deleteButton.setOnClickListener(view -> {

            new AlertDialog.Builder(this)
                    .setMessage(R.string.popup_message_confirmation_delete_account)
                    .setPositiveButton(R.string.popup_message_choice_yes, (dialogInterface, i) ->
                            userManager.deleteUser(ProfileActivity.this)
                                    .addOnSuccessListener(aVoid -> {
                                                finish();
                                            }
                                    )
                    )
                    .setNegativeButton(R.string.popup_message_choice_no, null)
                    .show();

        });
    }



    private void updateUIWithUserData(){
        if(userManager.isCurrentUserLogged()){
            FirebaseUser user = userManager.getCurrentUser();

            if(user.getPhotoUrl() != null){
                setProfilePicture(user.getPhotoUrl());
            }
            setTextUserData(user);
            getUserData();
        }
    }

    private void setProfilePicture(Uri profilePictureUrl){
        Glide.with(this)
                .load(profilePictureUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView);
    }

    private void setTextUserData(FirebaseUser user){

        //Get email & username from User
        String email = TextUtils.isEmpty(user.getEmail()) ?
                getString(R.string.info_no_email_found) : user.getEmail();
        String username = TextUtils.isEmpty(user.getDisplayName()) ?
                getString(R.string.info_no_username_found) : user.getDisplayName();

        //Update views with data
        binding.usernameEditText.setText(username);
        binding.emailTextView.setText(email);
    }

    private void getUserData(){
        userManager.getUserData().addOnSuccessListener(user -> {
            // Set the data with the user information
            String username = TextUtils.isEmpty(user.getUsername()) ? getString(R.string.info_no_username_found) : user.getUsername();
            binding.isMentorCheckBox.setChecked(user.getIsMentor());
            binding.usernameEditText.setText(username);
        });
    }

}

