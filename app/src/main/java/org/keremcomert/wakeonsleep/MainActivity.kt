package org.keremcomert.wakeonsleep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.keremcomert.wakeonsleep.databinding.ActivityMainBinding
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val wolJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + wolJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        with(b){
            setContentView(root)
            bWOL.setOnClickListener { createWOLCoroutine() }
        }
    }

    private fun createWOLCoroutine(){
        coroutineScope.launch(Dispatchers.Main) { sendWOLSignal() }
    }

    private suspend fun sendWOLSignal(){
        withContext(Dispatchers.IO){
            try{
                val socket = DatagramSocket(4000)
                socket.broadcast = true
                val wolHeader = "ffffffffffff"
                val mac: String = params.get(0).toString()
                val macWolData = String(CharArray(16)).replace("\u0000", mac) //repeat mac 16 times
                val hexData = wolHeader + macWolData
                val data: ByteArray = hexData.toByteArray()
                val packet = DatagramPacket(data, data.size, getBroadcastAddress(), 40000)
                socket.send(packet)
                socket.close()
            }catch (e: IOException){}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wolJob.cancel()
    }
}