# Instalación en Windows

## Lo que necesita

- Windows 10 u 11 de 64 bits.
- Android Studio estable.
- Un cable USB si prefiere probar en su celular.

## Abrir el proyecto Android

1. Abra **Android Studio**.
2. Pulse **Open**.
3. Elija la carpeta `android-app` de este proyecto.
4. Si Android Studio ofrece instalar Android SDK 37 o Build Tools 36, acepte.
5. Espere a que termine “Gradle Sync”. La primera vez puede tardar varios minutos.

El proyecto usa el JDK incluido con Android Studio. No hace falta instalar Java aparte.

## Abrir el panel de ventas

1. Abra PowerShell dentro de la carpeta del proyecto.
2. Ejecute:

```powershell
cd web-dashboard
npm.cmd install
npm.cmd run dev
```

3. Abra `http://localhost:3000` en Chrome, Edge o Firefox.
4. Pulse **Abrir archivo de Drive** y elija `ventas-AAAA-MM-DD.csv` dentro de su Google Drive.

El archivo se procesa en el navegador y no se envía a ningún servidor.

