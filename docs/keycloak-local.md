# Keycloak local setup

## Start infrastructure

From the project directory, start the development stack:

```powershell
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
```

Services:

- Keycloak: `http://localhost:8180`
- Keycloak Admin Console: `http://localhost:8180/admin`
- Redis: `localhost:6379` when the optional Compose profile is enabled

The Keycloak master admin credentials are `admin` / `admin`. These credentials are for local development only.

If Redis is not already running locally, start it together with Keycloak:

```powershell
docker compose --profile redis up -d
```

## Realm configuration

The development overlay imports realm `my-workflow-dev`; running only `compose.yaml` uses realm `my-workflow`. The application client is `workflow-frontend` in both realms.

The client supports:

- Authorization Code flow with PKCE for the Angular frontend.
- Direct Access Grant for the existing `/api/auth/login` compatibility endpoint.

Local realm users:

| Username | Password | Role |
| --- | --- | --- |
| `admin-local` | `admin123` | `ADMIN` |
| `manager-local` | `manager123` | `MANAGER` |
| `staff-local` | `staff123` | `STAFF` |
| `user-local` | `user123` | `USER` |

The seeded Keycloak users are useful for testing tokens. The `manager-local` seed does not have a MySQL business profile, so use the bootstrap manager below for application workflows.

## Development manager account

When the backend starts with manager bootstrap enabled, it creates a manager in the active Keycloak realm, assigns the `MANAGER` realm role, and stores the matching business profile and cart in MySQL. The first available username is `manager`; if the shared MySQL database already contains it, the backend tries `manager1`, `manager2`, and so on. The development password defaults to `manager123`.

The backend marks the created Keycloak identity and reuses it after restarts. If each cloned machine has its own Keycloak but connects to the same MySQL database, each machine receives a unique username while keeping its local Keycloak identity. After both saves succeed, the backend logs `[MANAGER-BOOTSTRAP] TẠO TÀI KHOẢN THÀNH CÔNG` with the selected username and role, never the password. Watch it with `docker compose -f compose.yaml -f compose.dev.yaml logs -f backend`.

Bootstrap is enabled by default in the development configuration. `compose.prod.yaml` disables it by default. Set `KEYCLOAK_BOOTSTRAP_MANAGER_PASSWORD` to a private value when changing the development password. Do not use the documented development password in a public or production environment.

## Start the backend

The full Compose command above already starts the backend. To run it from the IDE instead, start only its infrastructure first; Keycloak must be running because the resource server reads realm metadata at startup.

```powershell
docker compose -f compose.yaml -f compose.dev.yaml up -d keycloak redis mongo
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

If the Maven wrapper is not present:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Register an application user

`POST /api/users` creates the identity in Keycloak and the business profile/cart in MySQL. New public registrations always receive the `USER` role.

The password is stored only in Keycloak. MySQL keeps a random BCrypt placeholder because the existing schema requires a non-null password column.

Existing MySQL users are not automatically migrated because their original passwords cannot be recovered from BCrypt hashes. Re-register them in local development or create matching Keycloak users manually.

## Login through the compatibility endpoint

```powershell
$body = @{
  username = "registered-user"
  password = "secret123"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/auth/login `
  -ContentType application/json `
  -Body $body

$token = $response.data.accessToken
```

The returned token is issued and signed by Keycloak. Call protected APIs with:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8080/api/orders/user/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

## Obtain a seeded role token directly

```powershell
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8180/realms/my-workflow-dev/protocol/openid-connect/token `
  -ContentType application/x-www-form-urlencoded `
  -Body @{
    grant_type = "password"
    client_id = "workflow-frontend"
    username = "manager-local"
    password = "manager123"
  }

$token = $tokenResponse.access_token
```

## Reset the local realm

Realm import runs only when the realm does not already exist. Stop the matching Compose stack, remove its `keycloak_data` volume only if you intentionally want to reset Keycloak users, then start the same stack again. The development overlay imports `keycloak/realm-export-dev.json`; the base stack imports `keycloak/realm-export.json`.

```powershell
docker compose -f compose.yaml -f compose.dev.yaml down
docker volume ls
docker volume rm workflow-dev_keycloak_data
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
```

Check the exact volume name with `docker volume ls` before removal. Removing this volume deletes the local Keycloak users; the MySQL user profiles remain.
