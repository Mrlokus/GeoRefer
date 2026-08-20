# Especificación de requisitos — GeoLuker

## 1. Objetivo

GeoLuker orientará a trabajadores de la plantación de Luker Agrícola sobre un único mapa oficial incluido en el APK. Todas las funciones operativas deberán trabajar sin conexión y sin cuentas de usuario.

## 2. Alcance de la versión 1

### Incluido

- Mapa GeoPDF oficial integrado y georreferenciado.
- Posición GNSS, precisión, satélites y rumbo del teléfono.
- Zoom, desplazamiento y rotación del mapa.
- Centrado automático y botón para volver a la ubicación.
- Aviso de ubicación fuera de la plantación.
- Búsqueda local de lotes por código.
- Creación, edición, eliminación y localización de puntos.
- Almacenamiento privado dentro del teléfono.
- Distribución privada mediante APK.

### Excluido

- Descarga de mapas, selección por departamento o rectángulo.
- Mapas satelitales o proveedores cartográficos externos.
- Importación de GeoPDF o archivos del almacenamiento.
- Exportación o importación de puntos.
- Cuentas, nube, servidor, sincronización o telemetría.
- Permisos y servicios de internet.

## 3. Requisitos funcionales

- **RF-001:** Al abrir, la aplicación cargará automáticamente el mapa oficial integrado.
- **RF-002:** La ausencia o invalidez del GeoPDF mostrará un error claro sin cerrar la aplicación.
- **RF-003:** El mapa admitirá gestos simultáneos de zoom, desplazamiento y rotación.
- **RF-003A:** El zoom se renderizará progresivamente por mosaicos y conservará una caché máxima de 48 MB.
- **RF-003B:** La cámara limitará el desplazamiento para evitar perder el mapa y animará los centrados automáticos.
- **RF-004:** Habrá controles para ver el mapa completo y restablecer el norte arriba.
- **RF-005:** Al recibir la primera posición válida dentro del mapa, la vista se centrará y ampliará automáticamente.
- **RF-006:** Un botón permitirá alternar entre centrado y orientación según el teléfono.
- **RF-007:** El GPS se mostrará como un punto azul pequeño, una flecha de rumbo y un círculo de incertidumbre proporcional a la precisión reportada.
- **RF-007A:** El modo de seguimiento mantendrá la ubicación centrada hasta que el usuario manipule el mapa.
- **RF-008:** La tarjeta GPS mostrará coordenadas, precisión y satélites usados/visibles; no mostrará el texto intermitente “Buscando señal”.
- **RF-009:** Si la posición geográfica no pertenece a la cobertura del GeoPDF, se mostrará “Estás fuera de Luker Agricola”.
- **RF-009A:** El aviso considerará la precisión GPS y exigirá lecturas consecutivas para evitar parpadeos cerca del límite.
- **RF-010:** La búsqueda aceptará coincidencias parciales de código y funcionará sin internet.
- **RF-011:** Al elegir un lote, el mapa se centrará en su rótulo y mostrará un indicador visual distinto del GPS y de los puntos guardados.
- **RF-012:** Una pulsación prolongada dentro del mapa iniciará un punto provisional.
- **RF-013:** Para guardar un punto se exigirá nombre; la nota será opcional.
- **RF-014:** Los puntos podrán editarse, eliminarse y localizarse desde la sección Puntos.
- **RF-014A:** Los puntos podrán buscarse y ordenarse por fecha, nombre o cercanía; sus nombres aparecerán en el mapa a partir de un nivel de zoom legible.
- **RF-014B:** No se guardará un segundo punto a menos de 1,5 m de uno existente.
- **RF-015:** Todos los puntos se conservarán únicamente en el almacenamiento interno privado de la aplicación.
- **RF-016:** La aplicación solicitará ubicación precisa en el momento de usar el GPS y ofrecerá acceso a ajustes cuando esté desactivado o denegado.

## 4. Requisitos no funcionales

- **RNF-001 Offline:** El uso normal no dependerá de internet y el manifiesto no declarará permisos de red.
- **RNF-002 Rendimiento:** Una vista previa de 2048 px se complementará con mosaicos visibles de hasta 16 384 px, renderizados fuera del hilo principal y con caché acotada.
- **RNF-003 Privacidad:** No se transmitirán ubicación, puntos ni identificadores.
- **RNF-004 Persistencia:** Los puntos sobrevivirán al cierre y reinicio de la aplicación, pero no a la desinstalación.
- **RNF-005 Diseño:** La interfaz usará la identidad marrón/crema de Luker Agrícola; el GPS permanecerá azul por claridad cartográfica.
- **RNF-006 Legibilidad:** Los controles y mensajes deberán mantener contraste suficiente para uso exterior.
- **RNF-007 Compatibilidad:** Android 10/API 29 o posterior.
- **RNF-008 Precisión:** La interfaz mostrará la incertidumbre real informada por Android y no prometerá precisión topográfica.
- **RNF-009 Actualización:** Un cambio del mapa se entregará mediante un APK nuevo firmado con la misma clave.
- **RNF-010 Datos:** La actualización de una app con el mismo identificador conservará los puntos locales.

## 5. Datos integrados

- GeoPDF de una sola página en `res/raw/luker_map.pdf`.
- Logo oficial en `res/drawable-nodpi/luker_agricola_logo.png`.
- Catálogo derivado del mapa en `assets/lots.json`.
- Identificador Android `co.geoluker.app`.

El catálogo actual contiene 177 códigos detectados. Su regeneración debe formar parte obligatoria de cada actualización del mapa para evitar búsquedas desalineadas.

## 6. Criterios de aceptación

1. La app se instala y abre el mapa sin conexión ni preparación previa.
2. El mapa continúa visible después de activar modo avión y reiniciar la app.
3. El GPS aparece dentro del mapa cuando la coordenada está cubierta.
4. La ubicación externa activa el aviso definido.
5. El rumbo cambia coherentemente al rotar el teléfono.
6. Se puede buscar y centrar un lote sin conexión.
7. Se puede crear, editar, localizar y eliminar un punto.
8. Los puntos persisten al reiniciar la app.
9. No existen pantallas, permisos, dependencias ni textos de descarga/importación de mapas.
10. Pruebas unitarias, compilación Kotlin y Android Lint finalizan correctamente.

## 7. Pruebas de campo recomendadas

- Cielo abierto, vegetación densa, cercanía a construcciones y borde de la plantación.
- Validación de lotes en varios extremos del mapa.
- Rotación con norte arriba y orientación según el teléfono.
- Pérdida y recuperación del GPS.
- Reinicio en modo avión.
- Uso prolongado para observar memoria, batería y fluidez.
- Instalación de una actualización firmada comprobando que conserve los puntos.

## 8. Limitaciones conocidas

La precisión depende del receptor GNSS, la geometría satelital y el entorno. El punto azul y su círculo representan la estimación del sistema, no una medición topográfica. El catálogo localiza el centro aproximado del texto del lote impreso en el GeoPDF; no representa un polígono oficial de sus límites.
