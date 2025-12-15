

# 🔐 Keycloak + Spring Boot HTTPS Backend Setup

This document explains how to run **Keycloak using Docker**, enable **HTTPS in Spring Boot backend using a self-signed certificate**, and connect it with a frontend application.

> ⚠️ **Note**
> Keycloak runs on **HTTP**, while the **backend runs on HTTPS**.

---

## 🧩 Architecture

```
Frontend (Angular)  →  http://localhost:4200
Backend (Spring)    →  https://localhost:8081
Keycloak            →  http://localhost:8080
```

---

## 1️⃣ Run Keycloak using Docker

Run the following command to start **Keycloak 24.0.1** in dev mode:

```bash
docker run -d \
  --name keycloak_24.0.1 \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.1 \
  start-dev
```

### Access Keycloak

```
http://localhost:8080
```

Login using:

```
Username: admin
Password: admin
```

---

## 2️⃣ Create Self-Signed Certificate for Backend

Generate a **PKCS12 certificate** for Spring Boot HTTPS:

```bash
keytool -genkeypair \
  -alias backend \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore backend.p12 \
  -validity 365 \
  -storepass bnt@123 \
  -dname "CN=localhost, OU=IT, O=BNTSoft, L=Pune, ST=Maharashtra, C=IN"
```

### Output

```
backend.p12
```

Move the file to:

```
src/main/resources/backend.p12
```

---

## 3️⃣ Configure HTTPS in Spring Boot

Add the following configuration to `application.properties`:

```properties
# =======================
# SERVER SSL CONFIG
# =======================
server.port=8081

server.ssl.enabled=true
server.ssl.key-store=classpath:backend.p12
server.ssl.key-store-password=bnt@123
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=backend
```

Backend will start on:

```
https://localhost:8081
```

> Browser will show a warning because the certificate is self-signed — this is expected for DEV.

---

## 4️⃣ Frontend Configuration (Angular)

### In `app.component.ts`
Ensure API calls point to backend HTTPS URL:

```ts
 async callBackend() {
    this.response = await this.makeRequest('https://localhost:8081/auth/check');
  }

  async callAdmin() {
    this.response = await this.makeRequest('https://localhost:8081/auth/admin');
  }
```

---

## 5️⃣ Keycloak Client Configuration

In **Keycloak Admin Console**:

```
Realm: myrealm
Client ID: angular-app1
```

### Client Settings

| Setting             | Value                                             |
| ------------------- | ------------------------------------------------- |
| Root URL            | [http://localhost:4200](http://localhost:4200)    |
| Valid Redirect URIs | [http://localhost:4200/](http://localhost:4200/)* |
| Web Origins         | [http://localhost:4200](http://localhost:4200)    |
---

