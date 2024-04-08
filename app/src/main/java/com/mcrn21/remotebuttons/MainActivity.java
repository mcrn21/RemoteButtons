package com.mcrn21.remotebuttons;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.text.method.LinkMovementMethod;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver mBroadcastReceiver;
    private final ArrayList<String> mCommandsHistory = new ArrayList<String>();
    static final int MAX_COMMANDS_HISTORY_SIZE = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAppBar();
        initBottomAppBar();
        initCommandsHistory();
        initSelectLaunchApp();
        initBroadcastReceiver();

        Intent serviceIntent = new Intent(this, RemoteButtonsService.class);
        startForegroundService(serviceIntent);

        updateCurrentSerialUsbDeviceLabel(Settings.readDeviceInfo(this));
        updateLaunchAppLabel(Settings.readLaunchApp(this));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void initAppBar() {
        MaterialToolbar topAppBar = (MaterialToolbar) findViewById(R.id.top_app_bar);

        topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.about_item) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_about, null);

                    TextView aboutTextTextView = (TextView) view.findViewById(R.id.about_text_text_view);
                    aboutTextTextView.setMovementMethod(LinkMovementMethod.getInstance());

                    dialogBuilder.setView(view);
                    dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialogBuilder.show();
                    return true;
                }
                return false;
            }
        });
    }

    private void initBottomAppBar() {
        FloatingActionButton addDeviceActionButton = (FloatingActionButton) findViewById(R.id.add_device_action_button);
        addDeviceActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SelectSerialUsbDeviceDialogFragment(MainActivity.this).show(
                        getSupportFragmentManager(), SelectSerialUsbDeviceDialogFragment.TAG);
            }
        });

        BottomAppBar bottomAppBar = (BottomAppBar) findViewById(R.id.bottom_app_bar);
        bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.remove_device_item) {
                    Settings.writeDeviceInfo(null, MainActivity.this);
                    updateCurrentSerialUsbDeviceLabel(null);
                    sendStartSerial();
                    return true;
                } else if (id == R.id.settings_device_item) {
                    new SettingsSerialUsbDeviceDialogFragment(MainActivity.this).show(
                            getSupportFragmentManager(), SettingsSerialUsbDeviceDialogFragment.TAG);
                    return true;
                }

                return false;
            }
        });
    }

    private void initCommandsHistory() {
        TextView commandsHistoryTextView = (TextView) findViewById(R.id.commands_history_text_view);
        commandsHistoryTextView.setMovementMethod(new ScrollingMovementMethod());

        ImageView clearCommandsHistoryButton = (ImageView) findViewById(R.id.clear_commands_history_button);
        clearCommandsHistoryButton.setOnClickListener(view -> {
            mCommandsHistory.clear();
            updateCommandsHistoryLabel();
        });
    }

    private void initSelectLaunchApp() {
        Switch launchAppSwitch = (Switch) findViewById(R.id.launch_app_switch);
        launchAppSwitch.setChecked(Settings.readLaunchApp(this).enable);

        launchAppSwitch.setOnClickListener(view -> {
            LaunchApp launchApp = Settings.readLaunchApp(this);
            launchApp.enable = launchAppSwitch.isChecked();
            Settings.writeLaunchApp(launchApp, this);
        });

        ImageView selectLaunchAppButton = (ImageView) findViewById(R.id.select_launch_app_button);
        selectLaunchAppButton.setOnClickListener(view -> {
            new SelectAppDialogFragment(MainActivity.this).show(
                    getSupportFragmentManager(), SelectAppDialogFragment.TAG);
        });
    }

    private void initBroadcastReceiver() {
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(Common.INTENT_SERIAL_CONNECTION_UPDATED);
        broadcastFilter.addAction(Common.INTENT_REMOTE_BUTTONS_COMMAND);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Common.INTENT_SERIAL_CONNECTION_UPDATED.equals(intent.getAction())) {
                    boolean state = intent.getBooleanExtra("state", false);
                    updateCurrentSerialUsbDeviceLabel(state ? Settings.readDeviceInfo(MainActivity.this) : null);
                } else if (Common.INTENT_REMOTE_BUTTONS_COMMAND.equals(intent.getAction())) {
                    String command = intent.getStringExtra("command");

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss] > ");
                    String currentDateAndTime = formatter.format(new Date());
                    mCommandsHistory.add(currentDateAndTime + command);
                    if (mCommandsHistory.size() > MAX_COMMANDS_HISTORY_SIZE)
                        mCommandsHistory.remove(0);

                    updateCommandsHistoryLabel();
                }
            }
        };

        registerReceiver(mBroadcastReceiver, broadcastFilter, RECEIVER_EXPORTED);
    }

    public void sendStartSerial() {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_START_SERIAL);
        sendBroadcast(intent);
    }

    public void updateCurrentSerialUsbDeviceLabel(SerialUsbDevice.Info info) {
        TextView currentSerialUsbDeviceTextView = (TextView) findViewById(R.id.current_serial_usb_device_text_view);
        SerialUsbDevice serialUsbDevice = SerialUsbDevice.getSerialUsbDevice(info, this);

        if (serialUsbDevice == null) {
            currentSerialUsbDeviceTextView.setText(R.string.no_device);
            return;
        }

        String str = serialUsbDevice.toString();
        SpannableString spStr = new SpannableString(str);
        spStr.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        currentSerialUsbDeviceTextView.setText(spStr);
    }

    public void updateLaunchAppLabel(LaunchApp launchApp) {
        TextView launchAppNameTextView = (TextView) findViewById(R.id.launch_app_name_text_view);
        ImageView launchAppIconImageView = (ImageView) findViewById(R.id.launch_app_icon_image_view);

        if (launchApp.packageName.isEmpty()) {
            launchAppNameTextView.setText(R.string.no_device);
            launchAppIconImageView.setImageResource(R.drawable.ic_block);
            return;
        }

        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(launchApp.packageName, 0);
            launchAppNameTextView.setText(appInfo.loadLabel(getPackageManager()));
            launchAppIconImageView.setImageDrawable(appInfo.loadIcon(getPackageManager()));
        } catch (PackageManager.NameNotFoundException e) { }
    }

    private void updateCommandsHistoryLabel() {
        TextView commandsHistoryTextView = (TextView) findViewById(R.id.commands_history_text_view);
        commandsHistoryTextView.setText(String.join("\n", mCommandsHistory));
    }
}