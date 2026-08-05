# Georefer

Aplicación Android offline para técnicos de campo y agricultores. El proyecto está en su primer incremento funcional.

Se entrega como código fuente para abrirlo en Android Studio. No se ha generado ningún APK.

## Implementado

- Proyecto Android en Kotlin y Jetpack Compose.
- Identidad visual Bosque andino.
- Navegación base: Mapa, Descargas, Puntos y Ajustes.
- Pantalla inicial sin mapas precargados.
- Importación persistente de GeoPDF locales mediante el selector seguro de Android.
- Validación de GeoPDF de una página y tamaño máximo de 250 MB.
- Renderizado offline del mapa con zoom y desplazamiento táctil.
- Lectura de `BBox`, `LPTS` y `GPTS` para colocar el GPS sobre la página georreferenciada.
- Permisos de ubicación precisa y tratamiento de ubicación aproximada.
- Lectura del GNSS interno mediante APIs de Android, sin Google Maps ni servicios de ubicación externos.
- Semáforo GPS: Buscando, Baja, Aceptable y Buena.
- Precisión, coordenadas WGS 84, antigüedad y satélites usados/visibles.
- El GNSS se detiene cuando la aplicación sale del primer plano.
- Pruebas unitarias del clasificador de calidad.

## Aún no implementado

- Motor MapLibre y visualización de mapas reales.
- Descargas por municipio o zona.
- Compatibilidad con GeoPDF de varias páginas y variantes sin `LPTS/GPTS`.
- Puntos manuales persistentes.
- Catálogo estático y paquetes offline.

Consulta [los pasos para Android Studio](docs/android-studio-pasos.md) y la [especificación del producto](docs/especificacion-requisitos-georefer.md).
