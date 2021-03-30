package org.keremcomert.wakeonsleep

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.keremcomert.wakeonsleep.databinding.ActivityMainBinding
import java.math.BigInteger
import java.net.*
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val wolJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + wolJob)

    companion object {
        const val SEPERATOR = ":"
        const val MAC = "18c04d35cc5b"
        const val IP = "192.168.1.255"
        const val PORT = 4532
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        with(b) {
            setContentView(root)
            bWOL.setOnClickListener { createWOLCoroutine() }
        }
    }

    private fun createWOLCoroutine() {
        coroutineScope.launch(Dispatchers.Main) { sendWOLSignal() }
    }
    private suspend fun sendWOLSignal() {
        withContext(Dispatchers.IO) {
            val socket = DatagramSocket(PORT)
            socket.broadcast = true
            var hexData = "ffffffffffff"
            for (x in 0..15){ hexData += MAC }
            Log.d("SEND", hexData)
            val data: ByteArray = BigInteger(hexData, 16).toByteArray()
            val packet = DatagramPacket(data, data.size, InetAddress.getByName(IP), 9)
            Log.d("SEND", "Data: $data, data.size: ${data.size}, Inet:  ${InetAddress.getByName(IP)}")
            socket.send(packet)
            socket.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wolJob.cancel()
    }
}