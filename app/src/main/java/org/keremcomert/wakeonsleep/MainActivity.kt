package org.keremcomert.wakeonsleep

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentRequest
import kotlinx.coroutines.*
import org.keremcomert.wakeonsleep.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions
import java.math.BigInteger
import java.net.*
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, ClassifyReceivedListener {
    private lateinit var b: ActivityMainBinding
    private lateinit var sleepPendingIntent: PendingIntent
    private val wolJob = Job()
    private val sleepJob = Job()
    private val wolScope = CoroutineScope(Dispatchers.Main + wolJob)

    companion object {
        const val MAC = "18c04d35cc5b"
        const val IP = "192.168.1.255"
        const val PORT = 4532
        const val PERM_REQ_CODE = 21
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        with(b) {
            setContentView(root)
            bWOL.setOnClickListener {
                // askForPermission()
                wolScope.launch(Dispatchers.Main) { sendWOLSignal() }
            }
        }

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACTIVITY_RECOGNITION)) {
            Log.d("SEND", "has perm")
            checkForSleepiness()
        } else {
            Log.d("SEND", "NO perm")
            askForPermission()
        }

    }


    private fun checkForSleepiness() {
        sleepPendingIntent = SleepReceiver.createSleepReceiverPendingIntent(context = applicationContext)
        val task = ActivityRecognition.getClient(this).requestSleepSegmentUpdates(
                sleepPendingIntent, SleepSegmentRequest.getDefaultSleepSegmentRequest())

        task.addOnSuccessListener {
            Log.d("SEND", "Successfully subscribed to sleep data.")
        }
        task.addOnFailureListener { exception ->
            Log.d("SEND", "Exception when subscribing to sleep data: $exception")
        }
    }

    override fun onClassifyReceived(confidence: Int) {
       wolScope.launch { sendWOLSignal() }
    }

    private fun askForPermission() {
        Log.d("SEND", "here")
        EasyPermissions.requestPermissions(
                this,
                getString(R.string.need_activity_recognition),
                PERM_REQ_CODE,
                Manifest.permission.ACTIVITY_RECOGNITION
        )
    }


    private suspend fun sendWOLSignal() {
        withContext(Dispatchers.IO) {
            wolJob.start()
            val socket = DatagramSocket(PORT)
            var hexData = "ffffffffffff"
            for (x in 0..15) {
                hexData += MAC
            }
            val data: ByteArray = BigInteger(hexData, 16).toByteArray()
            val packet = DatagramPacket(data, data.size, InetAddress.getByName(IP), 9)
            Log.d("SEND", "Data: $data, data.size: ${data.size}, Inet:  ${InetAddress.getByName(IP)}")
            socket.broadcast = true
            socket.send(packet)
            socket.close()
            //wolJob.complete()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        checkForSleepiness()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) { //askForPermission()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}