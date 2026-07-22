# 📱 RemoteScreen Android

Sistema de screen mirroring e controle remoto entre dispositivos Android.

## 🚀 Como Compilar

### Servidor (dispositivo a ser controlado):
1. Abra pasta `servidor/` no Android Studio
2. Sync Project with Gradle Files
3. Build → Build APK

### Cliente (dispositivo que controla):
1. Abra pasta `cliente/` no Android Studio
2. Sync Project with Gradle Files
3. Build → Build APK

## 📡 Como Usar

1. Instale o **servidor** no dispositivo alvo
2. Instale o **cliente** no dispositivo controlador
3. Conecte ambos na mesma rede WiFi
4. Abra o servidor, conceda permissão de captura de tela
5. Anote o IP mostrado
6. No cliente, digite o IP e conecte

## ⚠️ Requisitos

- Android 5.0+ (API 21+)
- Permissão de captura de tela no servidor
- Mesma rede WiFi ou port forwarding

## 👨‍💻 Desenvolvedor

Criado por: Lucas Rodrigues
