# Community Gallery Camera V1

Proyecto Android de prueba para Samsung con dos aplicaciones:

- **Gallery Camera**: importa imágenes a una galería privada y puede devolver una imagen a una app de prueba mediante un flujo de captura explícito.
- **Gallery Camera Demo**: solicita una imagen, abre Gallery Camera y muestra el resultado recibido.

## Cómo obtener los APK

GitHub Actions compila automáticamente:

- `GalleryCamera-debug.apk`
- `GalleryCameraDemo-debug.apk`

Ve a la pestaña **Actions** del repositorio y abre la ejecución **Build Samsung APKs**. En **Artifacts** descarga el paquete generado.

## Prueba en Samsung

1. Instala ambos APK de la misma compilación.
2. Abre **Gallery Camera**.
3. Pulsa **Importar imágenes** y selecciona fotos.
4. Abre **Gallery Camera Demo**.
5. Pulsa **Solicitar foto**.
6. Selecciona una imagen.
7. La Demo mostrará el archivo recibido.

Consulta `INSTALAR_EN_SAMSUNG.md` para instrucciones detalladas.

## Compatibilidad

- minSdk: 29
- targetSdk: 35
- compileSdk: 35
- Java 17
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3

La integración de esta V1 se limita a apps de prueba/controladas que invoquen explícitamente la actividad de Gallery Camera.
