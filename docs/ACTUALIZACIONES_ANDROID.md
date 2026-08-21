# Actualizaciones Android

Caja Simple consulta la última versión publicada en `Lucasleiva1/cobrador` mediante la API pública de GitHub Releases. No guarda usuarios, contraseñas ni tokens de GitHub.

La carpeta local `release-assets` conserva cada APK distribuible con la versión visible en su nombre. Nunca se reemplaza un APK numerado por el de una versión posterior.

Cada Release estable debe incluir:

- `caja-simple-vX.Y.Z.apk`: APK de producción firmado, numerado para archivo y distribución manual.
- `caja-simple-vX.Y.Z.apk.sha256`: huella SHA-256 del APK numerado.
- `caja-simple.apk`: alias técnico de la misma versión que las aplicaciones instaladas usan para actualizarse.
- `caja-simple.apk.sha256`: huella SHA-256 del alias técnico.
- `version.json`: información técnica de la versión publicada.

El nombre numerado es obligatorio para cualquier archivo destinado a personas. El nombre sin versión se genera únicamente dentro del workflow como alias técnico y no sustituye al archivo numerado.

La aplicación compara la versión publicada con la versión instalada. Si es superior, ofrece **Actualizar ahora** o **Más tarde**. La misma comprobación está disponible en **Ajustes → Actualizaciones → Buscar actualización**.

Android exige que el usuario autorice la instalación desde esta aplicación y confirma cada instalación mediante su interfaz oficial. No se realizan instalaciones silenciosas.

## Firma de producción

Todas las versiones deben conservar los archivos privados `caja-simple-release.jks` y `signing.properties` ubicados en la carpeta `.caja-simple` del perfil de Windows, fuera del repositorio. El compilador también admite las variables privadas `CAJA_SIMPLE_SIGNING_KEYSTORE` y `CAJA_SIMPLE_SIGNING_PASSWORD` para indicar otra ubicación segura y su contraseña. Ninguna clave se guarda en Git. Si se pierde la firma definitiva, Android no permitirá actualizar las instalaciones existentes.
