package com.dhiego.connectivitymonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    //Views da tela
    private lateinit var tvWifiStatus: TextView
    private lateinit var tvWifiSsid: TextView
    private lateinit var tvBtStatus: TextView
    private lateinit var tvGnssStatus: TextView
    private lateinit var tvGnssInfo: TextView
    private lateinit var rvEvents: RecyclerView

    //Adapter do log de eventos
    private val adapter = EventAdapter(mutableListOf())

    //Recebe atualizações do Service
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ConnectivityMonitorService.ACTION_WIFI_UPDATE  -> atualizarWifi(intent)
                ConnectivityMonitorService.ACTION_BT_UPDATE    -> atualizarBt(intent)
                ConnectivityMonitorService.ACTION_GNSS_UPDATE  -> atualizarGnss(intent)
                ConnectivityMonitorService.ACTION_EVENT_LOG    -> adicionarEvento(intent)
            }
        }
    }

    //Solicita permissões ao usuário
    private val permissaoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        val todasConcedidas = permissoes.values.all { it }
        if (todasConcedidas) iniciarService()
    }

    //Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        conectarViews()
        configurarRecyclerView()
        verificarPermissoes()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ConnectivityMonitorService.ACTION_WIFI_UPDATE)
            addAction(ConnectivityMonitorService.ACTION_BT_UPDATE)
            addAction(ConnectivityMonitorService.ACTION_GNSS_UPDATE)
            addAction(ConnectivityMonitorService.ACTION_EVENT_LOG)
        }
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    //O metodo para de ouvir os broadcasts para não consumir memória em segundo plano quando o App não estiver em uso
    override fun onPause() {
        super.onPause()
        // Para de ouvir broadcasts quando a tela não está visível
        unregisterReceiver(receiver)
    }

    //Setup
    private fun conectarViews() {
        tvWifiStatus  = findViewById(R.id.tvWifiStatus)
        tvWifiSsid    = findViewById(R.id.tvWifiSsid)
        tvBtStatus    = findViewById(R.id.tvBtStatus)
        tvGnssStatus  = findViewById(R.id.tvGnssStatus)
        tvGnssInfo    = findViewById(R.id.tvGnssInfo)
        rvEvents      = findViewById(R.id.rvEvents)
    }

    private fun configurarRecyclerView() {
        rvEvents.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true  // mostra o evento mais recente no topo
            stackFromEnd  = true
        }
        rvEvents.adapter = adapter
    }

    //Permissões
    private fun verificarPermissoes() {
        val necessarias = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val faltando = necessarias.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltando.isEmpty()) {
            iniciarService()
        } else {
            permissaoLauncher.launch(faltando.toTypedArray())
        }
    }

    private fun iniciarService() {
        val intent = Intent(this, ConnectivityMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    //Atualiza a tela com os dados recebidos
    private fun atualizarWifi(intent: Intent) {
        val conectado = intent.getBooleanExtra(ConnectivityMonitorService.EXTRA_WIFI_CONNECTED, false)
        val ssid      = intent.getStringExtra(ConnectivityMonitorService.EXTRA_WIFI_SSID)

        tvWifiStatus.text = if (conectado) "● Wi-Fi: Conectado" else "○ Wi-Fi: Desconectado"
        tvWifiStatus.setTextColor(getColor(if (conectado) android.R.color.holo_green_dark else android.R.color.darker_gray))
        tvWifiSsid.text = if (conectado) "SSID: $ssid" else "SSID: -"
    }

    private fun atualizarBt(intent: Intent) {
        val ativo = intent.getBooleanExtra(ConnectivityMonitorService.EXTRA_BT_ENABLED, false)
        tvBtStatus.text = if (ativo) "● Bluetooth: Ativo" else "○ Bluetooth: Inativo"
        tvBtStatus.setTextColor(getColor(if (ativo) android.R.color.holo_green_dark else android.R.color.darker_gray))
    }

    private fun atualizarGnss(intent: Intent) {
        val fix  = intent.getBooleanExtra(ConnectivityMonitorService.EXTRA_GNSS_FIX, false)
        val sats = intent.getIntExtra(ConnectivityMonitorService.EXTRA_GNSS_SATS, 0)

        tvGnssStatus.text = if (fix) "● GNSS: Fix obtido" else "○ GNSS: Sem fix"
        tvGnssStatus.setTextColor(getColor(if (fix) android.R.color.holo_green_dark else android.R.color.darker_gray))
        tvGnssInfo.text = "Satélites: $sats"
    }

    private fun adicionarEvento(intent: Intent) {
        val tipo = intent.getStringExtra(ConnectivityMonitorService.EXTRA_EVENT_TYPE) ?: return
        val desc = intent.getStringExtra(ConnectivityMonitorService.EXTRA_EVENT_DESC) ?: ""
        adapter.addEvent(ConnectivityEvent(System.currentTimeMillis(), tipo, desc))
    }
}