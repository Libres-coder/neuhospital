package com.example.neusoft_hospital.core.util

import android.util.Log

object SmsSimulator {
    private const val TAG = "SmsSimulator"
    const val FIXED_DEV_CODE = "123456"

    fun generateCode(phone: String): String {
        try { Log.d(TAG, "[SMS Mock] Sending SMS to $phone — code: $FIXED_DEV_CODE (use this to verify)") } catch (_: Throwable) {}
        return FIXED_DEV_CODE
    }

    fun verify(phone: String, input: String): Boolean {
        val ok = input == FIXED_DEV_CODE
        try { Log.d(TAG, "[SMS Mock] Verify $phone — input=$input, result=$ok") } catch (_: Throwable) {}
        return ok
    }
}