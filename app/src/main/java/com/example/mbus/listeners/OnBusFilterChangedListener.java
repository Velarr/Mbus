package com.example.mbus.listeners;

import com.example.mbus.data.BusInfo;
import java.util.List;

public interface OnBusFilterChangedListener {
    void onBusFilterChanged(List<BusInfo> filteredBuses);
}
