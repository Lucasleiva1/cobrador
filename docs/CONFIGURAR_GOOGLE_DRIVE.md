# Configurar Google Drive

La versión estable utiliza un receptor privado de Google Apps Script que se ejecuta con la cuenta propietaria de Drive. El celular no necesita Google Drive instalado ni una cuenta de Google iniciada.

## Configuración de compilación

Copie `android-app/local.properties.example` como `android-app/local.properties` y complete:

- `sdk.dir`: ruta del SDK de Android.
- `driveBackupUrl`: URL publicada de Apps Script terminada en `/exec`.
- `driveBackupToken`: clave privada compartida con el receptor.

`local.properties` no se guarda en Git.

Después de cada venta se actualiza:

- Teléfono: `Documentos/Caja Simple/AAAA-MM-DD/ventas-AAAA-MM-DD.csv`.
- Drive: `Caja Simple - Ventas/AAAA-MM-DD/ventas-AAAA-MM-DD.csv`.

Solo se crea una carpeta cuando ese día tiene al menos una venta.

## Qué ocurre sin internet

La venta siempre se guarda primero en el celular. Si Drive no puede escribir en ese momento, el respaldo se reintenta. La falta de internet nunca bloquea el cobro.

## Ver las ventas en la computadora

Si usa Google Drive para escritorio, abra `Mi unidad/Caja Simple - Ventas`. También puede entrar en [drive.google.com](https://drive.google.com). El CSV se abre directamente con Google Sheets o con el panel incluido en este proyecto.
