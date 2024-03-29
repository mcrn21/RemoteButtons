package com.mcrn21.remotebuttons;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.text.method.LinkMovementMethod;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        initCommandsTextView();
        initBroadcastReceiver();

        updateCurrentSerialUsbDeviceLabel(Settings.readDeviceId(this));
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
                showSelectSerialUsbDeviceDialog();
            }
        });

        BottomAppBar bottomAppBar = (BottomAppBar) findViewById(R.id.bottom_app_bar);
        bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.remove_device_item) {
                    Settings.writeDeviceId(-1, MainActivity.this);
                    updateCurrentSerialUsbDeviceLabel(-1);
                    sendStartSerial();
                    return true;
                } else if (id == R.id.clear_log_item) {
                    mCommandsHistory.clear();
                    updateCommandsHistoryLabel();
                    return true;
                } else if (id == R.id.settings_item) {
                    showSettingsDialog();
                    return true;
                }

                return false;
            }
        });
    }

    private void initCommandsTextView() {
        TextView commandsTextView = (TextView) findViewById(R.id.commands_history_text_view);
        commandsTextView.setMovementMethod(new ScrollingMovementMethod());
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
                    updateCurrentSerialUsbDeviceLabel(state ? Settings.readDeviceId(MainActivity.this) : -1);
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
    }

    private void sendStartSerial() {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_START_SERIAL);
        sendBroadcast(intent);
    }

    private void showSelectSerialUsbDeviceDialog() {
        SerialUsbDevicesAdapter adapter = new SerialUsbDevicesAdapter(SerialUsbDevice.getSerialUsbDevices(getApplicationContext()));

        View view = getLayoutInflater().inflate(R.layout.dialog_serial_usb_devices, null);
        RecyclerView usbDevicesView = (RecyclerView) view.findViewById(R.id.serial_usb_devices_view);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(R.string.select_device_label);
        usbDevicesView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        usbDevicesView.setAdapter(adapter);
        dialogBuilder.setView(view);

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = dialogBuilder.show();

        adapter.setOnClickListener(new SerialUsbDevicesAdapter.OnClickListener() {
            @Override
            public void onClick(int position, SerialUsbDevice usbDevice) {
                int deviceId = usbDevice.getDeviceId();
                Settings.writeDeviceId(deviceId, MainActivity.this);
                updateCurrentSerialUsbDeviceLabel(deviceId);
                sendStartSerial();
                dialog.dismiss();
            }
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_connection_settings, null);

        Settings.Connection conn = Settings.readConnectionSettings(MainActivity.this);

        // Baud rate
        List<String> baudRateList = Arrays.asList(getResources().getStringArray(R.array.baud_rate_list));
        ArrayAdapter baudRateArrayAdapter = new ArrayAdapter(this, R.layout.item_connection_settings_dropdown, baudRateList);
        MaterialAutoCompleteTextView baudRateTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.baud_rate_text_view);
        baudRateTextView.setAdapter(baudRateArrayAdapter);
        baudRateTextView.setInputType(0);
        int baudRateIndex = baudRateList.indexOf(String.valueOf(conn.baudRate));
        baudRateTextView.setText((String) baudRateArrayAdapter.getItem(baudRateIndex), false);

        // Data bits
        List<String> dataBitsList = Arrays.asList(getResources().getStringArray(R.array.data_bits_list));
        ArrayAdapter dataBitsArrayAdapter = new ArrayAdapter(this, R.layout.item_connection_settings_dropdown, dataBitsList);
        MaterialAutoCompleteTextView dataBitsTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.data_bits_text_view);
        dataBitsTextView.setAdapter(dataBitsArrayAdapter);
        dataBitsTextView.setInputType(0);
        int dataBitsIndex = dataBitsList.indexOf(String.valueOf(conn.dataBits));
        dataBitsTextView.setText((String) dataBitsArrayAdapter.getItem(dataBitsIndex), false);

        // Parity
        List<String> parityList = Arrays.asList(getResources().getStringArray(R.array.parity_list));
        ArrayAdapter parityArrayAdapter = new ArrayAdapter(this, R.layout.item_connection_settings_dropdown, parityList);
        MaterialAutoCompleteTextView parityTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.parity_text_view);
        parityTextView.setAdapter(parityArrayAdapter);
        parityTextView.setInputType(0);
        parityTextView.setText((String) parityArrayAdapter.getItem(conn.parity), false);

        // Stop bits
        List<String> stopBitsList = Arrays.asList(getResources().getStringArray(R.array.stop_bits_list));
        ArrayAdapter stopBitsArrayAdapter = new ArrayAdapter(this, R.layout.item_connection_settings_dropdown, stopBitsList);
        MaterialAutoCompleteTextView stopBitsTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.stop_bits_text_view);
        stopBitsTextView.setAdapter(stopBitsArrayAdapter);
        stopBitsTextView.setInputType(0);
        stopBitsTextView.setText((String) stopBitsArrayAdapter.getItem(conn.stopBits - 1), false);

        dialogBuilder.setTitle(R.string.connection_settings);
        dialogBuilder.setView(view);
        dialogBuilder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.Connection conn = new Settings.Connection();
                conn.baudRate = Integer.parseInt(baudRateTextView.getText().toString());
                conn.dataBits = Integer.parseInt(dataBitsTextView.getText().toString());
                conn.parity = parityList.indexOf(parityTextView.getText().toString());
                conn.stopBits = stopBitsList.indexOf(stopBitsTextView.getText().toString()) + 1;
                Settings.writeConnectionSettings(conn, MainActivity.this);
                sendStartSerial();
                dialog.dismiss();
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogBuilder.show();
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