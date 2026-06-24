# Keycloak local setup

## Start infrastructure

From the project directory:

```powershell
docker compose up -d
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

The imported realm is `my-workflow`. The application client is `workflow-frontend`.

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

These seeded Keycloak users are useful for testing role-protected endpoints. They do not automatically have matching business profiles in the MySQL `users` table.

## Start the backend

Keycloak must be running before Spring Boot starts because the resource server reads the realm metadata from the issuer URL.

```powershell
mvnw.cmd spring-boot:run
```

If the Maven wrapper is not present:

```powershell
mvn spring-boot:run
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
  -Uri http://localhost:8180/realms/my-workflow/protocol/openid-connect/token `
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

Realm import runs only when the realm does not already exist. To recreate it from `keycloak/realm-export.json`:

```powershell
docker compose down
docker volume rm my-project_keycloak_data
docker compose up -d
```

The exact volume prefix can differ if Docker Compose uses another project name. Check it with `docker volume ls` before removal.
