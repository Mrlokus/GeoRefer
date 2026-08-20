<div align="center">

# 🌿 Georefer

### Cartografía offline para trabajo rural en Colombia

Localiza tu posición, prepara mapas antes de salir a campo y registra puntos sin depender de cobertura móvil.

[![Android](https://img.shields.io/badge/Android-10%2B-174F3D?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-49695B?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-D79B32?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![MapLibre](https://img.shields.io/badge/MapLibre-Offline-2D75D5?style=for-the-badge)](https://maplibre.org/)

<sub>Offline first · Sin cuentas · Datos guardados localmente · Diseñada para técnicos de campo y agricultores</sub>

</div>

---

## ¿Qué es Georefer?

Georefer es una aplicación Android de orientación cartográfica para entornos rurales. Permite descargar previamente una zona de Colombia, trabajar sin Internet, visualizar la posición GNSS del teléfono y guardar puntos de interés directamente sobre el mapa.

La interfaz utiliza la identidad visual **Bosque Andino**: limpia, profesional y pensada para conservar legibilidad bajo luz solar o durante jornadas prolongadas.

> [!IMPORTANT]
> El repositorio contiene el código fuente. El APK debe generarse desde Android Studio y distribuirse de forma privada por el responsable del proyecto.

## Funciones principales

| Área | Capacidades |
|---|---|
| 🗺️ **Mapas offline** | Descarga por cualquiera de los 32 departamentos, Bogotá D. C. o un rectángulo seleccionado manualmente. |
| 🛰️ **Vistas cartográficas** | Rural, Cartográfica, Minimalista, Alto contraste y Satélite 2025. |
| 📍 **Posicionamiento GNSS** | Coordenadas WGS 84, precisión real, círculo de incertidumbre y satélites usados/visibles. |
| 🧭 **Orientación** | Rotación e inclinación del mapa, norte arriba y orientación según el rumbo del teléfono. |
| 📌 **Puntos de campo** | Crear mediante pulsación prolongada, editar, eliminar, centrar en el mapa y calcular distancia y rumbo. |
| 🔄 **Intercambio de datos** | Importación y exportación de puntos en GeoJSON, KML, GPX y CSV. |
| 📄 **GeoPDF** | Importación desde el almacenamiento interno, lectura de georreferenciación y visualización offline. |
| 💾 **Almacenamiento** | Estimación previa de tamaño, protección del espacio libre, pausa, reanudación e integridad de descargas. |
| 🔒 **Privacidad** | Sin inicio de sesión, sin nube propia y sin respaldo de los datos internos de la aplicación. |

## Experiencia de campo

```text
Con Internet                    Sin cobertura
────────────                    ─────────────
Elegir zona                     Abrir mapa guardado
Seleccionar vista       ───▶    Ubicarse con GNSS
Descargar recursos              Rotar y ampliar
                                Registrar puntos
                                Consultar distancia y rumbo
```

El posicionamiento GNSS continúa funcionando sin datos móviles. Internet solo es necesario para descargar inicialmente los mapas o consultar recursos cartográficos que todavía no estén almacenados.

## Tipos de mapa

- **Rural:** vías, caminos, predios y referencias útiles para trabajo de campo.
- **Cartográfico:** poblaciones, vías y límites con lectura equilibrada.
- **Minimalista:** reduce elementos visuales para destacar ubicación y puntos.
- **Alto contraste:** vista oscura para condiciones de poca luz.
- **Satélite 2025:** mosaico Sentinel-2 sin nubes, destinado exclusivamente a uso no comercial.
- **GeoPDF local:** mapas georreferenciados aportados por el usuario desde el teléfono.

Las descargas se almacenan dentro de la aplicación. Antes de comenzar, Georefer calcula un tamaño aproximado, comprueba el espacio disponible y conserva una reserva mínima para no saturar el dispositivo.

## Puntos y formatos compatibles

Mantén presionada una ubicación para crear un marcador. Cada punto puede incluir nombre y nota, y permanece únicamente en el teléfono hasta que decidas exportarlo.

| Formato | Importar | Exportar | Uso recomendado |
|---|:---:|:---:|---|
| GeoJSON | ✅ | ✅ | Sistemas de información geográfica y aplicaciones web. |
| KML | ✅ | ✅ | Google Earth y herramientas cartográficas. |
| GPX | ✅ | ✅ | Navegadores y dispositivos GPS. |
| CSV | ✅ | ✅ | Hojas de cálculo y procesamiento tabular. |

Al importar, la aplicación valida coordenadas y evita agregar nuevamente puntos con la misma ubicación.

## Requisitos

- Android Studio con JDK integrado.
- Android SDK 35.
- Java/Kotlin configurado con destino JVM 17.
- Dispositivo con Android 10 o posterior (`minSdk 29`).
- Receptor GPS/GNSS para posicionamiento en campo.
- Internet únicamente para la primera sincronización de Gradle y la descarga de mapas.

## Inicio rápido

1. Clona el repositorio:

   ```bash
   git clone https://github.com/Mrlokus/GeoRefer.git
   cd GeoRefer
   ```

2. Abre en Android Studio la **carpeta raíz `GeoRefer`**, no la carpeta `app`.
3. Espera a que termine **Gradle Sync**.
4. Verifica que el SDK 35 esté instalado.
5. Conecta un dispositivo Android con depuración USB.
6. Selecciona la configuración `app` y pulsa **Run**.

La guía detallada de configuración, ejecución y firma está en [docs/android-studio-pasos.md](docs/android-studio-pasos.md).

## Verificación del proyecto

En Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

En macOS o Linux:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Estado verificado del proyecto:

- Compilación Kotlin y recursos: ✅
- Pruebas unitarias: **25 aprobadas**
- Android Lint: **0 errores**
- Generación automática de APK: no incluida

## Arquitectura

```text
app/src/main/java/co/georefer/app/
├── location/       Lecturas GPS, GNSS, precisión y satélites
├── map/            GeoPDF, regiones offline y estilos cartográficos
├── orientation/    Rumbo magnético, norte verdadero y sensores
├── points/         Persistencia e intercambio de puntos
├── settings/       Preferencias locales del mapa
└── ui/             Pantallas y componentes Jetpack Compose
```

### Tecnologías

- **Kotlin** y **Coroutines/StateFlow** para estado reactivo.
- **Jetpack Compose + Material 3** para la interfaz.
- **MapLibre Native** para navegación y regiones cartográficas offline.
- **PDFBox Android + PdfRenderer** para lectura y renderizado de GeoPDF.
- **SharedPreferences** para preferencias y puntos locales.
- **JUnit 4** para pruebas unitarias.

## Principios del proyecto

- **Offline first:** las funciones esenciales deben operar sin cobertura.
- **Datos bajo control del usuario:** puntos y mapas permanecen en el dispositivo.
- **Precisión honesta:** la aplicación muestra la incertidumbre reportada por Android; no inventa una exactitud mayor.
- **Interoperabilidad:** los datos de campo pueden salir en formatos abiertos.
- **Simplicidad:** no hay cuentas, perfiles ni pasos innecesarios antes de abrir el mapa.

## Limitaciones actuales

- Los departamentos se descargan usando su rectángulo envolvente, no el polígono administrativo exacto.
- La selección por municipio todavía no está implementada.
- Los GeoPDF están limitados a una página, un máximo de 250 MB y referencias compatibles con `BBox`, `LPTS` y `GPTS`.
- La cantidad máxima preventiva es de 6.000 teselas por región.
- La precisión final depende del receptor, el cielo visible, la vegetación y las condiciones del terreno.

## Fuentes cartográficas y atribuciones

- Las vistas vectoriales utilizan estilos de **OpenFreeMap** y datos de **OpenStreetMap/OpenMapTiles**, sujetos a sus respectivas atribuciones y condiciones de uso.
- La vista **Satélite 2025** utiliza **EOxCloudless**, por EOX IT Services GmbH, e incluye datos Copernicus Sentinel modificados de 2025. Su uso dentro de este proyecto se plantea como **no comercial** y requiere conservar la atribución correspondiente.
- Las envolventes departamentales se basan en el Marco Geoestadístico Nacional integrado 2018 del **DANE**.

Antes de distribuir la aplicación, revisa las condiciones vigentes de cada proveedor de mapas y conserva las atribuciones visibles incluidas en la interfaz.

## Hoja de ruta

- [ ] Selección por municipio.
- [ ] Recorte exacto por polígonos administrativos.
- [ ] Curvas de nivel y capas rurales adicionales con licencia offline compatible.
- [ ] Compatibilidad ampliada con GeoPDF multipágina.
- [ ] Paquetes cartográficos propios para distribución a mayor escala.
- [ ] Pruebas instrumentadas de interfaz y flujo completo en dispositivo.

## Documentación

- [Guía paso a paso para Android Studio](docs/android-studio-pasos.md)
- [Especificación y requerimientos del producto](docs/especificacion-requisitos-georefer.md)

## Licencia

Este repositorio no incluye actualmente un archivo de licencia de software. Hasta que se agregue uno, no se conceden automáticamente permisos de redistribución o modificación pública. Las fuentes cartográficas y los datos de terceros conservan sus propias licencias y condiciones.

---

<div align="center">

**Georefer** · Orientación clara cuando la señal deja de acompañarte.

</div>
