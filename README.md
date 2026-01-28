# AuthVs Server - OIDC 1.0 Authorization Server

Based on Spring Boot 3.5.10 and Spring Authorization Server.

## 1. Environment Setup

### Database
Ensure MySQL is running. Create database `authvs`.
Execute the SQL script to create tables (or let MyBatis/JPA handle it if configured, but here we assume schema exists as per requirements).

```sql
CREATE DATABASE IF NOT EXISTS authvs DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE authvs;

-- Execute the CREATE TABLE statements provided in the requirements
-- (See below or user prompt)
```

### Configuration
Edit `src/main/resources/application.yml` to match your database credentials.

## 2. Initialize Data

Run the test class `com.authvs.server.DataInitializerTest` to insert initial data.
- **User**: `admin` / `password`
- **Client**: `oidc-client` / `secret`

## 3. Run Application

Run `com.authvs.server.AuthVsServerApplication`.
Server will start on port `9000`.

## 4. Test Steps

### Step 1: Get Authorization Code
Open browser and visit:
```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=web-portal&scope=openid profile email&redirect_uri=http://localhost:3000/callback
```
1. Redirects to login page.
2. Login with `admin` / `password`.
3. Approve consent (if asked).
4. Browser redirects to `https://oauth.pstmn.io/v1/callback?code=<code>`.
5. Copy the `<code>`.

### Step 2: Exchange Code for Token
Replace `CODE` with the code from Step 1.

```bash
curl -X POST http://localhost:9000/oauth2/token \
-H "Authorization: Basic b2lkYy1jbGllbnQ6c2VjcmV0" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=authorization_code" \
-d "code=CODE" \
-d "redirect_uri=http://localhost:3000/callback"
```
*Note: `b2lkYy1jbGllbnQ6c2VjcmV0` is Base64 of `oidc-client:secret`.*

### Step 3: Refresh Token
Replace `REFRESH_TOKEN` with the refresh token from Step 2.

```bash
curl -X POST http://localhost:9000/oauth2/token \
-H "Authorization: Basic b2lkYy1jbGllbnQ6c2VjcmV0" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=refresh_token" \
-d "refresh_token=REFRESH_TOKEN"
```

### Step 4: Client Credentials Mode

```bash
curl -X POST http://localhost:9000/oauth2/token \
-H "Authorization: Basic b2lkYy1jbGllbnQ6c2VjcmV0" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "scope=openid profile"
```

### Step 5: User Info
Replace `ACCESS_TOKEN` with the access token from previous steps.

```bash
curl -X GET http://localhost:9000/userinfo \
-H "Authorization: Bearer ACCESS_TOKEN"
```

## 5. Project Structure

- `entity`: Database entities (`User`, `OauthClient`)
- `mapper`: MyBatis-Plus mappers
- `service`: `UserDetailsService` and `RegisteredClientRepository` implementations
- `config`: Security configurations (`AuthorizationServerConfig`, `DefaultSecurityConfig`)
