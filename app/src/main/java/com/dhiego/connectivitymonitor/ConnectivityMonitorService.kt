package com.dhiego.connectivitymonitor

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.GnssStatus
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.net.wifi.WifiInfo

class ConnectivityMonitorService : Service() {


    companion object {
        const val ACTION_WIFI_UPDATE = "com.dhiego.connectivity.WIFI_UPDATE"
        const val ACTION_BT_UPDATE   = "com.dhiego.connectivity.BT_UPDATE"
        const val ACTION_GNSS_UPDATE = "com.dhiego.connectivity.GNSS_UPDATE"
        const val ACTION_EVENT_LOG   = "com.dhiego.connectivity.EVENT_LOG"

        const val EXTRA_WIFI_CONNECTED = "wifi_connected"
        const val EXTRA_WIFI_SSID      = "wifi_ssid"
        const val EXTRA_BT_ENABLED     = "bt_enabled"
        const val EXTRA_GNSS_FIX       = "gnss_fix"
        const val EXTRA_GNSS_SATS      = "gnss_sats"
        const val EXTRA_EVENT_TYPE     = "event_type"
        const val EXTRA_EVENT_DESC     = "event_desc"

        private const val CHANNEL_ID      = "connectivity_channel"
        private const val NOTIFICATION_ID = 1001
    }


    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var locationManager: LocationManager
    private lateinit var bluetoothAdapter: BluetoothAdapter


    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {

         //O sistema chama o metodo para avisar que a conexão com a rede Wifi foi efetuada ou quebrada
        override fun onAvailable(network: Network) {
            broadcastWifi(connected = true, ssid = getWifiSsid())
            broadcastEvent("WIFI_CONECTADO", "Wi-Fi conectado")
        }

        override fun onLost(network: Network) {
            broadcastWifi(connected = false)
            broadcastEvent("WIFI_DESCONECTADO", "Wi-Fi desconectado")
        }
    }


    private val gnssCallback = object : GnssStatus.Callback() {

        override fun onStarted() {
            broadcastEvent("GNSS_INICIADO", "Receptor GNSS ligado")
        }

        override fun onFirstFix(ttffMillis: Int) {
            broadcastEvent("GNSS_FIX", "Primeiro fix em ${ttffMillis}ms")
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {

            var usedSats = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) usedSats++
            }
            val hasFix = usedSats >= 4  // mínimo para fix 3D
            broadcastGnss(hasFix = hasFix, satellites = usedSats)
        }

        override fun onStopped() {
            broadcastGnss(hasFix = false, satellites = 0)
            broadcastEvent("GNSS_PARADO", "Receptor GNSS desligado")
        }
    }


    private val bluetoothReceiver = object : BroadcastReceiver() {

        //O sistema chama o método para avisar que o estado do Bluetooth foi alterado
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            when (state) {
                BluetoothAdapter.STATE_ON  -> {
                    broadcastBt(enabled = true)
                    broadcastEvent("BT_LIGADO", "Bluetooth ativado")
                }
                BluetoothAdapter.STATE_OFF -> {
                    broadcastBt(enabled = false)
                    broadcastEvent("BT_DESLIGADO", "Bluetooth desativado")
                }
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        locationManager     = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        bluetoothAdapter    = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        criarCanalNotificacao()
        startForeground(NOTIFICATION_ID, criarNotificacao("Monitorando conectividade…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registrarWifiCallback()
        registrarBluetoothReceiver()
        registrarGnssCallback()

        //Envia o estado atual logo ao iniciar, sem esperar uma mudança
        broadcastBt(enabled = bluetoothAdapter.isEnabled)

        /* Se o sistema encerrar o Service por falta de memória
         Ele reinicia automaticamente quanto tiver memória disponivel para melhorar o desempenho.
        */
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(wifiCallback)
        unregisterReceiver(bluetoothReceiver)
        locationManager.unregisterGnssStatusCallback(gnssCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

@Suppress("MissingPermission")
    private fun registrarWifiCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, wifiCallback)
    }

    private fun registrarBluetoothReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    @Suppress("MissingPermission")
    private fun registrarGnssCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerGnssStatusCallback(mainExecutor, gnssCallback)
            } else {
                locationManager.registerGnssStatusCallback(gnssCallback, null)
            }
        } catch (e: SecurityException) {
            broadcastEvent("GNSS_ERRO", "Permissão de localização negada")
        }
    }


    @Suppress("MissingPermission", "DEPRECATION")
    private fun getWifiSsid(): String? {
        return try {
            val network = connectivityManager.activeNetwork
            val caps    = connectivityManager.getNetworkCapabilities(network)
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                caps?.transportInfo as? WifiInfo
            } else null
            if (info is android.net.wifi.WifiInfo) {
                info.ssid?.removeSurrounding("\"")
            } else null
        } catch (e: Exception) { null }
    }


    private fun broadcastWifi(connected: Boolean, ssid: String? = null) {
        sendBroadcast(Intent(ACTION_WIFI_UPDATE).apply {
            putExtra(EXTRA_WIFI_CONNECTED, connected)
            putExtra(EXTRA_WIFI_SSID, ssid ?: "")
        })
    }

    private fun broadcastBt(enabled: Boolean) {
        sendBroadcast(Intent(ACTION_BT_UPDATE).apply {
            putExtra(EXTRA_BT_ENABLED, enabled)
        })
    }

    private fun broadcastGnss(hasFix: Boolean, satellites: Int) {
        sendBroadcast(Intent(ACTION_GNSS_UPDATE).apply {
            putExtra(EXTRA_GNSS_FIX,  hasFix)
            putExtra(EXTRA_GNSS_SATS, satellites)
        })
    }

    private fun broadcastEvent(type: String, desc: String) {
        sendBroadcast(Intent(ACTION_EVENT_LOG).apply {
            putExtra(EXTRA_EVENT_TYPE, type)
            putExtra(EXTRA_EVENT_DESC, desc)
        })
    }


    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Connectivity Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(canal)
        }
    }

    private fun criarNotificacao(texto: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Connectivity Monitor")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}