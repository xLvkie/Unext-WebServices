Instrucciones para el Equipo de Desarrollo: Módulo de Autenticación Unext

El repositorio en GitHub ya cuenta con las dependencias de JWT en el pom.xml, la configuración de la conexión a PostgreSQL y todas las entidades de la base de datos mapeadas. El objetivo de este siguiente bloque de trabajo es habilitar los endpoints de registro e inicio de sesión para comenzar las pruebas oficiales en Postman.

Contexto del Sistema y Flujo de Usuarios
La plataforma Unext está diseñada para atender a tres segmentos principales. Aunque todos comparten el mismo núcleo de autenticación, la experiencia y permisos cambian drásticamente una vez dentro del sistema:

Postulante: Crea y configura su perfil para navegar por el dashboard, buscar empleo, gestionar mensajería/notificaciones, subir su CV y solicitar convalidaciones académicas a su universidad.
Empleador (Reclutador): Mantiene una interfaz similar de navegación, pero orientada a visualizar la competencia, crear vacantes laborales y obtener indicadores de "Empresa Convalidada" según las aprobaciones institucionales.
Institución: Accede a un panel para visualizar estadísticas de sus estudiantes y cuenta con un apartado exclusivo para aprobar o rechazar las prácticas y conocimientos académicos de sus alumnos.

Todos los segmentos ingresan por la misma pasarela de autenticación. El token JWT que devuelva nuestro backend será el encargado de indicarle al frontend (React) qué interfaz renderizar y si el usuario debe ser redirigido obligatoriamente a completar su perfil (isProfileCompleted).

Pasos de Implementación en IntelliJ

Creación de DTOs (Data Transfer Objects):
Dentro del paquete dto, implementen las clases que estructurarán los JSON de entrada y salida. Esto evita exponer nuestras entidades de base de datos directamente al cliente.
RegisterRequestDTO: Estructura para recibir email, password y role. Asegúrense de usar las anotaciones de validación (@NotBlank, @Email).
LoginRequestDTO: Estructura para recibir únicamente email y password.
AuthResponseDTO: Estructura de respuesta que devolverá el token JWT generado, el userId, el role y la bandera booleana isProfileCompleted.

Configuración de Seguridad (Generación de Tokens):
En el paquete security, desarrollen la clase JwtUtil. Esta clase se encargará de firmar digitalmente los tokens y validarlos. Debe extraer la clave secreta (jwt.secret) y el tiempo de expiración (jwt.expiration) que ya están definidos en el archivo application.properties.

Lógica de Negocio (AuthService):
Dentro del paquete service, construyan el servicio que procese las peticiones de autenticación.

Para el registro: Validen que el correo no exista ya en la base de datos, encripten la contraseña entrante (configurando un bean de BCryptPasswordEncoder) y guarden el nuevo registro en la tabla users.
Para el login: Busquen al usuario por correo mediante el repositorio, comparen la contraseña ingresada con el hash de la base de datos y, si coinciden, utilicen JwtUtil para generar y devolver el token.

Exposición de Endpoints (AuthController):
En el paquete controller, creen el controlador REST con la ruta base /api/auth.
Mapeen POST /api/auth/register para recibir el RegisterRequestDTO.
Mapeen POST /api/auth/login para recibir el LoginRequestDTO y retornar el AuthResponseDTO al cliente.

Pruebas Esperadas en Postman
Una vez que el servidor de Spring Boot recompile y arranque sin errores, el equipo debe abrir Postman y certificar el flujo completo:
Ejecutar un POST a http://localhost:8080/api/auth/register enviando un JSON con un correo, contraseña y rol (ej. POSTULANT). Validar que PostgreSQL guarde el registro.
Ejecutar un POST a http://localhost:8080/api/auth/login con las credenciales creadas. El backend debe responder exitosamente entregando el JSON con el token JWT.
