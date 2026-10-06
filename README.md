    Unext - Plataforma de Empleabilidad Universitaria (Backend API)
Unext es una plataforma integral diseñada para conectar a tres actores fundamentales en el ecosistema laboral y académico: Estudiantes (Postulantes), Instituciones Educativas (Universidades/Institutos) y Empresas (Reclutadores).

Este repositorio contiene el backend de la aplicación, construido bajo una arquitectura RESTful robusta, con seguridad basada en roles y flujos de negocio complejos para la gestión de prácticas pre-profesionales y reclutamiento temprano.

    🚀 Características Principales por Rol
El sistema utiliza Spring Security y JWT para aislar las funcionalidades de cada segmento a través de Control de Acceso Basado en Roles (RBAC).

    🎓 1. Segmento Institución (INSTITUTION)
- Validación Académica: Verificación oficial de estudiantes que pertenecen a la institución.

- Gestión de Convenios de Prácticas: Revisión, aprobación o rechazo de solicitudes de convenios generadas por empresas para sus estudiantes.

- Insignias de Confianza (Endorsements): Capacidad de otorgar y retirar el estatus de "Empresa Aliada" a reclutadores verificados.

- Dashboard Analítico: Panel de estadísticas en tiempo real con métricas como la tasa de empleabilidad de sus alumnos, total de convenios activos y alumnos contratados.


    🏢 2. Segmento Empresa (RECRUITER)
- Gestión de Vacantes (Job Offers): Creación, actualización y publicación de ofertas laborales.

- Solicitud de Convenios: Generación de propuestas formales de prácticas para estudiantes, enviadas automáticamente a la institución del alumno para su evaluación.

- Directorio de Talento: Acceso a un motor de búsqueda de estudiantes, priorizando perfiles que cuentan con validación institucional oficial.


    👨‍💻 3. Segmento Estudiante (POSTULANT)
- Gestión de Perfil y Habilidades: Creación de portafolio profesional, gestión de ciclo académico y registro de habilidades técnicas con niveles de dominio.

- Postulaciones Laborales: Flujo de aplicación a vacantes publicadas por las empresas.

- Chat Restringido y Contextual: Comunicación directa con reclutadores (habilitado solo si hay una postulación en curso) y con su propia Institución (habilitado solo si la institución ya aprobó su vínculo académico).


    ⚙️ 4. Módulos Transversales
- Sistema de Notificaciones Automáticas: Gatillos en tiempo real para eventos clave (cambios de estado en convenios, nuevas postulaciones, otorgamiento de insignias).

- Mensajería Directa (Chat Contextual): Motor de chat interno que diferencia entre consultas generales y mensajes específicos ligados a una postulación laboral.


    🛠️ Stack Tecnológico
- Lenguaje: Java 21

- Framework: Spring Boot 3.x

- Seguridad: Spring Security + JSON Web Tokens (JWT)

- Persistencia de Datos: Spring Data JPA / Hibernate

- Base de Datos: PostgreSQL

- Herramientas Útiles: Lombok, Bean Validation

    
    📂 Estructura del Proyecto
El código está organizado siguiendo los principios de separación de responsabilidades y agupación por dominio de negocio:

...


    ⚙️ Configuración y Despliegue (Local)
1. Requisitos Previos
   JDK 17 o superior instalado.

   - PostgreSQL 14 o superior ejecutándose localmente.

   - Maven para la gestión de dependencias.

2. Variables de Entorno (application.properties)
   - Configura tus credenciales de base de datos y la llave secreta JWT en src/main/resources/application.properties:

3. Ejecución
   - Puedes levantar el proyecto desde tu IDE ejecutando la clase principal UnextWebServicesApplication.java
   - El servidor estará disponible en http://localhost:8080

    
    🔒 Autorización y Pruebas en API Clientes (Postman)
Todos los endpoints (excepto /api/auth/**) están protegidos mediante tokens Bearer. Para realizar pruebas:

- Haz un POST a /api/auth/login con tus credenciales.
- Copia el token JWT de la respuesta.
- En tus peticiones subsecuentes, ve a la pestaña Authorization, selecciona Bearer Token y pega el token.