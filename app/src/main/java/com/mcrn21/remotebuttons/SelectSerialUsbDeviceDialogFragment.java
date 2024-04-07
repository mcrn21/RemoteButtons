package com.mcrn21.remotebuttons;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class SelectSerialUsbDeviceDialogFragment extends DialogFragment {
    MainActivity mMainActivity = null;

    SelectSerialUsbDeviceDialogFragment(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        dialogBuilder.setTitle(R.string.select_device_label);

        ArrayAdapter<SerialUsbDevice> adapter = new ArrayAdapter(requireContext(), 0, SerialUsbDevice.getSerialUsbDevices(requireContext())) {
            @NonNull
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                if(view == null)
                    view = LayoutInflater.from(requireContext()).inflate(R.layout.item_serial_usb_device,parent,false);

                SerialUsbDevice serialUsbDevice = (SerialUsbDevice) getItem(position);
                String str = serialUsbDevice.toString();
                SpannableString spStr = new SpannableString(str);
                spStr.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                TextView text1 = (TextView) view.findViewById(R.id.text1);
                text1.setText(spStr);

                return view;
            }
        };

        dialogBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SerialUsbDevice serialUsbDevice = (SerialUsbDevice) adapter.getItem(which);
                if (serialUsbDevice != null) {
                    int deviceId = serialUsbDevice.getDeviceId();
                    Settings.writeDeviceId(deviceId, requireContext());
                    mMainActivity.updateCurrentSerialUsbDeviceLabel(deviceId);
                    mMainActivity.sendStartSerial();
                }
                dialog.dismiss();
            }
        });

        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return  dialogBuilder.create();
    }

    public static String TAG = "SelectSerialUsbDeviceDialog";
}
