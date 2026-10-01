# Probar Community Gallery Camera V1 en Samsung

Descarga los dos APK del mismo artifact de GitHub Actions.

1. Instala **GalleryCamera-debug.apk**.
2. Instala **GalleryCameraDemo-debug.apk**.
3. Abre Gallery Camera.
4. Pulsa **Importar imágenes** y elige dos o tres fotos.
5. Abre Gallery Camera Demo.
6. Pulsa **Solicitar foto**.
7. Gallery Camera se abrirá y mostrará las imágenes importadas.
8. Toca una imagen.
9. La Demo volverá al frente y mostrará la imagen recibida.

## Samsung / One UI

Si Android bloquea la instalación:
Ajustes > Seguridad y privacidad > Más ajustes de seguridad > Instalar apps desconocidas.

Autoriza temporalmente el navegador o Mis archivos que estés usando para abrir los APK.

## Importante

Instala ambos APK del mismo workflow. La V1 de prueba usa un permiso Android de tipo **signature**, por lo que ambos APK deben estar firmados con la misma clave de compilación.
