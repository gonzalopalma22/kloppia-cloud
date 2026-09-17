# KloppIA Backend

Backend de **KloppIA**, una plataforma académica que permite a estudiantes organizar sus materias, subir apuntes en PDF y obtener resúmenes automáticos generados con inteligencia artificial (Google Gemini).

Arquitectura de microservicios en **Spring Boot**, con autenticación delegada a **Azure Active Directory (Microsoft Entra ID)** como proveedor de identidad (IDaaS), desplegado en instancias **AWS EC2** y expuesto mediante **AWS API Gateway**.

---

## 🏗️ Arquitectura

```
Cliente (React + MSAL)
   │  Authorization: Bearer <JWT emitido por Azure AD>
   ▼
┌─────────────────────────┐
│   AWS API Gateway        │  ◄──── Único punto de entrada público
│  (kloppia-api-gateway)   │        Enruta hacia cada microservicio
└────────┬─────────────────┘
         │
   ┌─────┴──────────┐
   ▼                ▼
┌──────────────┐  ┌──────────────┐
│materia-service│  │apunte-service│
│  EC2 :8082    │  │  EC2 :8083   │
│  Valida JWT   │  │  Valida JWT  │
└──────┬────────┘  └──────┬───────┘
       │                  │
       └────────┬─────────┘
                ▼
       PostgreSQL (Supabase)
```

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `materia-service` | 8082 | CRUD de materias del usuario autenticado |
| `apunte-service` | 8083 | Subida de PDFs, resumen con Gemini, flashcards y chat contextual |

> **Nota:** este proyecto ya no incluye un `auth-service` ni un API Gateway propio (Spring Cloud Gateway). La autenticación es responsabilidad de **Azure AD**, y el enrutamiento hacia los microservicios lo realiza **AWS API Gateway**. Cada microservicio valida el token JWT de forma independiente.

---

## 🛠️ Stack Tecnológico

- **Java 17** + **Spring Boot 3.5**
- **Spring Security** + **OAuth2 Resource Server** — validación de JWT emitido por Azure AD
- **Spring Data JPA** — persistencia con Hibernate
- **PostgreSQL** alojado en **Supabase**
- **Google Gemini API** — resúmenes, flashcards y chat sobre PDFs
- **Lombok**
- **JUnit 5 + Mockito** — pruebas unitarias de la capa de servicio
- **AWS EC2** — despliegue de cada microservicio
- **AWS API Gateway (HTTP API)** — enrutamiento y punto de entrada único

---

## 📋 Prerrequisitos

| Herramienta | Versión mínima |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ (incluido como `mvnw`) |
| Cuenta de Azure AD | con una App Registration para la API (ver sección Configuración) |

---

## ⚙️ Configuración del entorno

Cada microservicio requiere las siguientes variables de entorno:

```env
# Base de datos (PostgreSQL / Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=<usuario>
SPRING_DATASOURCE_PASSWORD=<contraseña>

# Solo apunte-service:
GEMINI_API_KEY=<tu_api_key_de_gemini>
```

Y en `application.properties` de cada servicio (no son secretos, son identificadores públicos):

```properties
azure.tenant-id=<tenant-id-de-tu-azure-ad>
azure.client-id=<client-id-de-la-app-api-registrada-en-azure>
```

> ⚠️ **Nunca subas archivos `.env` al repositorio.** Están incluidos en `.gitignore`.

### Configuración previa en Azure AD

Antes de ejecutar el backend, debes tener:

1. Una **App Registration** para el frontend (tipo SPA).
2. Una **App Registration** para la API, con:
   - Un scope expuesto (`access_as_user`) bajo "Expose an API".
   - **App Roles** definidos (`ADMIN`, `USER`) bajo "App roles".
   - El campo `requestedAccessTokenVersion` en `2` dentro del manifiesto (para que emita tokens v2.0).
3. Usuarios asignados a los roles correspondientes en **Enterprise Applications → Users and groups**.

---

## 🚀 Ejecución local

Cada servicio se levanta de forma independiente:

```bash
# materia-service
cd materia-service
export SPRING_DATASOURCE_URL=...
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...
./mvnw spring-boot:run

# apunte-service (en otra terminal)
cd apunte-service
export SPRING_DATASOURCE_URL=...
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...
export GEMINI_API_KEY=...
./mvnw spring-boot:run
```

En Windows, reemplaza `./mvnw` por `mvnw.cmd`.

---

## ☁️ Despliegue en AWS

1. **EC2**: se aprovisiona una instancia Ubuntu Server (t3.micro) por microservicio. En cada una se instala Java 17, se clona el repositorio, se compila con `mvn clean package -DskipTests` y se ejecuta en segundo plano:
   ```bash
   nohup java -jar target/materia-service-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
   ```
2. **Security Group**: se habilita el puerto correspondiente a cada servicio (8082 / 8083) y el puerto 22 para SSH.
3. **API Gateway**: se crea una HTTP API con rutas hacia cada EC2:
   - `/api/materias` y `/api/materias/{proxy+}` → `materia-service`
   - `/api/materias/{materiaId}/apuntes` y `/api/materias/{materiaId}/apuntes/{proxy+}` → `apunte-service`

   Cada integración de tipo proxy reconstruye la ruta completa hacia el backend (incluyendo variables de path como `{materiaId}`).

---

## 📡 Endpoints de la API

Todos los endpoints requieren el header `Authorization: Bearer <token>`, obtenido tras autenticarse con Azure AD desde el frontend.

### `materia-service` — `/api/materias`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/materias` | Listar materias del usuario autenticado |
| `POST` | `/api/materias` | Crear una materia |
| `PUT` | `/api/materias/{id}` | Editar nombre/descripción |
| `DELETE` | `/api/materias/{id}` | Eliminar una materia |
| `GET` | `/api/materias/{id}` | Obtener una materia por ID |

### `apunte-service` — `/api/materias/{materiaId}/apuntes`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/materias/{materiaId}/apuntes` | Subir PDF (genera resumen con IA) |
| `GET` | `/api/materias/{materiaId}/apuntes` | Listar apuntes de una materia |
| `GET` | `/api/materias/{materiaId}/apuntes/{id}` | Obtener un apunte por ID |
| `PUT` | `/api/materias/{materiaId}/apuntes/{id}` | Editar título |
| `DELETE` | `/api/materias/{materiaId}/apuntes/{id}` | Eliminar un apunte |
| `GET` | `.../apuntes/{id}/flashcards?cantidad=10` | Generar flashcards del resumen |
| `GET` | `.../apuntes/{id}/chat/historial` | Obtener historial de chat |
| `POST` | `.../apuntes/{id}/chat` | Enviar pregunta sobre el apunte |

---

## 🔐 Flujo de seguridad

1. El usuario se autentica en el **frontend** contra **Azure AD**, mediante MSAL.
2. Azure AD emite un **JWT (access_token)** firmado, con claims como `iss`, `aud`, `roles`, `sub` y `exp`.
3. El frontend adjunta ese token en cada solicitud mediante el header `Authorization: Bearer <token>`.
4. La solicitud llega a **AWS API Gateway**, que la enruta hacia la EC2 correspondiente.
5. Cada microservicio valida el token con **Spring Security + OAuth2 Resource Server**:
   - Verifica la **firma** contra el JWKS público de Azure AD.
   - Verifica el **issuer** (`https://login.microsoftonline.com/{tenant}/v2.0`).
   - Verifica la **audiencia** (`aud`) contra el client ID de la API.
   - Verifica la **vigencia** (`exp`, `nbf`).
   - Mapea el claim `roles` a autoridades de Spring Security (`ROLE_ADMIN`, `ROLE_USER`).
6. Si el token es inválido o está ausente, responde **401 Unauthorized**. Si el usuario no tiene el rol requerido, responde **403 Forbidden**.

> La gestión de usuarios y la asignación de roles se realiza **directamente en Azure AD** (Enterprise Applications → Users and groups), no dentro de esta aplicación.

---

## 🌐 CORS

El manejo de CORS se realiza **directamente en cada microservicio** (Spring Security), no en el API Gateway, mediante un `CorsConfigurationSource` que define el origen permitido, métodos y cabeceras aceptadas. Las solicitudes `OPTIONS` (preflight) están explícitamente permitidas sin autenticación.

---

## 🧪 Tests

```bash
cd materia-service && ./mvnw test
cd apunte-service && ./mvnw test
```

Cada microservicio incluye tests con **JUnit 5** y **Mockito** que cubren creación, listado, obtención, edición y eliminación de recursos, además de la validación de que un usuario no pueda acceder a recursos de otro usuario.

---

## 🗂️ Estructura del proyecto

```
kloppia-backend/
├── .env                          # Variables de entorno (no subir al repo)
├── materia-service/
│   ├── pom.xml
│   └── src/main/java/com/klopp/materia_service/
│       ├── controller/       # MateriaController
│       ├── dto/              # MateriaDTO, MateriaResponseDTO
│       ├── model/            # Materia
│       ├── repository/       # MateriaRepository
│       ├── security/         # SecurityConfig (validación JWT Azure AD + CORS)
│       └── service/          # MateriaService
└── apunte-service/
    ├── pom.xml
    └── src/main/java/com/klopp/apunte_service/
        ├── controller/       # ApunteController
        ├── dto/              # ApunteDTO, ApunteResponseDTO, FlashcardDTO, ChatRequestDTO/ChatResponseDTO
        ├── model/            # Apunte, Chat
        ├── repository/       # ApunteRepository, ChatRepository
        ├── security/         # SecurityConfig (validación JWT Azure AD + CORS)
        └── service/          # ApunteService, GeminiService
```

---


## 📄 Licencia

© 2026 [gonzaloPalma22](https://github.com/gonzaloPalma22). Todos los derechos reservados.
