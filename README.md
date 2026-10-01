# Community Gallery Camera V1.1

Proyecto Android para Samsung con dos aplicaciones de prueba:

- **Gallery Camera**: importa imágenes a una galería privada.
- **Gallery Camera Demo**: prueba el flujo explícito de captura compatible entre apps controladas.

## Compatibilidad añadida en V1.1

Gallery Camera ahora ofrece sus imágenes de dos formas estándar de Android:

1. **Storage Access Framework / DocumentsProvider**  
   Cuando una app usa `ACTION_OPEN_DOCUMENT`, el selector de archivos de Android puede mostrar **Gallery Camera** como una fuente de imágenes.

2. **GET_CONTENT / PICK**  
   Cuando una app usa `ACTION_GET_CONTENT` o `ACTION_PICK` con `image/*`, **Gallery Camera** puede aparecer como opción para elegir una imagen.

La selección siempre requiere una acción del usuario.

## Captura de cámara

Android 11 y posteriores reservan los intents implícitos `IMAGE_CAPTURE` a cámaras preinstaladas del sistema. Por eso una APK normal no puede convertirse en sustituto universal de la cámara para todas las apps.

El módulo Demo continúa usando una invocación explícita para probar el flujo de captura controlado.

## APK

GitHub Actions compila automáticamente:

- `GalleryCamera-debug.apk`
- `GalleryCameraDemo-debug.apk`

Abre la última ejecución **Build Samsung APKs** y descarga el artifact **Community-Gallery-Camera-Samsung**.

## Prueba de selección general

1. Instala Gallery Camera.
2. Importa varias imágenes.
3. Abre una app que permita adjuntar/subir una imagen mediante el selector estándar de Android.
4. Busca **Gallery Camera** entre las fuentes/opciones disponibles.
5. Elige una imagen.
6. Android devuelve un `content://` URI con permiso temporal de lectura a la app solicitante.

## Compatibilidad

- minSdk: 29
- targetSdk: 35
- compileSdk: 35
- Version: 1.1
