# Receptor de ventas de Caja Simple

`Codigo.gs` recibe cada venta individual, evita duplicados por UUID y vuelve a generar un único CSV legible por día. El bloqueo de Apps Script impide que dos teléfonos escriban al mismo tiempo.

Propiedades privadas del proyecto:

- `DEVICE_TOKEN`: clave compartida con la aplicación. Nunca se guarda en Git.
- `MAX_AUTHORIZED_DEVICES`: cantidad de teléfonos reales permitidos. Si no se define, el valor es `3`.
- `AUTHORIZED_DEVICES`: lista administrada automáticamente por el receptor.

Para ampliar el cupo en el futuro sólo hay que modificar `MAX_AUTHORIZED_DEVICES`; no hace falta reinstalar la aplicación. Las compilaciones de prueba con identificador `debug-` se rechazan y no consumen lugares.
