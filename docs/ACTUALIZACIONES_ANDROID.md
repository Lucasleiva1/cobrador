# Actualizaciones Android

Caja Simple consulta la última versión publicada en `Lucasleiva1/cobrador` mediante la API pública de GitHub Releases. No guarda usuarios, contraseñas ni tokens de GitHub.

Cada Release estable debe incluir:

- `caja-simple.apk`: APK de producción firmado.
- `caja-simple.apk.sha256`: huella SHA-256 para verificar la descarga.
- `version.json`: información técnica de la versión publicada.

La aplicación compara la versión publicada con la versión instalada. Si es superior, ofrece **Actualizar ahora** o **Más tarde**. La misma comprobación está disponible en **Ajustes → Actualizaciones → Buscar actualización**.

Android exige que el usuario autorice la instalación desde esta aplicación y confirma cada instalación mediante su interfaz oficial. No se realizan instalaciones silenciosas.

## Firma de producción

Todas las versiones deben conservar el mismo archivo privado `android-app/keystore/caja-simple-release.jks` y la variable privada de Windows `CAJA_SIMPLE_SIGNING_PASSWORD`. Ambos están excluidos de Git. Si se pierde la clave, Android no permitirá actualizar las instalaciones existentes.
