package com.mcrn21.remotebuttons;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainFragment  extends Fragment {
    private BroadcastReceiver mBroadcastReceiver;
    private final ArrayList<String> mCommandsHistory = new ArrayList<String>();
    static final int MAX_COMMANDS_HISTORY_SIZE = 30;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Common.INTENT_SERIAL_CONNECTION_UPDATED.equals(intent.getAction())) {
                    SharedPreferences sharedPref = requireContext().getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
                    Device device = new Gson().fromJson(sharedPref.getString("device", ""), Device.class);
                    updateDeviceTextView(device);
                } else if (Common.INTENT_REMOTE_BUTTONS_COMMAND.equals(intent.getAction())) {
                    String command = intent.getStringExtra("command");

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss] > ");
                    String currentDateAndTime = formatter.format(new Date());
                    mCommandsHistory.add(currentDateAndTime + command);
                    if (mCommandsHistory.size() > MAX_COMMANDS_HISTORY_SIZE)
                        mCommandsHistory.remove(0);

                    updateCommandsHistoryTextView();
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        getParentFragmentManager().setFragmentResultListener("deviceSelected", this, (requestKey, result) -> {
            Device device = result.getParcelable("device");

            SharedPreferences sharedPref = requireContext().getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("device", new Gson().toJson(device));
            editor.apply();

            updateDeviceTextView(device);
            sendStartSerial();
        });

        getParentFragmentManager().setFragmentResultListener("launchAppSelected", this, (requestKey, result) -> {
            String packageName = result.getString("packageName");

            SharedPreferences sharedPref = requireContext().getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("launchAppPackageName", packageName);
            editor.apply();

            updateLaunchAppTextView(packageName);
        });

        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(Common.INTENT_SERIAL_CONNECTION_UPDATED);
        broadcastFilter.addAction(Common.INTENT_REMOTE_BUTTONS_COMMAND);
        ContextCompat.registerReceiver(getContext(), mBroadcastReceiver, broadcastFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void onStop() {
        getContext().unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, viewGroup, false);

        // Bottom bar
        FloatingActionButton addDeviceActionButton = (FloatingActionButton) view.findViewById(R.id.add_device_action_button);
        addDeviceActionButton.setOnClickListener(v -> new SelectDeviceDialogFragment().show(
                getParentFragmentManager(), SelectDeviceDialogFragment.TAG));

        BottomAppBar bottomAppBar = (BottomAppBar) view.findViewById(R.id.bottom_app_bar);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.remove_device_item) {
                Device device = new Device();
                SharedPreferences sharedPref = requireContext().getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("device", new Gson().toJson(device));
                editor.apply();

                updateDeviceTextView(device);
                sendStartSerial();
                return true;
            } else if (id == R.id.settings_device_item) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment, new PreferenceFragment(), "preferences").addToBackStack(null).commit();
                return true;
            }

            return false;
        });

        // Commands history
        TextView commandsHistoryTextView = (TextView) view.findViewById(R.id.commands_history_text_view);
        commandsHistoryTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView clearCommandsHistoryButton = (ImageView) view.findViewById(R.id.clear_commands_history_button);
        clearCommandsHistoryButton.setOnClickListener(v -> {
            mCommandsHistory.clear();
            updateCommandsHistoryTextView();
        });

        // Launch app
        ImageView selectLaunchAppButton = (ImageView) view.findViewById(R.id.select_launch_app_button);
        selectLaunchAppButton.setOnClickListener(v -> {
            new SelectAppDialogFragment().show(
                    getParentFragmentManager(), SelectAppDialogFragment.TAG);
        });

        return view;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPref = requireContext().getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        Device device = new Gson().fromJson(sharedPref.getString("device", ""), Device.class);
        String launchAppPackageName = sharedPref.getString("launchAppPackageName", "");

        updateDeviceTextView(device);
        updateLaunchAppTextView(launchAppPackageName);
    }

    public void sendStartSerial() {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_START_SERIAL);
        getContext().sendBroadcast(intent);
    }

    public void updateDeviceTextView(Device device) {
        TextView deviceTextView = (TextView) getView().findViewById(R.id.device_text_view);

        if (device == null || !device.isValid()) {
            deviceTextView.setText(R.string.no_device);
            return;
        }

        String str = device.description;
        SpannableString spStr = new SpannableString(str);
        spStr.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        deviceTextView.setText(spStr);
    }

    public void updateLaunchAppTextView(String packageName) {
        TextView launchAppNameTextView = (TextView) getView().findViewById(R.id.launch_app_name_text_view);
        ImageView launchAppIconImageView = (ImageView) getView().findViewById(R.id.launch_app_icon_image_view);

        try {
            ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(packageName, 0);
            launchAppNameTextView.setText(appInfo.loadLabel(getContext().getPackageManager()));
            launchAppIconImageView.setImageDrawable(appInfo.loadIcon(getContext().getPackageManager()));
        } catch (PackageManager.NameNotFoundException e) {
            launchAppNameTextView.setText(R.string.no_device);
            launchAppIconImageView.setImageResource(R.drawable.ic_block);
        }
    }

    private void updateCommandsHistoryTextView() {
        TextView commandsHistoryTextView = (TextView) getView().findViewById(R.id.commands_history_text_view);
        commandsHistoryTextView.setText(String.join("\n", mCommandsHistory));
    }
}
