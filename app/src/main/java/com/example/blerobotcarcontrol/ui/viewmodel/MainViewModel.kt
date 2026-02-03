package com.example.blerobotcarcontrol.ui.viewmodel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE_NO_RESPONSE
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blerobotcarcontrol.data.BLEDevice
import com.example.blerobotcarcontrol.data.ConnectionState
import com.example.blerobotcarcontrol.data.ControlCommand
import com.example.blerobotcarcontrol.data.LogEntry
import com.example.blerobotcarcontrol.data.LogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    //--------- UUID для ESP32 BLE UART
    private val UART_SERVICE_UUID = UUID.fromString("C6FBDD3C-7123-4C9E-86AB-005F1A7EDA01")
    private val RX_CHARACTERISTIC_UUID = UUID.fromString("D769FACF-A4DA-47BA-9253-65359EE480FB")
    private val TX_CHARACTERISTIC_UUID = UUID.fromString("B88E098B-E464-4B54-B827-79EB2B150A9F")

    //--------- BLE Variables
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var writeCharacteristic: BluetoothGattCharacteristic
    private val targetDeviceName = "BLE_CAR"

    //--------- UI State
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = mutableStateListOf<BLEDevice>()
    val discoveredDevices: List<BLEDevice> = _discoveredDevices

    private val _logs = mutableStateListOf<LogEntry>()

    val logs: List<LogEntry> = _logs

    private val _selectedDevice = mutableStateOf<BLEDevice?>(null)
    val selectedDevice: BLEDevice? get() = _selectedDevice.value

    private var isScanning = false

    private var isSelected = false

//-------------------- INIT -----------------------------------------------------
    init {
        initializeBluetooth()
        addLog("Приложение запущено", LogType.INFO)
    }

//------------------ INITIALIZE BLUETOOTH ---------------------------------------
    private fun initializeBluetooth() {
        val bluetoothManager = getApplication<Application>()
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

//-------------------- CHECK PERMISSIONS -----------------------------------------
    fun checkPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {   // Если версия SDK Android >= API Level 31 (Android 12)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                getApplication(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

//----------------------------- START SCAN -----------------------------------------
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!checkPermissions()) {
            /* отладка
            val str = (Build.VERSION.SDK_INT).toString() + "  ?  API Level = " + (Build.VERSION_CODES.S).toString()
            addLog("SDK version = " + str, LogType.INFO ) */
            addLog("Требуются разрешения для BLE", LogType.ERROR)
            return
        }
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            addLog("Bluetooth выключен", LogType.ERROR)
            return
        }
        _discoveredDevices.clear()
        _connectionState.value = ConnectionState.Scanning
        isScanning = true
        addLog("Начато сканирование BLE устройств...", LogType.INFO)
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        bluetoothLeScanner?.startScan(scanCallback)

        // Остановить сканирование через ... секунд
        viewModelScope.launch {
            delay(500)
            stopScan()
        }
    }

//----------------------- STOP SCAN ------------------------------------
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            _connectionState.value = ConnectionState.Disconnected
            //addLog("Сканирование остановлено. Найдено устройств: ${_discoveredDevices.size}", LogType.INFO)
            addLog("Сканирование остановлено.", LogType.INFO)
        }
    }

//-------------------- SCAN CALLBACK -----------------------------------------
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            
            // Фильтруем целевые устройства
            // if(true) {
            // if (deviceName.contains("BLE", ignoreCase = true)) {
            if(device.address.compareTo("B0:A6:04:5A:91:96") == 0 && !isSelected) { // по MAC-адресу устройства BLE_CAR
                    // if(device.address.compareTo("94:A9:90:71:58:D6") == 0) { // по MAC-адресу устройства BLE_BOAT
                    val bleDevice = BLEDevice(
                        name = deviceName,
                        address = device.address,
                        device = device
                    )

                    // Добавляем в список, если устройство отсутствует
                    if (_discoveredDevices.none { it.address == device.address }) {
                        _discoveredDevices.add(bleDevice)
                     //   addLog("Найдено: $deviceName (${device.address})", LogType.SUCCESS)
                    }
                    selectDevice(bleDevice)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _connectionState.value = ConnectionState.Error("Ошибка сканирования: $errorCode")
            addLog("Ошибка сканирования: $errorCode", LogType.ERROR)
        }
    }

//----------------- SELECT DEVICE -------------------------------------------
    fun selectDevice(device: BLEDevice) {
        _selectedDevice.value = device
        addLog("Выбрано устройство: ${device.name}", LogType.INFO)
        isSelected = true
      //  viewModelScope.launch {
        stopScan()
      //  }
    }

//------------------  CONNECT TO DEVICE --------------------------------------
    @SuppressLint("MissingPermission")
    fun connectToDevice() {
        val device = _selectedDevice.value ?: run {
            addLog("Не выбрано устройство", LogType.ERROR)
            return
        }

        _connectionState.value = ConnectionState.Connecting
        addLog("Подключение к ${device.name}...", LogType.INFO)

        if (bluetoothGatt != null) {
            bluetoothGatt?.close()
            bluetoothGatt = null
        }

        device.device?.let { bluetoothDevice ->
            bluetoothGatt = bluetoothDevice.connectGatt(
                getApplication(),
                false,
                gattCallback
            )
        }
    }

//--------------------  DICONNECT ------------------------------------------
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic
        _connectionState.value = ConnectionState.Disconnected
        addLog("Разрыв соединения", LogType.INFO)
    }

    //------------------  GATT CALLBACK -------------------------------------
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            viewModelScope.launch { // асинхронное выполнение корутины в области viewModelScope
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (newState) {
                        BluetoothGatt.STATE_CONNECTED -> {
                            addLog("Сопряжение выполнено.",
                                LogType.SUCCESS)
                            gatt.discoverServices()     // Обнаружение сервисов
                        }
                        BluetoothGatt.STATE_DISCONNECTED -> {
                            _connectionState.value = ConnectionState.Disconnected
                            addLog("Разрыв соединения.", LogType.INFO)
                            gatt.close()
                        }
                    }
                }
                else {
                    addLog("Сопряжение не выполнено.", LogType.ERROR)
                    gatt.close()
                }
            }
        }

//------------------ ON SERVICES DISCOVERED -----------------------------------------
    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                              // Обратный вызов 'onServicesDiscovered' получен: сервисы обнаружены
       super.onServicesDiscovered(gatt, status)
            
       viewModelScope.launch { // асинхронное выполнение корутины в области viewModelScope
           if (status == BluetoothGatt.GATT_SUCCESS) {
               val service = gatt.getService(UART_SERVICE_UUID)
               if (service != null) {
                   writeCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID)
                   if (writeCharacteristic != null) {
                       _connectionState.value =
                       ConnectionState.Connected(_selectedDevice.value?.name ?: "Unknown")
                       addLog("UART-сервис найден.", LogType.SUCCESS)
                   } else {
                        _connectionState.value = ConnectionState.Error("Характеристика записи не найдена")
                        addLog("Характеристика записи не найдена", LogType.ERROR)
                   }
               } else {
                    _connectionState.value = ConnectionState.Error("UART сервис не найден")
                    addLog("UART-сервис не найден", LogType.ERROR)
               }
           } else {
                    _connectionState.value = ConnectionState.Error("Ошибка обнаружения сервисов: $status")
                    addLog("Ошибка обнаружения сервисов: $status", LogType.ERROR)
           }
       }
    }

    //------------------ ON CHARACTERISTIC WRITE ---------------------------
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (status != BluetoothGatt.GATT_SUCCESS) {
            viewModelScope.launch {  // асинхронное выполнение корутины в области viewModelScope
                addLog("Ошибка отправки команды: $status", LogType.ERROR)
            }
        }
        else if(status == BluetoothGatt.GATT_SUCCESS) {
            viewModelScope.launch {  // асинхронное выполнение корутины в области viewModelScope
         //       addLog("Отправлено успешно с подтверждением.", LogType.SUCCESS)
            }
        }
    }
}

//------------------------- SEND COMMAND ------------------------------
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission", "SuspiciousIndentation")
fun sendCommand(command: ControlCommand) {
    if (bluetoothGatt == null || writeCharacteristic == null) {
        addLog("Нет соединения", LogType.ERROR)
        return
    }
    if ((writeCharacteristic!!.properties).and(PROPERTY_WRITE).toInt() == PROPERTY_WRITE) {  // default to "8"
        writeCharacteristic!!.writeType = WRITE_TYPE_DEFAULT    // default to "2"
        //   writeCharacteristic!!.writeType = PROPERTY_WRITE   // default to "8"
    }
    else if ((writeCharacteristic!!.properties).and(PROPERTY_WRITE_NO_RESPONSE).toInt() ==
        PROPERTY_WRITE_NO_RESPONSE) { // default to "4"
        writeCharacteristic!!.writeType = WRITE_TYPE_NO_RESPONSE    // default to "1"
    }
    /*  отладка
    if (writeCharacteristic!!.writeType == 2)
        addLog("writeType: WRITE_TYPE_DEFAULT", LogType.INFO)
    else if (writeCharacteristic!!.writeType == 8)
        addLog("writeType: PROPERTY_WRITE", LogType.INFO)
    */
    writeCharacteristic!!.value = command.code.toByteArray()

    @Suppress("DEPRECATION")
    bluetoothGatt?.writeCharacteristic(writeCharacteristic)
    addLog("command: ${command.code}", LogType.INFO)
}

//--------------------- CLEAR LOGS ------------------------------------
fun clearLogs() {
    _logs.clear()
     //  addLog("Журнал событий очищен", LogType.INFO)
}

//----------------------- ADD LOGS -------------------------------------
    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(timestamp, message, type)

        viewModelScope.launch { // асинхронное выполнение корутины в области viewModelScope
            _logs.add(0, logEntry) // Добавляем в начало
            // Ограничим размер лога
            if (_logs.size > 100) {
                _logs.removeAt(_logs.size - 1)
            }
        }
    }

//----------------- FORMAT TIMESTAMP -----------------------------------
    fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat(" HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

//------------------- ON CLEARED   ---------------------------------------
    override fun onCleared() {
        super.onCleared()
       // disconnect()
        stopScan()
    }
}
