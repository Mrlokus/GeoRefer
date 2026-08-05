# Abrir, probar y generar Georefer en Android Studio

Este proyecto se entrega como código fuente. No se ha compilado ni generado ningún APK; tú realizarás esas acciones desde Android Studio siguiendo esta guía.

## 1. Abrir el proyecto

1. Inicia Android Studio.
2. Selecciona **Open**.
3. Abre la carpeta raíz `C:\Users\Daniel\Desktop\GEOREFER` (no abras únicamente la carpeta `app`).
4. Si Android Studio pregunta si confías en el proyecto, selecciona **Trust Project**.
5. Espera a que termine **Gradle Sync**.

La primera sincronización puede descargar dependencias y requiere conexión a Internet. Los mapas de trabajo seguirán siendo offline cuando se implemente el motor cartográfico.

## 2. Revisar el JDK y el SDK

1. Abre **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
2. En **Gradle JDK**, elige el JDK integrado de Android Studio, versión 21.
3. Abre **Tools > SDK Manager**.
4. Confirma que están instalados:
   - Android SDK Platform 35.
   - Android SDK Build-Tools 35.0.0 o posterior.
   - Android SDK Platform-Tools.
5. Pulsa **Sync Project with Gradle Files** si realizaste algún cambio.

Si aparece el mensaje **SDK location not found**, abre el archivo `local.properties` de la raíz y verifica que contenga:

```properties
sdk.dir=C\:\\Users\\Daniel\\AppData\\Local\\Android\\Sdk
```

## 3. Preparar el POCO X8 Pro Max

1. En el teléfono, abre **Ajustes > Acerca del teléfono**.
2. Pulsa repetidamente la versión de HyperOS hasta activar las opciones de desarrollador.
3. Abre **Ajustes adicionales > Opciones de desarrollador**.
4. Activa **Depuración USB**.
5. Conecta el teléfono al computador y acepta su huella RSA cuando aparezca el aviso.
6. En Android Studio, selecciona el POCO en la lista de dispositivos.

## 4. Ejecutar la aplicación

1. Selecciona la configuración `app`.
2. Pulsa **Run**.
3. En Georefer, entra en **Mapa** y pulsa **Activar ubicación**.
4. Concede **Ubicación precisa** y elige permitirla mientras usas la aplicación.
5. Sal al exterior y espera el primer posicionamiento GNSS.

El semáforo usa estas reglas:

| Estado | Regla |
|---|---|
| Buscando | Sin lectura o lectura con más de 10 segundos |
| Baja | Precisión mayor de 15 m |
| Aceptable | Precisión mayor de 5 m y hasta 15 m |
| Buena | Precisión de hasta 5 m |

Se necesitan dos lecturas consecutivas para mejorar de estado. Una degradación se muestra inmediatamente.

## 5. Pruebas manuales recomendadas

1. Abre la app sin conceder permisos: no debe solicitar ubicación automáticamente.
2. Concede solo ubicación aproximada: debe mostrar **Ubicación aproximada**.
3. Concede ubicación precisa: debe iniciar el estado **Buscando señal**.
4. Desactiva la ubicación del teléfono: debe mostrar **GPS desactivado**.
5. Activa modo avión manteniendo el GPS encendido: la posición debe seguir funcionando.
6. Lleva la aplicación al fondo: debe dejar de solicitar lecturas.
7. Prueba a cielo abierto, bajo vegetación y junto a una construcción.
8. Verifica que la aplicación muestre siempre la precisión real informada por Android.

## 6. Ejecutar las pruebas unitarias

Desde Android Studio:

1. Abre `app/src/test/java/co/georefer/app/location/GpsQualityClassifierTest.kt`.
2. Pulsa el icono de ejecución junto al nombre de la clase.
3. Confirma que todas las pruebas estén en verde.

También puedes usar la ventana **Gradle** y ejecutar `app > Tasks > verification > testDebugUnitTest`.

## 7. Generar un APK de prueba

1. Abre **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Según la versión de Android Studio, el menú puede llamarse **Generate App Bundles or APKs > Generate APKs**.
2. Espera el mensaje de compilación finalizada.
3. Pulsa **Locate** para abrir la carpeta del archivo.

La ruta habitual es:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 8. Generar un APK firmado para distribuir

1. Abre **Build > Generate Signed App Bundle or APK**.
2. Selecciona **APK**.
3. Crea o selecciona un archivo de claves seguro.
4. Guarda la contraseña fuera del proyecto y conserva una copia protegida de la clave.
5. Selecciona la variante `release`.
6. Genera el APK y pruébalo en el POCO antes de distribuirlo.

No pierdas la clave de firma: las futuras versiones deberán firmarse con la misma clave para instalarse como actualización.
