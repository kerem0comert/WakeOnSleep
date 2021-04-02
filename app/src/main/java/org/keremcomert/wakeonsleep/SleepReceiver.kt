package org.keremcomert.wakeonsleep

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class SleepReceiver() : BroadcastReceiver() {

    private var counter = 0

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SEND", "REC IS CALLED")

        if (SleepClassifyEvent.hasEvents(intent)) {
            val sleepClassifyEvents: List<SleepClassifyEvent> =
                SleepClassifyEvent.extractEvents(intent)
            Log.d("SEND", "My confidence: ${sleepClassifyEvents[0].confidence}")
            if(counter > 3) sendWOLSignal(context!!)
            else if(sleepClassifyEvents[0].confidence > 85) counter++
        } else if (SleepSegmentEvent.hasEvents(intent)) {
            val sleepSegmentEvents: List<SleepSegmentEvent> =
                SleepSegmentEvent.extractEvents(intent)
            Log.d("SEND", "SleepSegmentEvent List: $sleepSegmentEvents")
        }
    }

    private fun sendWOLSignal(ctx: Context) {
        Log.d("SEND", "SENDING WOL IN GLOBAL")
        GlobalScope.launch(Dispatchers.IO) {
            val socket = DatagramSocket(MainActivity.PORT)
            var hexData = "ffffffffffff"
            for (x in 0..15) {
                hexData += MainActivity.MAC
            }
            val data: ByteArray = BigInteger(hexData, 16).toByteArray()
            val packet = DatagramPacket(data, data.size, InetAddress.getByName(MainActivity.IP), 9)
            Log.d(
                "SEND", "Data: $data, data.size: ${data.size}, Inet:  ${
                    InetAddress.getByName(
                        MainActivity.IP
                    )
                }"
            )
            socket.broadcast = true
            socket.send(packet)
            socket.close()
            startMainActivityToLockScreen(ctx)
        }
        //wolJob.complete()
    }

    private fun startMainActivityToLockScreen(ctx: Context){
        val bundle = Bundle()
        bundle.putBoolean(MainActivity.LOCK_SCREEN, true)
        val intent = Intent()
        intent.setClassName(ctx, "org.keremcomert.wakeonsleep.MainActivity")
        intent.putExtras(bundle)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ctx.startActivity(intent)
    }

    fun createSleepReceiverPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SleepReceiver::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

    }
}