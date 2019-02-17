package com.tistory.overimagine.voltecalllogfix.Util;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Horyeong Park on 2017-06-11.
 */

public class PhoneUtil {
    private static final String TAG = "PhoneUtil";

    private List<SubscriptionInfo> subInfoList;

    private ArrayList<String> PhoneNumbers;
    private ArrayList<String> SIMSlots;

    private boolean Sim1Enabled = false;
    private boolean Sim2Enabled = false;

    public PhoneUtil(Context mContext) {
//        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(TELEPHONY_SERVICE);
        subInfoList = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();

        SIMSlots = new ArrayList<>();
        PhoneNumbers = new ArrayList<>();

        for (SubscriptionInfo subscriptionInfo : subInfoList) {
            Log.i(TAG, "SIM Slot: " + (subscriptionInfo.getSimSlotIndex() + 1));
            Log.i(TAG, "SIM Slot: " + subscriptionInfo.getNumber());

            if (subscriptionInfo.getSimSlotIndex() == 0)
                Sim1Enabled = true;
            else
                Sim2Enabled = true;

            SIMSlots.add("SIM " + (subscriptionInfo.getSimSlotIndex() + 1));

            if (subscriptionInfo.getNumber().length() > 11) {
                PhoneNumbers.add("010" + subscriptionInfo.getNumber().substring(subscriptionInfo.getNumber().length() - 8, subscriptionInfo.getNumber().length()));
            } else {
                PhoneNumbers.add(subscriptionInfo.getNumber());
            }
        }
    }

    public boolean isMultiSimEnabled() {
        return subInfoList.size() > 1;
    }

    private boolean isSIM1Enabled() {
        return Sim1Enabled;
    }

    private boolean isSIM2Enabled() {
        return Sim2Enabled;
    }

    public String getSIM1Number() {
        if (isSIM1Enabled())
            return PhoneNumbers.get(0);

        else return null;
    }

    public String getSIM2Number() {
        if (isSIM2Enabled())
            if (isSIM1Enabled())
                return PhoneNumbers.get(1);
            else
                return PhoneNumbers.get(0);

        else return null;
    }

    private String getSIM1Slot() {
        if (isSIM1Enabled())
            return SIMSlots.get(0) + "  -  " + PhoneNumbers.get(0);

        else return null;
    }

    private String getSIM2Slot() {
        if (isSIM2Enabled())
            if (isSIM1Enabled())
                return SIMSlots.get(1) + "  -  " + PhoneNumbers.get(1);
            else
                return SIMSlots.get(0) + "  -  " + PhoneNumbers.get(0);

        else return null;
    }

    public String getSimSlotStatus(boolean Line) {
        if (isMultiSimEnabled())
            if (Line) return getSIM1Slot() + "\n" + getSIM2Slot();
            else return getSIM1Slot() + ",    " + getSIM2Slot();
        else {
            if (isSIM1Enabled())
                return getSIM1Slot();
            else
                return getSIM2Slot();
        }
    }

    public String getLine1Number() {
        return PhoneNumbers.get(0);
    }
}
