package com.example.bluetooth

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Message
import android.os.SystemClock

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import android.os.Handler;


import android.widget.TextView
import android.widget.Toast
import java.nio.charset.StandardCharsets
import com.example.bluetooth.databinding.ActivityMainBinding
val BT_MESSAGE_READ = 2
val BT_CONNECTING_STATUS = 3
val BT_MESSAGE_WRITE = 4
@Suppress("DEPRECATION")
class ConnectedBluetoothThread(var context: Context, val mmSocket: BluetoothSocket, var TVRecivedData: TextView) : Thread() {
    var m_Context: Context? = null

    var m_BluetoothHandler: Handler? = null
    var m_TVRecivedData: TextView? = null

    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?

    init {
        m_Context = context
        m_TVRecivedData = TVRecivedData

        //블루투스 핸들로 객체 생성, 메세지를 받으면 출력해준다.
        m_BluetoothHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == BT_MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String((msg.obj as ByteArray)!!, StandardCharsets.UTF_8)
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    m_TVRecivedData?.setText(readMessage)

                }
            }
        }

        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null
        try {
            tmpIn = mmSocket.inputStream
            tmpOut = mmSocket.outputStream
        } catch (e: IOException) {
            Toast.makeText(
                m_Context,
                "소켓 연결 중 오류가 발생했습니다.",
                Toast.LENGTH_LONG
            ).show()
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut
    }

    override fun run() {
        val buffer = ByteArray(1024)
        var bytes: Int
        while (true) {
            try {
                bytes = mmInStream!!.available()
                if (bytes != 0) {
                    SystemClock.sleep(100)
                    bytes = mmInStream.available()
                    bytes = mmInStream.read(buffer, 0, bytes)
                    m_BluetoothHandler!!.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget()
                }

            } catch (e: IOException) {
                break
            }
        }
    }

    fun write(str: String) {
        val bytes = str.toByteArray()
        try {
            mmOutStream!!.write(bytes)
        } catch (e: IOException) {
            Toast.makeText(
                m_Context,
                "데이터 전송 중 오류가 발생했습니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun obtainMessageStatus() {
        m_BluetoothHandler!!.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget()
    }

    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
            Toast.makeText(
                m_Context,
                "데이터 전송 중 오류가 발생했습니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}