package com.mcrn21.remotebuttons;

import android.app.Dialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;


public class AboutDialogFragment extends DialogFragment {
    AboutDialogFragment() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());

        View view = getLayoutInflater().inflate(R.layout.dialog_about, null);
        TextView aboutTextTextView = (TextView) view.findViewById(R.id.about_text_text_view);
        aboutTextTextView.setMovementMethod(LinkMovementMethod.getInstance());
        dialogBuilder.setView(view);

        dialogBuilder.setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());

        return dialogBuilder.create();
    }

    public static String TAG = "AboutDialog";
}
