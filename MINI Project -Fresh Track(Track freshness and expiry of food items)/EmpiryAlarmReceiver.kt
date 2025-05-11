package com.example.fridgemanager1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import java.text.SimpleDateFormat
import java.util.*

class ExpiryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val dbHelper = FridgeDatabaseHelper(context)
        val itemList = dbHelper.getAllItems()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val expiringItems = itemList.filter {
            try {
                val expiryDate = sdf.parse(it.expiryDate)
                val expiryCal = Calendar.getInstance().apply {
                    time = expiryDate!!
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diff = expiryCal.timeInMillis - today.timeInMillis
                val daysLeft = diff / (1000 * 60 * 60 * 24)
                daysLeft in 0..1
            } catch (e: Exception) {
                false
            }
        }

        if (expiringItems.isNotEmpty()) {
            val message = expiringItems.joinToString("\n") { "${it.name} expires on ${it.expiryDate}" }

            val phone = "5554" // Default emulator number
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phone, null, message, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
