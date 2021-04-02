package org.keremcomert.wakeonsleep

import android.Manifest
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.keremcomert.wakeonsleep.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var b: ActivityMainBinding

    companion object {
        const val MAC = "18c04d35cc5b"
        const val IP = "192.168.1.255"
        const val PORT = 4532
        const val PERM_REQ_CODE = 21
        const val LOCK_SCREEN = "lockScreen"
        const val LOG_TAG = "LogTag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getBooleanExtra(LOCK_SCREEN, false)){
            lockScreen()
            return
        }
        b = ActivityMainBinding.inflate(layoutInflater)
        with(b) {
            setContentView(root)
            bWOL.setOnClickListener {
                lockScreen()
            }
        }

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACTIVITY_RECOGNITION)) {
            Log.d(LOG_TAG, "has perm")
            checkForSleepiness()
        } else {
            Log.d(LOG_TAG, "NO perm")
            askForPermission()
        }
    }

     private fun lockScreen(){
        val adminComponent = ComponentName(this, DAReceiver::class.java)
        val devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            startActivityForResult(intent, 0)
        } else {
            devicePolicyManager.lockNow()
        }

    }


    private fun checkForSleepiness() {
        val task = ActivityRecognition.getClient(this).requestSleepSegmentUpdates(
            SleepReceiver().createSleepReceiverPendingIntent(context = applicationContext),
            SleepSegmentRequest.getDefaultSleepSegmentRequest()
        )

        task.addOnSuccessListener {
            Log.d(LOG_TAG, "Successfully subscribed to sleep data.")
        }
        task.addOnFailureListener { exception ->
            Log.d(LOG_TAG, "Exception when subscribing to sleep data: $exception")
        }
    }

    private fun askForPermission() {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.need_activity_recognition),
            PERM_REQ_CODE,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
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