package com.example.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetooth.databinding.ActivityMainBinding
import java.util.*
import java.io.IOException;

val BT_REQUEST_ENABLE = 1


//stm32wb55 with p2p server, Building wireless applications with STM32WB Series microcontrollerspdf 69 page
val P2P_SERVICE_UUID = UUID.fromString("0000FE40-cc7a-482a-984a-7fed5b3e58f")
val P2P_WRITE_READ_UUID = UUID.fromString("0000FE41-8e22-4541-9d4c-21edae82ed19")
val P2P_NOTIFY_UUID = UUID.fromString("0000FE42-8e22-4541-94dc-21edae82ed19")

class MainActivity : AppCompatActivity() {

    //null 가능 type-> ?르 붙인다.
    var m_TVBluetoothStatus: TextView? = null
    var m_TVReceiveData: TextView? = null
    var m_TVSendData: TextView? = null

    var m_BtnBLEOn: Button? = null
    var m_BtnBLEOff: Button? = null
    var m_BtnConnect: Button? = null
    var m_BtnSendData: Button? = null

    var m_BluetoothAdapter: BluetoothAdapter? = null
    var m_PairedDevices: Set<BluetoothDevice>? = null
    var m_ListPairedDevices: List<String>? = null
    var m_BluetoothDevice: BluetoothDevice? = null
    var m_BluetoothSocket: BluetoothSocket? = null

    var m_ThreadConnectedBluetooth: ConnectedBluetoothThread? = null

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //1. 변수 초기화
        m_TVBluetoothStatus = findViewById<TextView>(R.id.TextViewstatus) as TextView
        //m_TVBluetoothStatus?.setText("hello") // safe call operator를 사용해서 mutual Property build error를 해결
        m_TVReceiveData = findViewById<TextView>(R.id.TextViewReceiveData) as TextView
        m_TVSendData = findViewById<TextView>(R.id.TextEditSendData) as TextView

        m_BtnBLEOn = findViewById<Button>(R.id.buttonBLEOn) as Button
        m_BtnBLEOff = findViewById<Button>(R.id.buttonBLEOff) as Button
        m_BtnConnect = findViewById<Button>(R.id.buttonConnect) as Button
        m_BtnSendData = findViewById<Button>(R.id.buttonSend) as Button


        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        //2. 권한 설정
        //Build.VERSION.SDK_INT = 앱이 구동되고 있는 장치의 안드로이드 OS 버전
        //Build.VERSION_CODES.S = OS 버전별 상수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("hskang", "Cur Deivce Version=" + Build.VERSION.SDK_INT)
            Log.d("hskang", "code version=" + Build.VERSION_CODES.S)
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("hskang", "Cur Deivce Version=" + Build.VERSION.SDK_INT)
            Log.d("hskang", "code version=" + Build.VERSION_CODES.M)
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }


        //3. object의 Event 설정
        val BLEOnClickListener = m_BtnBLEOn?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                BluetoothOn()
            }
        })

        val BLEOffClickListener = m_BtnBLEOff?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                BluetoothOff()
            }
        })

        val BLEConnectOnClickListener = m_BtnConnect?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                listPairedDevices()
            }
        })

        val BLESendOnClickListener = m_BtnSendData?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //m_TVBluetoothStatus?.setText(m_TVSendData?.getText()) //status에 입력된 text 표시
                if(m_ThreadConnectedBluetooth != null){
                    m_ThreadConnectedBluetooth!!.write(m_TVSendData?.getText().toString())
                    m_TVSendData?.setText("")
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BT_REQUEST_ENABLE -> if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                Toast.makeText(applicationContext, "블루투스 활성화", Toast.LENGTH_LONG).show()
                m_TVBluetoothStatus?.setText("활성화")
            } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
                m_TVBluetoothStatus?.setText("비활성화")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun BluetoothOn() :Unit{
        if(m_BluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
        }
        else {
            if (m_BluetoothAdapter?.isEnabled() == true) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show()
                m_TVBluetoothStatus?.setText("활성화")
            }
            else {
                val EnableIntentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(EnableIntentBluetooth, BT_REQUEST_ENABLE)
            }
        }
    }

    fun BluetoothOff() :Unit{
        if (m_BluetoothAdapter?.isEnabled() == true) {
            m_BluetoothAdapter?.disable()
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_LONG).show()
            m_TVBluetoothStatus?.setText("비활성화")
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    fun listPairedDevices() :Unit{
        Log.d("hskang", "Enter listPairedDevices")
        if (m_BluetoothAdapter?.isEnabled() == true) {
            m_PairedDevices = m_BluetoothAdapter!!.bondedDevices
            if(m_PairedDevices?.isEmpty() == false){
                Log.d("hskang", "Paired Device Exist!")
                var builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("장치 선택")

                m_ListPairedDevices = ArrayList<String>()
                for (device in m_PairedDevices!!) {
                    (m_ListPairedDevices as ArrayList<String>).add(device.name)
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }

                val items: Array<CharSequence> =
                    (m_ListPairedDevices as ArrayList<String>).toArray(arrayOfNulls<CharSequence>((m_ListPairedDevices as ArrayList<String>).size))

                (m_ListPairedDevices as ArrayList<String>).toArray(arrayOfNulls<CharSequence>((m_ListPairedDevices as ArrayList<String>).size))


                /*
                val BLEOffClickListener = m_BtnBLEOff?.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        BluetoothOff()
                    }
                })*/
                var DialogListener = object : DialogInterface.OnClickListener {
                    override fun onClick(d: DialogInterface? , item: Int) {
                        connectSelectedDevice(items[item].toString());
                    }
                }
                builder.setItems(items, DialogListener)

                val alert = builder.create()
                alert.show()
            }
            else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show()
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_LONG).show()
        }
    }

    fun connectSelectedDevice(selectedDeviceName: String) {
        for (tempDevice: BluetoothDevice in m_PairedDevices!!) {
            if (selectedDeviceName == (tempDevice as BluetoothDevice).name) {
                m_BluetoothDevice = tempDevice as BluetoothDevice
                break
            }
        }

        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra("address", m_BluetoothDevice?.getAddress())
        startActivity(intent)

        try {

            /*
            m_BluetoothSocket = m_BluetoothDevice!!.createRfcommSocketToServiceRecord(BT_SERIAL_PORT_UUID)
            m_BluetoothSocket!!.connect()
            m_ThreadConnectedBluetooth = ConnectedBluetoothThread(this, m_BluetoothSocket!! , m_TVReceiveData as TextView)
            m_ThreadConnectedBluetooth!!.start()
            m_ThreadConnectedBluetooth!!.obtainMessageStatus()
            */
        } catch (e: IOException) {
            Toast.makeText(applicationContext, "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

    }
}