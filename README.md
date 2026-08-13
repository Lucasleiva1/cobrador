# Caja Simple

Monorepo de una caja Android offline-first y un panel web privado para consultar ventas.

## Carpetas

- `android-app`: aplicación nativa Kotlin + Jetpack Compose, Room, DataStore y WorkManager.
- `web-dashboard`: visor Next.js local que abre los CSV guardados en Google Drive; los datos no se suben a otro servidor.
- `docs`: instalación y pruebas paso a paso en Windows.

## Inicio rápido

La aplicación Android funciona sin internet ni cuenta. Consulte [docs/INSTALACION_WINDOWS.md](docs/INSTALACION_WINDOWS.md) y [docs/INSTALAR_APK_EN_CELULAR.md](docs/INSTALAR_APK_EN_CELULAR.md).

Para el panel web:

```powershell
cd web-dashboard
npm.cmd install
npm.cmd run dev
```

Abra `http://localhost:3000`. Sin `.env.local`, usa datos demostrativos solamente en desarrollo.

## Respaldo

La aplicación guarda primero cada venta en Room y genera una copia local en `Documentos/Caja Simple/AAAA-MM-DD/`. Cuando hay internet, envía el CSV por HTTPS al receptor privado de Google Apps Script, que lo actualiza en `Caja Simple - Ventas/AAAA-MM-DD/` dentro de Google Drive.

La URL y la clave del receptor se leen desde `android-app/local.properties`, que está excluido de Git. Consulte `android-app/local.properties.example` para preparar otra computadora sin publicar credenciales.

## Actualizaciones

La aplicación consulta la última versión publicada en GitHub Releases al abrirse y también permite buscarla manualmente desde Ajustes. El APK se verifica con SHA-256 antes de abrir el instalador oficial de Android. Consulte [docs/ACTUALIZACIONES_ANDROID.md](docs/ACTUALIZACIONES_ANDROID.md).
