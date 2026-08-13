# Arquitectura

- **Room** guarda borradores y ventas confirmadas de forma local y atómica.
- **SaleEngine** calcula totales, faltantes y vueltos para modo Guiado y Rápido.
- **DataStore** conserva nombre, tema, modo y montos rápidos.
- **WorkManager** reconstruye el CSV diario desde Room después de cada confirmación, lo guarda en una carpeta local por fecha y lo envía cuando hay internet.
- **Google Apps Script** recibe el CSV por HTTPS con una clave privada y actualiza la carpeta diaria correspondiente de Google Drive.
- **PdfDocument + FileProvider** generan y comparten reportes PDF sin permisos amplios de almacenamiento.
- **Next.js** muestra un CSV seleccionado localmente; el archivo nunca sale del navegador.

Room sigue siendo la fuente de verdad. Google Drive es respaldo y transporte de archivos, no una base de datos.
