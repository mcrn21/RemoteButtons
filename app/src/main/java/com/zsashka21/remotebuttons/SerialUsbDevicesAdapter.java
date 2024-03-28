package com.zsashka21.remotebuttons;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SerialUsbDevicesAdapter extends RecyclerView.Adapter<SerialUsbDevicesAdapter.ViewHolder> {
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text1;

        public ViewHolder(View view) {
            super(view);
            text1 = (TextView) view.findViewById(R.id.text1);
        }

        public TextView getText1() {
            return text1;
        }
    }

    public interface OnClickListener {
        void onClick(int position, SerialUsbDevice usbDevice);
    }

    ArrayList<SerialUsbDevice> mSerialUsbDevices;
    private OnClickListener mOnClickListener;

    public SerialUsbDevicesAdapter(ArrayList<SerialUsbDevice> serialUsbDevices) {
        mSerialUsbDevices = serialUsbDevices;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_serial_usb_device, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, @SuppressLint("RecyclerView") int position) {
        SerialUsbDevice serialUsbDevice = mSerialUsbDevices.get(position);

        String str = serialUsbDevice.toString();
        SpannableString spStr = new SpannableString(str);
        spStr.setSpan(new StyleSpan(Typeface.BOLD), 0, str.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        viewHolder.getText1().setText(spStr);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnClickListener != null)
                    mOnClickListener.onClick(position, serialUsbDevice);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSerialUsbDevices.size();
    }
}
