package com.mcrn21.remotebuttons;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Arrays;
import java.util.List;

public class SettingsSerialUsbDeviceDialogFragment extends DialogFragment {
    MainActivity mMainActivity = null;

    SettingsSerialUsbDeviceDialogFragment(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        dialogBuilder.setTitle(R.string.connection_settings);

        View view = getLayoutInflater().inflate(R.layout.dialog_connection_settings, null);
        dialogBuilder.setView(view);

        Settings.Connection conn = Settings.readConnectionSettings(requireContext());

        // Baud rate
        List<String> baudRateList = Arrays.asList(getResources().getStringArray(R.array.baud_rate_list));
        ArrayAdapter baudRateArrayAdapter = new ArrayAdapter(requireContext(), R.layout.item_connection_settings_dropdown, baudRateList);
        MaterialAutoCompleteTextView baudRateTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.baud_rate_text_view);
        baudRateTextView.setAdapter(baudRateArrayAdapter);
        baudRateTextView.setInputType(0);
        int baudRateIndex = baudRateList.indexOf(String.valueOf(conn.baudRate));
        baudRateTextView.setText((String) baudRateArrayAdapter.getItem(baudRateIndex), false);

        // Data bits
        List<String> dataBitsList = Arrays.asList(getResources().getStringArray(R.array.data_bits_list));
        ArrayAdapter dataBitsArrayAdapter = new ArrayAdapter(requireContext(), R.layout.item_connection_settings_dropdown, dataBitsList);
        MaterialAutoCompleteTextView dataBitsTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.data_bits_text_view);
        dataBitsTextView.setAdapter(dataBitsArrayAdapter);
        dataBitsTextView.setInputType(0);
        int dataBitsIndex = dataBitsList.indexOf(String.valueOf(conn.dataBits));
        dataBitsTextView.setText((String) dataBitsArrayAdapter.getItem(dataBitsIndex), false);

        // Parity
        List<String> parityList = Arrays.asList(getResources().getStringArray(R.array.parity_list));
        ArrayAdapter parityArrayAdapter = new ArrayAdapter(requireContext(), R.layout.item_connection_settings_dropdown, parityList);
        MaterialAutoCompleteTextView parityTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.parity_text_view);
        parityTextView.setAdapter(parityArrayAdapter);
        parityTextView.setInputType(0);
        parityTextView.setText((String) parityArrayAdapter.getItem(conn.parity), false);

        // Stop bits
        List<String> stopBitsList = Arrays.asList(getResources().getStringArray(R.array.stop_bits_list));
        ArrayAdapter stopBitsArrayAdapter = new ArrayAdapter(requireContext(), R.layout.item_connection_settings_dropdown, stopBitsList);
        MaterialAutoCompleteTextView stopBitsTextView = (MaterialAutoCompleteTextView) view.findViewById(R.id.stop_bits_text_view);
        stopBitsTextView.setAdapter(stopBitsArrayAdapter);
        stopBitsTextView.setInputType(0);
        stopBitsTextView.setText((String) stopBitsArrayAdapter.getItem(conn.stopBits - 1), false);

        dialogBuilder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.Connection conn = new Settings.Connection();
                conn.baudRate = Integer.parseInt(baudRateTextView.getText().toString());
                conn.dataBits = Integer.parseInt(dataBitsTextView.getText().toString());
                conn.parity = parityList.indexOf(parityTextView.getText().toString());
                conn.stopBits = stopBitsList.indexOf(stopBitsTextView.getText().toString()) + 1;
                Settings.writeConnectionSettings(conn, requireContext());
                mMainActivity.sendStartSerial();
                dialog.dismiss();
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return dialogBuilder.create();
    }

    public static String TAG = "SettingsSerialUsbDeviceDialog";
}
