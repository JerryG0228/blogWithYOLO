package com.example.my_blog_app;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ImageInfoDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_URI = "imageUri";

    public static ImageInfoDialogFragment newInstance(Uri imageUri) {
        ImageInfoDialogFragment fragment = new ImageInfoDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image_info, container, false);

        ImageView ivImage = view.findViewById(R.id.ivImage);

        if (getArguments() != null) {
            Uri imageUri = getArguments().getParcelable(ARG_IMAGE_URI);

            ivImage.setImageURI(imageUri);
        }

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }
}
