# Georefer

Aplicación Android offline para técnicos de campo y agricultores. El proyecto está en su primer incremento funcional.

Se entrega como código fuente para abrirlo en Android Studio. No se ha generado ningún APK.

## Implementado

- Proyecto Android en Kotlin y Jetpack Compose.
- Identidad visual Bosque andino.
- Navegación base: Mapa, Descargas, Puntos y Ajustes.
- Pantalla inicial sin mapas precargados.
- Motor MapLibre Native para mapas vectoriales reales.
- Descarga por Meta, Casanare o un rectángulo seleccionado sobre el mapa.
- Vistas Rural, Cartográfica, Minimalista y Alto contraste.
- Progreso, pausa, reanudación, eliminación y activación de mapas offline.
- Almacenamiento de regiones dentro de la aplicación y límite preventivo de 6.000 teselas.
- Cambio del mapa activo exclusivamente desde Descargas.
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

- Selección por municipio y recorte exacto al polígono departamental; actualmente se usa su rectángulo envolvente.
- Vista satelital y curvas de nivel con una fuente que autorice descarga offline gratuita.
- Compatibilidad con GeoPDF de varias páginas y variantes sin `LPTS/GPTS`.
- Puntos manuales persistentes.
- Servidor propio o paquetes estáticos para distribución a mayor escala sin depender de una instancia pública.

Consulta [los pasos para Android Studio](docs/android-studio-pasos.md) y la [especificación del producto](docs/especificacion-requisitos-georefer.md).
