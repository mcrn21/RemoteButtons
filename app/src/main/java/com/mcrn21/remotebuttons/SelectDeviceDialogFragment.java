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

public class SelectDeviceDialogFragment extends DialogFragment {
    SelectDeviceDialogFragment() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        dialogBuilder.setTitle(R.string.select_device_label);

        ArrayAdapter<Device> adapter = new ArrayAdapter(requireContext(), 0, Device.getDevices(requireContext())) {
            @NonNull
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                if(view == null)
                    view = LayoutInflater.from(requireContext()).inflate(R.layout.item_device,parent,false);

                Device device = (Device) getItem(position);
                assert device != null;
                String str = device.description;
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
                Device device = (Device) adapter.getItem(which);
                if (device != null) {
                    Bundle result = new Bundle();
                    result.putParcelable("device", device);
                    getParentFragmentManager().setFragmentResult("deviceSelected", result);
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
