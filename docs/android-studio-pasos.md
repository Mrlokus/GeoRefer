# Compilar y distribuir GeoLuker desde Android Studio

## 1. Abrir el proyecto correctamente

1. Inicia Android Studio.
2. Selecciona **Open**.
3. Elige la carpeta raíz `GEOREFER`, donde están `settings.gradle.kts`, `gradlew.bat` y la carpeta `app`.
4. Espera el mensaje de sincronización finalizada.

Si Android Studio indica que falta `gradle-wrapper.properties`, comprueba que abriste la raíz del proyecto y no `GEOREFER/app`.

## 2. Seleccionar Java y sincronizar

1. Ve a **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
2. En **Gradle JDK**, selecciona el JDK integrado de Android Studio (`jbr-21`) o un JDK compatible.
3. Conserva **Distribution: Wrapper**.
4. Pulsa **Sync Project with Gradle Files**.

El proyecto compila bytecode compatible con Java 17, aunque una versión reciente de Android Studio puede ejecutar Gradle con su JDK 21 integrado.

## 3. Probar en el teléfono

1. Activa las opciones de desarrollador y la depuración USB en el teléfono.
2. Conéctalo y acepta la clave del computador.
3. Selecciona el dispositivo en la barra superior.
4. Elige la configuración `app` y pulsa **Run**.
5. Concede ubicación precisa.
6. Comprueba mosaicos al ampliar, límites de cámara, seguimiento GPS, rotación, búsqueda de lotes y búsqueda/orden de puntos.
7. Activa modo avión, cierra y vuelve a abrir GeoLuker. El mapa y los puntos deben continuar disponibles.

## 4. Verificaciones antes de distribuir

En la terminal de Android Studio ejecuta:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat compileDebugKotlin
```

- `testDebugUnitTest` ejecuta las pruebas de georreferenciación, GPS, rumbo y navegación a puntos.
- `lintDebug` busca problemas de Android, recursos, permisos y compatibilidad.
- `compileDebugKotlin` confirma que todo el código Kotlin compila sin crear el APK final.

## 5. Crear un APK firmado

1. Actualiza `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Ve a **Build > Generate Signed App Bundle or APK**.
3. Selecciona **APK**.
4. Elige tu almacén de claves existente o crea uno y guárdalo de forma segura.
5. Selecciona la variante `release`.
6. Activa las firmas V1 y V2 si Android Studio las ofrece.
7. Finaliza el asistente.

El resultado suele quedar en `app/build/outputs/apk/release/`. No publiques ni envíes el archivo de claves, sus contraseñas ni archivos `keystore.properties` al repositorio.

## 6. Actualizar el mapa

1. Sustituye `app/src/main/res/raw/luker_map.pdf` por el nuevo mapa oficial.
2. Asegúrate de que sea un GeoPDF de una página con puntos de control geográficos.
3. Regenera `app/src/main/assets/lots.json`:

```powershell
python tools/generate_lot_catalog.py app/src/main/res/raw/luker_map.pdf app/src/main/assets/lots.json
```

4. Repite las verificaciones del apartado 4.
5. Incrementa la versión y crea un APK firmado nuevo.

## 7. Instalar una actualización

Una nueva compilación con el mismo identificador `co.geoluker.app` y la misma clave de firma puede instalarse sobre la versión anterior, conservando los puntos. Si cambia la clave o el identificador, Android la tratará como otra aplicación.

La aplicación anterior `co.georefer.app` no se reemplaza automáticamente porque tiene otro identificador. Desinstálala manualmente cuando ya no se necesiten sus datos.
