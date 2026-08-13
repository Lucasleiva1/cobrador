# Instalar el APK en un celular

## Método simple

1. Copie el archivo `app-debug.apk` al celular por cable, Drive o correo propio.
2. En el celular, abra el archivo.
3. Si Android muestra “No se permiten apps de esta fuente”, pulse **Ajustes** y habilite temporalmente la fuente usada.
4. Pulse **Instalar**.
5. Abra **Caja Simple**.

La ruta generada por Gradle es:

`android-app/app/build/outputs/apk/debug/app-debug.apk`

## Por USB con ADB

1. Active **Opciones de desarrollador → Depuración USB** en el celular.
2. Conecte el cable y acepte la huella de la computadora.
3. Ejecute desde `android-app`:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

La instalación debug es para uso directo y pruebas. Android puede advertir que la app no proviene de Play Store; es esperado.

