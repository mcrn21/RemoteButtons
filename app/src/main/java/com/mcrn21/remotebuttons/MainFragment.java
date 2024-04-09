package com.mcrn21.remotebuttons;

import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class MainFragment  extends Fragment {
    private BroadcastReceiver mBroadcastReceiver;
    private final ArrayList<String> mCommandsHistory = new ArrayList<String>();
    static final int MAX_COMMANDS_HISTORY_SIZE = 30;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, viewGroup, false);



        return view;
    }
}
