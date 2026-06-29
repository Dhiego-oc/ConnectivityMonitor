# ConnectivityMonitor

Aplicativo Android desenvolvido em Kotlin para monitoramento em tempo real
de interfaces de conectividade: Wi-Fi, Bluetooth e GNSS/GPS.

Desenvolvido como projeto de portfólio com foco em Android Embedded,
explorando as APIs de plataforma do AOSP que se comunicam diretamente
com as camadas de Hardware Abstraction Layer (HAL).

---

## Funcionalidades

- Monitoramento de Wi-Fi em tempo real — status de conexão e SSID da rede
- Monitoramento do Bluetooth — detecta ativação e desativação do adaptador
- Monitoramento de GNSS/GPS — acompanha o receptor e número de satélites em uso
- Log de eventos em tempo real com horário, tipo e descrição de cada mudança
- Serviço em segundo plano com notificação persistente (Foreground Service)
- Reinício automático após reboot do dispositivo

---

## Arquitetura

O Service roda em background e notifica a Activity apenas quando
algo muda — padrão orientado a eventos, sem verificações periódicas.

---

## APIs utilizadas e relação com Android Embedded

| API Android | Função no app | Camada correspondente no AOSP |
|---|---|---|
| `ConnectivityManager.NetworkCallback` | Detecta mudanças de Wi-Fi | Connectivity Service / Wi-Fi HAL |
| `BluetoothAdapter` + `ACTION_STATE_CHANGED` | Monitora estado do BT | BluetoothManagerService / BT HAL |
| `GnssStatus.Callback` | Monitora receptor GPS | GnssLocationProvider / GNSS HAL |
| `Foreground Service` | Monitoramento em background | Equivalente a serviço de sistema |

---

## Tecnologias

- Kotlin
- Android SDK 26+ (Android 8.0)
- ConnectivityManager NetworkCallback
- GnssStatus.Callback
- Foreground Service
- BroadcastReceiver

---

## Como executar

1. Clone o repositório git clone https://github.com/Dhiego-oc/ConnectivityMonitor.git
2. Abra no Android Studio
3. Execute em um dispositivo físico com Android 8.0+
4. Conceda as permissões de localização solicitadas

> Recomendado dispositivo físico — emuladores têm limitações com GNSS e Bluetooth reais.

---

## Autor

**Dhiego de Oliveira Cavalcante**

Estudante de Engenharia de Software — UNINTER

[GitHub](https://github.com/Dhiego-oc) • [LinkedIn](https://linkedin.com/in/dhiego-cavalcante-26671a354)
