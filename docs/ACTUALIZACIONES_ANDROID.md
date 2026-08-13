# Actualizaciones Android

Caja Simple consulta la última versión publicada en `Lucasleiva1/cobrador` mediante la API pública de GitHub Releases. No guarda usuarios, contraseñas ni tokens de GitHub.

Cada Release estable debe incluir:

- `caja-simple.apk`: APK de producción firmado.
- `caja-simple.apk.sha256`: huella SHA-256 para verificar la descarga.
- `version.json`: información técnica de la versión publicada.

La aplicación compara la versión publicada con la versión instalada. Si es superior, ofrece **Actualizar ahora** o **Más tarde**. La misma comprobación está disponible en **Ajustes → Actualizaciones → Buscar actualización**.

Android exige que el usuario autorice la instalación desde esta aplicación y confirma cada instalación mediante su interfaz oficial. No se realizan instalaciones silenciosas.

## Firma de producción

Todas las versiones deben conservar los archivos privados `caja-simple-release.jks` y `signing.properties` ubicados en la carpeta `.caja-simple` del perfil de Windows, fuera del repositorio. El compilador también admite la variable privada `CAJA_SIMPLE_SIGNING_PASSWORD` como alternativa. Ninguna clave se guarda en Git. Si se pierde la firma definitiva, Android no permitirá actualizar las instalaciones existentes.
