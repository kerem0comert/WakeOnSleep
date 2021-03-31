package org.keremcomert.wakeonsleep

interface ClassifyReceivedListener {
    fun onClassifyReceived(confidence: Int)
}