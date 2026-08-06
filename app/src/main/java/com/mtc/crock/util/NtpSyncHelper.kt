package com.mtc.crock.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Wi-Fi接続時にNTPサーバーから正確な時刻オフセットを取得するヘルパーオブジェクト
 */
object NtpSyncHelper {

    private const val NTP_PACKET_SIZE = 48
    private const val NTP_PORT = 123
    private const val OFFSET_70_YEARS = 2208988800L

    /**
     * NTPサーバーと通信し、ミリ秒単位のオフセット差分を取得する
     * @return 内部時計に対する補正値(ms)。失敗した場合は0Lを返す
     */
    suspend fun fetchNtpOffset(serverHost: String = Constants.NTP_SERVER_HOST): Long = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                soTimeout = Constants.NTP_TIMEOUT_MS
            }
            val address = InetAddress.getByName(serverHost)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            
            // NTP Request Header (Mode 3 = Client, Version 3)
            buffer[0] = 0x1B

            val requestTime = System.currentTimeMillis()
            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            val responseTime = System.currentTimeMillis()

            // Transmit Timestamp (Byte 40..47)
            var seconds = 0L
            for (i in 40..43) {
                seconds = (seconds shl 8) or (buffer[i].toLong() and 0xFF)
            }
            val serverTimeMs = (seconds - OFFSET_70_YEARS) * 1000L

            // 簡易ネットワークラウンドトリップ推定 offset = serverTime - (requestTime + rtt/2)
            val rtt = responseTime - requestTime
            val estimatedServerTime = serverTimeMs + (rtt / 2)
            val offset = estimatedServerTime - responseTime

            offset
        } catch (e: Exception) {
            0L
        } finally {
            socket?.close()
        }
    }
}
