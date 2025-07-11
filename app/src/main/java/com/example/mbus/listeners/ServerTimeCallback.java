package com.example.mbus.listeners;

import java.util.Date;

public interface ServerTimeCallback {
    void onTimeReceived(Date serverDate);
    void onError(Exception e);
}
