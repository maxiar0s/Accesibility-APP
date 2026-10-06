# Generar un APK firmado desde Android Studio

El APK firmado **no se ha generado** en esta unidad. Estos pasos son para que el usuario lo genere localmente con su propia clave; no es necesario compartir claves ni contraseñas en el chat.

## Pasos

1. Abre el proyecto en Android Studio y selecciona **Build → Generate Signed App Bundle or APK**.
2. Selecciona **APK** y continúa con **Next**.
3. Elige la clave existente que corresponda a esta aplicación. Si es una primera distribución y no existe una clave, usa **Create new** y guarda el almacén fuera del repositorio. Conserva una copia segura: necesitarás la misma clave para futuras actualizaciones.
4. Introduce el alias y las contraseñas únicamente en el asistente de Android Studio. No los incluyas en código, Gradle, archivos del proyecto, capturas ni Git.
5. Selecciona la variante **release** y el destino del archivo; finaliza la generación. Consulta la notificación del IDE para localizar el APK generado y comprueba que la operación terminó correctamente.

## Precauciones

- Una clave release diferente de la firma debug instalada puede impedir actualizar esa instalación. No desinstales ni borres datos para evitar el error sin evaluar su pérdida; para una actualización compatible necesitas el mismo identificador y firma que la aplicación instalada.
- `.gitignore` excluye preventivamente `*.jks`, `*.keystore` y `keystore.properties`; esto no protege claves ya rastreadas ni otros nombres. Mantén las claves fuera del repositorio y fuera del ZIP de entrega.
- No se configura firma con contraseñas en texto plano. El asistente realiza la firma bajo control del usuario.
- Compilar y probar debug no valida una firma release, instalación, audio real ni publicación. La instalación y la comprobación del APK firmado son pasos posteriores del usuario.
