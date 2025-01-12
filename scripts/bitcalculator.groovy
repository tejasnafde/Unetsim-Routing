// Import necessary UnetStack packages
import org.arl.unet.*
import org.arl.unet.kernel.*
import org.arl.unet.util.*
import org.arl.unet.packet.Packet

/**
 * BitRateCalculator
 * 
 * This script calculates the bit rate on the receiving laptop by monitoring
 * the amount of data received over a specified time interval.
 */

// Configuration Parameters
final long INTERVAL_MS = 1000  // Time interval in milliseconds (e.g., 1000 ms = 1 second)

// Variables to track data
long totalBitsReceived = 0
long previousTotalBits = 0

// Timestamp for interval tracking
long lastTimestamp = System.currentTimeMillis()

// Function to handle incoming data
def onDataReceived(byte[] data) {
    // Increment the total bits received
    totalBitsReceived += data.length * 8
}

// Subscribe to PacketReceived events
subscribe(Packet) { Packet pkt ->
    onDataReceived(pkt.payload)
}

// Timer to calculate and display bit rate at each interval
def timer = schedule(INTERVAL_MS, INTERVAL_MS) {
    long currentTimestamp = System.currentTimeMillis()
    long elapsedTime = currentTimestamp - lastTimestamp

    // Calculate bits received in the last interval
    long bitsThisInterval = totalBitsReceived - previousTotalBits
    double bitRate = (bitsThisInterval / (elapsedTime / 1000.0)) // bits per second

    println "Bit Rate: ${bitRate} bps"

    // Update for next interval
    previousTotalBits = totalBitsReceived
    lastTimestamp = currentTimestamp
}

// Ensure the script runs indefinitely and handles shutdown gracefully
onShutdown {
    timer.cancel()
    println "BitRateCalculator script terminated."
}
