package com.example.bluetooth


import android.app.Service
import android.bluetooth.*
import android.content.*
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.util.*
import kotlin.experimental.and


class DeviceControlActivity : AppCompatActivity() {

    val FW_UPDATE_RAWDATA_SIZE = 20
    private var deviceAddress: String = ""
    private var bluetoothService: BluetoothLeService? = null
    private var m_FileExplorer: FileExplorer? = null
    var m_FileListAdapter : ArrayAdapter<String>? = null

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            bluetoothService = null
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            bluetoothService = (p1 as BluetoothLeService.LocalBinder).service
            bluetoothService?.connect(deviceAddress)
        }

    }

    var connected: Boolean = false
    var gattUpdateReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when(action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    Log.d("hskang", "Receiver: Connect")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    Log.d("hskang", "Receiver: Disconnected")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    bluetoothService?.let {
                        SelectCharacteristicData(it.getSupportedGattServices())
                        Log.d("hskang", "Receiver: Service Discovered")
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {

                    if( intent.hasExtra(BluetoothLeService.EXTRA_DATA) == true) {
                        val resp: ByteArray? =
                            intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                        Log.d("hskang", resp.toString())
                        printTextView(resp as ByteArray)
                    }
                    else
                    {
                        Log.d("hskang", "ACTION_DATA_AVAILABLE: No Extra Data")
                    }
                }
            }


        }
    }
/*
    private var STM32WB55_P2P_service_writeCharacteristic: BluetoothGattCharacteristic? = null
    private var STM32WB55_P2P_readCharacteristic: BluetoothGattCharacteristic? = null
    private var STM32WB55_P2P_notifyCharacteristic: BluetoothGattCharacteristic? = null

 */
    private var STM32WB55_OTA_BASE_ADDR_CHAR: BluetoothGattCharacteristic? = null
    private var STM32WB55_OTA_CONFIRM_CHAR: BluetoothGattCharacteristic? = null
    private var STM32WB55_OTA_WRITE_RAW_DATA_CHAR: BluetoothGattCharacteristic? = null
    private fun SelectCharacteristicData(gattServices: List<BluetoothGattService>) {
        for(gattService in gattServices){
            when(gattService.uuid) {
                BluetoothLeService.OTA_SERVICE_UUID -> {
                    var gattCharacteristics: List<BluetoothGattCharacteristic> =
                        gattService.getCharacteristics()
                    for (gattCharacteristic in gattCharacteristics) {
                        when (gattCharacteristic.uuid) {
                            BluetoothLeService.OTA_BASE_ADDR_CHAR_UUID -> {
                                STM32WB55_OTA_BASE_ADDR_CHAR = gattCharacteristic
                                Log.d("hskang", "OTA_BASE_ADDR_CHAR_UUID")
                            }
                            BluetoothLeService.OTA_CONFIRM_CHAR_UUID -> {
                                Log.d("hskang", "OTA_CONFIRM_CHAR_UUID")
                                STM32WB55_OTA_CONFIRM_CHAR = gattCharacteristic
                            }
                            BluetoothLeService.OTA_WRITE_RAW_DATA_CHAR_UUID -> {
                                Log.d("hskang", "OTA_WRITE_RAW_DATA_CHAR_UUID")
                                STM32WB55_OTA_WRITE_RAW_DATA_CHAR = gattCharacteristic
                            }

                        }
                    }
                }
                BluetoothLeService.GENERIC_ACCESS_UUID ->{
                    Log.d("hskang", "GENERIC_ACCESS_UUID")
                }
                BluetoothLeService.GENERIC_ATTRIBUTE_UUID ->{
                    Log.d("hskang", "GENERIC_ATTRIBUTE_UUID")
                }
            }
        }
    }



    private fun SendRawData(bytes: ByteArray){
        STM32WB55_OTA_WRITE_RAW_DATA_CHAR?.let{
            if(it.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0){
                bluetoothService?.writeCharacteristic(it, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            }
        }
    }

    private fun SendBaseAddrConfig(bytes: ByteArray){
        STM32WB55_OTA_BASE_ADDR_CHAR?.let{
            if(it.properties == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE){
                bluetoothService?.writeCharacteristic(it, bytes, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
            }
        }
    }

    private fun EnableNotifycation(enable: Boolean){
        /*
        STM32WB55_OTA_WRITE_CHAR?.let{
            if(it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0){
                bluetoothService?.setCharacteristicNotification(it, enable)
            }
        }
        */
    }

    private fun printTextView(bytes: ByteArray)
    {
        var TextViewReceiveData : TextView = findViewById<TextView>(R.id.textviewReadData) as TextView
        TextViewReceiveData.setText("P2P ServerID:" + bytes.get(0) + " switch: " + bytes.get(1))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    fun byteArrayToHex(a: ByteArray): String {
        val sb = StringBuilder()
        for (b in a) sb.append(String.format("%02x ", b and 0xff.toByte()))
        return sb.toString()
    }

    private fun SendFileFUOTAServer(f: File) {
        if(f.exists() == false) {
            Toast.makeText(getApplicationContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_LONG).show()
        }

        /*
        var bytes = f.readBytes()
        var size = f.length()

        Log.d("hskang", "file size: " + size )
        Log.d("hskang", bytes. )*/


        var bytes = f.readBytes()
        var InputbyteArray = bytes.inputStream()
        var rawDataByteArray = ByteArray(FW_UPDATE_RAWDATA_SIZE)
        try {
            val totalLen: Int = InputbyteArray.available()
            var offset: Int = 0
            var remainDataSize: Int = 0
            while(true) {
                remainDataSize = totalLen - offset
                if( remainDataSize >= FW_UPDATE_RAWDATA_SIZE) {
                    InputbyteArray.read(rawDataByteArray, 0, FW_UPDATE_RAWDATA_SIZE)
                    Log.d("hskang", byteArrayToHex(rawDataByteArray))
                }
                else{
                    rawDataByteArray = ByteArray(FW_UPDATE_RAWDATA_SIZE)
                    InputbyteArray.read(rawDataByteArray, 0, remainDataSize)
                    Log.d("hskang", byteArrayToHex(rawDataByteArray))
                    break
                }

                offset += FW_UPDATE_RAWDATA_SIZE

                //TODO : 파일 다운로드 시작 블루투스로

                SystemClock.sleep(1)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }


        InputbyteArray.close()

        /*
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

        var DialogListener = object : DialogInterface.OnClickListener {
            override fun onClick(d: DialogInterface? , item: Int) {
                connectSelectedDevice(items[item].toString());
            }
        }
        builder.setItems(items, DialogListener)

        val alert = builder.create()
        alert.show()*/
    }

    private var m_strSendData: String? = null
    private var m_blNotifyEnable: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        var BtnSendData: Button = findViewById<Button>(R.id.btnSendData) as Button
        var BtnEnableNotify: Button = findViewById<Button>(R.id.btnNotifyEnable) as Button

        val BtnSendDataClickListener = BtnSendData.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val bytes: ByteArray = ByteArray(4, {0x0})
                // Actions
                //0x1:stop, 0x2 start wireless file upload, 0x7: file upload finished, 0x8: Cancel upload
                bytes[0] = 0x02
                // base addr = 0x007000
                // endian 주의 base addr = 0x107000 -> bytes[1] = 0x10, bytes[2] = 0x70, bytes[3] = 0x00 -> 0x107000
                bytes[1] = 0x00
                bytes[2] = 0x70
                bytes[3] = 0x00


                SendBaseAddrConfig(bytes)
            }
        })

        m_blNotifyEnable = false
        val BtnEnableNotifyClickListener = BtnEnableNotify.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if(m_blNotifyEnable == true) {
                    m_blNotifyEnable = false
                }
                else
                {
                    m_blNotifyEnable = true
                }
                EnableNotifycation(m_blNotifyEnable!!)
            }
        })

        var intentFilter : IntentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        registerReceiver(gattUpdateReceiver, intentFilter)

        deviceAddress = intent.getStringExtra("address").orEmpty()
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


        //탐색기 기능 추가
        m_FileExplorer = FileExplorer()
        //파일리스트를 가져온다.
        var filelist : ArrayList<String> = m_FileExplorer!!.GetFileList() as ArrayList<String>
        var ListVeiwFile : ListView = findViewById<ListView>(R.id.filelist) as ListView

        //파일리스트 adapter 생성
        m_FileListAdapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, filelist);

        //리스트뷰에 adapter 연결
        ListVeiwFile.adapter = m_FileListAdapter

        //리스너 연결(파일이름 선택시 처리하는 로직 추가)
        var ListViewOnClickListener = ListVeiwFile.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

                var aFiles: ArrayList<String> = m_FileExplorer!!.GetFileList() as ArrayList<String>
                var Name: String = aFiles.get(p2) //클릭된 위치의 값을 가져옴

                //디렉토리이면
                if (Name.startsWith("[") && Name.endsWith("]")) {
                    Name = Name.substring(1, Name.length - 1) //[]부분을 제거해줌
                }

                var CurFilePath: String = m_FileExplorer!!.GetCurFilePath() as String
                //들어가기 위해 /와 터치한 파일 명을 붙여줌
                val Path: String = CurFilePath + "/" + Name

                val f = File(Path) //File 클래스 생성


                if (f.isDirectory) { //디렉토리면?
                    var TextViewCurPath : TextView = findViewById<TextView>(R.id.currentPath) as TextView
                    TextViewCurPath.setText(m_FileExplorer!!.GetCurFilePath())
                    m_FileExplorer!!.SetCurFilePath(Path) //현재를 Path로 바꿔줌
                    m_FileExplorer!!.RefreshFiles() //리프레쉬
                    m_FileListAdapter!!.notifyDataSetChanged()
                } else { //디렉토리가 아니면 토스트 메세지를 뿌림
                    Toast.makeText(getApplicationContext(), aFiles.get(p2), 1).show()
                    SendFileFUOTAServer(f)
                }
            }
        })

        //현재 Path(초기값은 Root)를 기준으로 파일이름들을 Refresh시켜주고, 아답터도 업데이트시켜준다.
        var TextViewCurPath : TextView = findViewById<TextView>(R.id.currentPath) as TextView
        TextViewCurPath.setText(m_FileExplorer!!.GetCurFilePath())
        m_FileExplorer!!.RefreshFiles()
        m_FileListAdapter!!.notifyDataSetChanged()

        /*
        //파일 권한 요청(파일을 읽거나 쓸수 있게 권한 요청한다.)
        requestPermissions(
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ),
            1
        )
        */
    }

    fun mOnClick(v: View) {
        when (v.id) {
            R.id.btnroot -> if (m_FileExplorer!!.IsPathRoot() == false) { //루트가 아니면 루트로 가기
                m_FileExplorer!!.SetPathRoot()
                m_FileExplorer!!.RefreshFiles()
                m_FileListAdapter!!.notifyDataSetChanged()
            }
            R.id.btnup -> if (m_FileExplorer!!.IsPathRoot() == false) { //루트가 아니면
                m_FileExplorer!!.SetUpPath()
                m_FileExplorer!!.RefreshFiles()
                m_FileListAdapter!!.notifyDataSetChanged()
            }
        }
    }
}


class BluetoothLeService: Service() {

    companion object{
        var ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        var ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        var ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_DISCOVERED"
        var ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        var EXTRA_DATA : String= "EXTRA_DATA"

        val CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


        val OTA_SERVICE_UUID = UUID.fromString("0000fe20-cc7a-482a-984a-7f2ed5b3e58f")
        val OTA_BASE_ADDR_CHAR_UUID = UUID.fromString("0000fe22-8e22-4541-9d4c-21edae82ed19")
        val OTA_CONFIRM_CHAR_UUID = UUID.fromString("0000fe23-8e22-4541-9d4c-21edae82ed19")
        val OTA_WRITE_RAW_DATA_CHAR_UUID = UUID.fromString("0000fe24-8e22-4541-9d4c-21edae82ed19")

        /*
        val P2P_SERVICE_UUID = UUID.fromString("0000FE40-cc7a-482a-984a-7f2ed5b3e58f")
        val P2P_CHAR_WRITE_READ_UUID = UUID.fromString("0000FE41-8e22-4541-9d4c-21edae82ed19")
        val P2P_CHAR_NOTIFY_UUID = UUID.fromString("0000FE42-8e22-4541-9d4c-21edae82ed19")
        */

        val GENERIC_ACCESS_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val GENERIC_ACCESS_CHAR_DEVICE_NAME_UUID = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb")

        val GENERIC_ATTRIBUTE_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
        val GENERIC_ATTRIBUTE_CHAR_SERVICE_CHANGED_UUID = UUID.fromString("00002A05-0000-1000-8000-00805f9b34fb")
    }


    val STATE_DISCONNECTED = 0
    val STATE_CONNECTING = 1
    val STATE_CONNECTED = 2

    var connectionState = STATE_DISCONNECTED
    var bluetoothGatt: BluetoothGatt? = null
    var deviceAddress: String = ""

    private val bluetoothAdapter: BluetoothAdapter? by
    lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
   // var bluetoothAdapter: BluetoothAdapter  = BluetoothAdapter.getDefaultAdapter()

    inner class LocalBinder: Binder() {
        val service = this@BluetoothLeService
    }

    var binder = LocalBinder()
    override  fun onBind(intent: Intent?): IBinder? = binder

    fun connect(address: String): Boolean {
        bluetoothGatt?.let {
            if (address.equals(deviceAddress)) {
                if (it.connect()) {
                    connectionState = STATE_CONNECTING
                    return true
                } else {
                }
            }
        }

        val device = bluetoothAdapter!!.getRemoteDevice(address)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        deviceAddress = address
        connectionState = STATE_CONNECTING
        return true
    }

    var gattCallback = object: BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var intentAction = ""
            when( newState) {
                BluetoothProfile.STATE_CONNECTING ->{
                    connectionState = STATE_CONNECTED
                    Log.d("hskang", "Connecting to GATT server.")
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                }
                BluetoothProfile.STATE_DISCONNECTED ->{
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                    Log.d("hskang", "Disonnected to GATT server.")
                }
                BluetoothProfile.STATE_CONNECTED ->{
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    Log.d("hskang", "Attempting to start service discovery: " +
                            bluetoothGatt?.discoverServices())
                }
            }
            super.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    Log.d("hskang", "Gatt Discovered")
                }
                else -> {}
            }
            super.onServicesDiscovered(gatt, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                    Log.d("hskang", "onCharacteristicRead Succecss")
                }
                else ->{

                }
            }
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            super.onCharacteristicChanged(gatt, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        var intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic?) {
        var intent = Intent(action)
        characteristic?.let{
            when(characteristic.uuid){
                OTA_CONFIRM_CHAR_UUID -> {
                    val data: ByteArray = characteristic.getStringValue(0).toByteArray()
                    Log.d("hskang", "Recevied data:" + data.toString() )
                    intent.putExtra(BluetoothLeService.EXTRA_DATA, data)
                }
                else->{
                    Log.d("hskang", String.format("%s", characteristic.uuid.toString()))
                }
            }
        }

        sendBroadcast(intent)
    }

    fun getSupportedGattServices(): List<BluetoothGattService> {
        return bluetoothGatt!!.getServices()
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, bytes: ByteArray, writeType: Int ){

        characteristic.setValue(bytes)
        characteristic.writeType = writeType
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean){
        bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        val descriptor :BluetoothGattDescriptor = characteristic.getDescriptor(CLIENT_CHAR_CONFIG)
        if(enable == true)
        {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        else
        {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        bluetoothGatt?.writeDescriptor(descriptor)
    }

}