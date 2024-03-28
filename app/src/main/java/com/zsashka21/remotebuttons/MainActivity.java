package com.zsashka21.remotebuttons;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

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
        initSelectSerialUsbDeviceButton();
        initRemoveButton();
        initCommandsTextView();
        initBroadcastReceiver();
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

    private void initSelectSerialUsbDeviceButton() {
        final Button selectButton = (Button) findViewById(R.id.select_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SerialUsbDevicesAdapter adapter = new SerialUsbDevicesAdapter(SerialUsbDevice.getSerialUsbDevices(getApplicationContext()));

                View view = getLayoutInflater().inflate(R.layout.dialog_serial_usb_devices, null);
                RecyclerView usbDevicesView = (RecyclerView) view.findViewById(R.id.serial_usb_devices_view);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setTitle(R.string.select_device_label);
                usbDevicesView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                usbDevicesView.setAdapter(adapter);
                dialogBuilder.setView(view);

                dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = dialogBuilder.show();

                adapter.setOnClickListener(new SerialUsbDevicesAdapter.OnClickListener() {
                    @Override
                    public void onClick(int position, SerialUsbDevice usbDevice) {
                        sendApplyServiceDeviceId(usbDevice.getDeviceId());
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private void initRemoveButton() {
        final Button removeButton = (Button) findViewById(R.id.remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendApplyServiceDeviceId(-1);
            }
        });
    }

    private void initCommandsTextView() {
        TextView commandsTextView = (TextView) findViewById(R.id.commands_history_text_view);
        commandsTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void initBroadcastReceiver() {
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(Common.INTENT_APPLIED_SERVICE_DEVICE_ID);
        broadcastFilter.addAction(Common.INTENT_REMOTE_BUTTONS_COMMAND);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Common.INTENT_APPLIED_SERVICE_DEVICE_ID.equals(intent.getAction())) {
                    int deviceId = intent.getIntExtra("deviceId", -1);
                    updateCurrentSerialUsbDeviceLabel(deviceId);
                } else if (Common.INTENT_REMOTE_BUTTONS_COMMAND.equals(intent.getAction())) {
                    String command = intent.getStringExtra("command");

                    SimpleDateFormat formatter = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss] > ");
                    String currentDateAndTime = formatter.format(new Date());
                    mCommandsHistory.add(currentDateAndTime + command);
                    if (mCommandsHistory.size() > MAX_COMMANDS_HISTORY_SIZE)
                        mCommandsHistory.remove(0);

                    updateCommandsHistoryLabel();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            registerReceiver(mBroadcastReceiver, broadcastFilter, RECEIVER_EXPORTED);
        else
            registerReceiver(mBroadcastReceiver, broadcastFilter);

        Intent serviceIntent = new Intent(this, RemoteButtonsService.class);
        startService(serviceIntent);

        Intent getCurrentDeviceIdIntent = new Intent();
        getCurrentDeviceIdIntent.setAction(Common.INTENT_GET_SERVICE_DEVICE_ID);
        sendBroadcast(getCurrentDeviceIdIntent);
    }

    private void sendApplyServiceDeviceId(int deviceId) {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_APPLY_SERVICE_DEVICE_ID);
        intent.putExtra("deviceId", deviceId);
        sendBroadcast(intent);
    }

    private void updateCurrentSerialUsbDeviceLabel(int deviceId) {
        TextView currentSerialUsbDeviceTextView = (TextView) findViewById(R.id.current_serial_usb_device_text_view);
        SerialUsbDevice serialUsbDevice = SerialUsbDevice.getSerialUsbDevice(deviceId, this);

        if (serialUsbDevice == null) {
            currentSerialUsbDeviceTextView.setText(R.string.no_device);
            return;
        }

        String str = serialUsbDevice.toString();
        SpannableString spStr = new SpannableString(str);
        spStr.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        currentSerialUsbDeviceTextView.setText(spStr);
    }

    private void updateCommandsHistoryLabel() {
        TextView commandsHistoryTextView = (TextView) findViewById(R.id.commands_history_text_view);
        commandsHistoryTextView.setText(String.join("\n", mCommandsHistory));
    }
}