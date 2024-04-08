package com.mcrn21.remotebuttons;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

public class SelectAppDialogFragment extends DialogFragment {
    MainActivity mMainActivity = null;

    SelectAppDialogFragment(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        dialogBuilder.setTitle(R.string.select_app);

        @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> packList =
                requireContext().getPackageManager().getInstalledApplications(0);

        ArrayAdapter<ApplicationInfo> adapter = new ArrayAdapter(requireContext(), 0, packList) {
            @NonNull
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                if(view == null)
                    view = LayoutInflater.from(requireContext()).inflate(R.layout.item_select_app,parent,false);

                ApplicationInfo appInfo = (ApplicationInfo) getItem(position);
                if (appInfo != null) {
                    TextView text1 = (TextView) view.findViewById(R.id.text1);
                    text1.setText(appInfo.loadLabel(requireContext().getPackageManager()).toString());
                    ImageView imageView1 = (ImageView) view.findViewById(R.id.image_view_1);
                    imageView1.setImageDrawable(appInfo.loadIcon(requireContext().getPackageManager()));
                }

                return view;
            }
        };

        dialogBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ApplicationInfo appInfo = (ApplicationInfo) adapter.getItem(which);
                if (appInfo != null) {
                    Settings.getInstance().launchAppPackageName = appInfo.packageName;
                    mMainActivity.updateLaunchAppLabel(appInfo.packageName);
                    dialog.dismiss();
                }
            }
        });

        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return dialogBuilder.create();
    }

    public static String TAG = "SelectAppDialog";
}
