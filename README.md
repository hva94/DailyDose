# Snapshots

Basado en el proyecto 3 del curso de Curso de Android 12 con Kotlin: Intensivo y práctico 2022
Disponible en https://www.udemy.com/course/kotlin-intensivo/

Primer paquete de implementaciones y mejoras

*LISTO - Validación para no poder eliminar fotos ajenas
(Se ocultó el botón "Borrar" para fotos ajenas al mismo usuario, 
sólo será posible eliminar las fotos subidas por el mismo usuario).

-Modificación de la función "onBindViewHolder" para validar si es el 
mismo usuario (HomeFragment.kt) 
-Modificación de la función "saveSnapshot" para incluir el Id del usuario
que subió la imagen (AddFragment.kt)
-Modificación de la base de datos para agregar el "idUser"(Snapshot.kt)


*LISTO - Hacer zoom a las imágenes
(Se puede hacer "double tap" para hacer zoom a las publicaciones).

-Modificación del layout original, ImageView por TouchImageView (item_snapshot.xml)
-Implementación de la dependencia externa "TouchImageView" (build.gradle)
-Implementación del repositorio "maven { url 'https://jitpack.io' }" (settings.gradle)


*LISTO - Nombre e imagen de perfil para usuarios.
(Se implementa botón para añadir una foto de perfil y 
es mostrada junto con su nombre en el Fragment Home)

-Creación data class SnapshotUser (SnapshotUser.kt)
-Adición del PATH_SNAPSHOTS_USERS (SnapshotsApplication.kt)
-Se modificó el layout del fragmento Perfil (fragment_profile.xml)
-Se modificó el layout del fragmento item_snapshot (item_snapshot.xml)
-Se modificaron y añadieron nuevas variables y funciones (ProfileFragment.kt)
-Modificación de la función "onBindViewHolder" para mostrar 
los datos (HomeFragment.kt) 


*LISTO - Mejoras visuales y de usabilidad
(Se mejora la apariencia del inicio de sesión, se implementan 
otras mejoras estéticas y de lenguaje Kotlin).

-Se adañe una validación para que el usuario ingrese su foto de perfil antes de 
su primera publicación. (AddFragment.kt)
-Se oculta el botón Seleccionar para que no se vea cuándo el usuario
ya cargó una imagen en la func. selectImageResult (AddFragment.kt)
-Creación del tema para el login (themes.xml | night\themes.xml)
-Creación del banner icon para el login (ic_banner.png | ic_banner.xml)
-Adición de las propiedades nuevas en la función "setupAuth" (MainActivity.kt)
-Cambio de color en íconos varios para mejorar su vista "dark mode".
(ic_delete.xml | ic_image_search.xml)
-Bloqueo de orientación de la pantalla para mejorar la UserX
screenOrientation en el Manifest y setLockOrientation en la func. setupAuth.
(AndroidManifest.xml | MainActivity.kt)


---------------------------------------------------------------

*EN DESARROLLO (PENDIENTE) - Tomar una foto desde la app
-Se incluirá la opción múltiple al presionar el botón para elegir imagen,
por el momento muestra un SnackBar con un mensaje del desarrollador
-Se logró abrir la cámara, tomar la foto, crear el archivo, pero aún no 
se puede cargar la URI, por lo que se comentó todo el código relacionado
-Mejora pendiente... (Corrutinas por revisar)



Más mejoras pendientes...
*Disminuir y/o limitar el peso de las imágenes subidas
*Cambiar el ícono del Toast predeterminado
*Agregar action floating option para Crear
