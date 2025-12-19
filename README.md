

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

# Way 2 – With Trusted Certificate
---
# 🔐 TLS Certificate & Keystore Generation (PKCS12)

This document describes how to generate TLS certificates using a **private Certificate Authority (CA)** and package them into **PKCS12 (`.p12`) keystores** for use in a **Spring Boot application**.

The setup covers:

* Admin Portal
* Keycloak
* Extractor Service

---

## 📁 Final Directory Structure

```text
certificates/
│   └── keystore.p12
│   └── truststore.p12
├── keycloak/
│   ├── keystore.p12
│   └── truststore.p12
└── extractor/
    └── svc.cluster.local.cert.p12
```

---

## 🧠 High-Level Workflow

1. Create a **private CA**
2. Generate **private key** for each service
3. Generate **CSR (Certificate Signing Request)**
4. Sign CSR using the private CA
5. Export key + cert + CA into **PKCS12 keystore**
6. Create **truststore** where required

---

## 1️⃣ Create Required Directories if not

```bash
mkdir -p certificates certificates/keycloak certificates/extractor
```

### What this does

Creates folders to store keystore and truststore files for each service.

---

## 2️⃣ Create Private Certificate Authority (CA)

### 2.1 Generate CA Private Key

```bash
openssl genrsa -out myCA.key 4096
```

**Explanation**

* Generates a **4096-bit RSA private key**
* This key is used to **sign all service certificates**
* Must be kept **secret and secure**

📄 Output:

* `myCA.key` → CA private key

---

### 2.2 Create Self-Signed CA Certificate

```bash
openssl req -x509 -new -nodes \
  -key myCA.key \
  -sha256 \
  -days 3650 \
  -out myCA.crt
```

**Explanation**

* Creates a **self-signed CA certificate**
* Valid for **10 years**
* Uses SHA-256 hashing

📄 Output:

* `myCA.crt` → Public CA certificate (shared with services)

---

## 3️⃣ Admin Portal Certificate

### 3.1 Generate Admin Portal Private Key

```bash
openssl genrsa -out admin-portal.key 2048
```

**Explanation**

* Generates a **2048-bit RSA private key**
* Used only by Admin Portal

📄 Output:

* `admin-portal.key`

---

### 3.2 Generate Admin Portal CSR

```bash
openssl req -new \
  -key admin-portal.key \
  -out admin-portal.csr
```

**Explanation**

* Creates a Certificate Signing Request
* Contains public key + identity details

📄 Output:

* `admin-portal.csr`

---

### 3.3 Sign Admin Portal Certificate Using CA

```bash
openssl x509 -req \
  -in admin-portal.csr \
  -CA myCA.crt \
  -CAkey myCA.key \
  -CAcreateserial \
  -out admin-portal.crt \
  -days 365 \
  -sha256
```

**Explanation**

* Signs the CSR using the private CA
* `-CAcreateserial` creates `myCA.srl` if not present

📄 Output:

* `admin-portal.crt`
* `myCA.srl` (CA serial tracking file)

---

### 3.4 Create Admin Portal PKCS12 Keystore

```bash
openssl pkcs12 -export \
  -inkey admin-portal.key \
  -in admin-portal.crt \
  -certfile myCA.crt \
  -out certificates/keystore.p12 \
  -name admin-portal \
  -password pass:adminportal1234
```
📄 Output:

* `certificates/keystore.p12`
--- 
### 3.5 Create Admin Portal Truststore

```bash
keytool -importcert \
  -alias admin-portal-ca \
  -file myCA.crt \
  -keystore certificates/truststore.p12 \
  -storetype PKCS12 \
  -storepass adminportal1234 \
  -noprompt
```
**Explanation**

* Combines:

  * Private key
  * Signed certificate
  * CA certificate
* Produces a **Spring Boot compatible keystore**

📄 Output:

* `certificates/truststore.p12`

---

## 4️⃣ Keycloak Certificate

### 4.1 Generate Keycloak Private Key

```bash
openssl genrsa -out keycloak.key 2048
```

📄 Output:

* `keycloak.key`

---

### 4.2 Generate Keycloak CSR

```bash
openssl req -new \
  -key keycloak.key \
  -out keycloak.csr
```

📄 Output:

* `keycloak.csr`

---

### 4.3 Sign Keycloak Certificate Using CA

```bash
openssl x509 -req \
  -in keycloak.csr \
  -CA myCA.crt \
  -CAkey myCA.key \
  -CAcreateserial \
  -out keycloak.crt \
  -days 365 \
  -sha256
```

📄 Output:

* `keycloak.crt`

---

### 4.4 Create Keycloak PKCS12 Keystore

```bash
openssl pkcs12 -export \
  -inkey keycloak.key \
  -in keycloak.crt \
  -certfile myCA.crt \
  -out certificates/keycloak/keystore.p12 \
  -name keycloak \
  -password pass:keycloak1234
```

📄 Output:

* `certificates/keycloak/keystore.p12`

---

### 4.5 Create Keycloak Truststore

```bash
keytool -importcert \
  -alias my-private-ca \
  -file myCA.crt \
  -keystore certificates/keycloak/truststore.p12 \
  -storetype PKCS12 \
  -storepass keycloak1234 \
  -noprompt
```

**Explanation**

* Imports CA certificate into truststore
* Allows Keycloak to trust certificates issued by this CA

📄 Output:

* `certificates/keycloak/truststore.p12`

---

## 5️⃣ Extractor Service Certificate

### 5.1 Generate Extractor Private Key

```bash
openssl genrsa -out extractor.key 2048
```

📄 Output:

* `extractor.key`

---

### 5.2 Generate Extractor CSR

```bash
openssl req -new \
  -key extractor.key \
  -out extractor.csr
```

📄 Output:

* `extractor.csr`

---

### 5.3 Sign Extractor Certificate Using CA

```bash
openssl x509 -req \
  -in extractor.csr \
  -CA myCA.crt \
  -CAkey myCA.key \
  -CAcreateserial \
  -out extractor.crt \
  -days 365 \
  -sha256
```

📄 Output:

* `extractor.crt`

---

### 5.4 Create Extractor PKCS12 Keystore

```bash
openssl pkcs12 -export \
  -inkey extractor.key \
  -in extractor.crt \
  -certfile myCA.crt \
  -out certificates/extractor/svc.cluster.local.cert.p12 \
  -name extractor \
  -password pass:extractor1234
```

📄 Output:

* `svc.cluster.local.cert.p12`

---

## 🔐 Password Summary

| Component    | Password        |
| ------------ | --------------- |
| Admin Portal | adminportal1234 |
| Keycloak     | keycloak1234    |
| Extractor    | extractor1234   |

---
# TLS Setup for Frontend (Angular) and Keycloak

---

## TLS for Frontend (Angular)

The frontend requires a **private key** and a **TLS certificate** so that the Angular development server can run on **HTTPS**.

---

### 1. Generate Frontend Private Key

```bash
openssl genrsa -out frontend.key 2048
```

**Why this command is used**

* Generates a private key for the frontend server
* Required to encrypt and decrypt TLS traffic

**What it does**

* `genrsa` → generates an RSA private key
* `2048` → key size (secure and widely supported)
* **Output**: `frontend.key`

---

### 2. Create Certificate Signing Request (CSR)

```bash
openssl req -new \
  -key frontend.key \
  -out frontend.csr
```

**Why this command is used**

* A CSR is required to request a certificate from a Certificate Authority (CA)
* Contains the public key and identity information (CN, organization, etc.)

**What it does**

* `-new` → creates a new CSR
* `-key frontend.key` → uses the frontend private key
* **Output**: `frontend.csr`

---

### 3. Sign CSR Using Internal CA

```bash
openssl x509 -req \
  -in frontend.csr \
  -CA ../auth-service/myCA.crt \
  -CAkey ../auth-service/myCA.key \
  -CAcreateserial \
  -out frontend.crt \
  -days 365 \
  -sha256
```

**Why this command is used**

* Converts the CSR into a trusted TLS certificate
* The certificate is trusted because it is signed by the internal CA (`myCA`)

**What it does**

* `-req` → input is a CSR
* `-CA myCA.crt` → CA certificate
* `-CAkey myCA.key` → CA private key used for signing
* `-CAcreateserial` → creates a serial number file for the CA
* `-days 365` → certificate validity period
* `-sha256` → secure hashing algorithm
* **Output**: `frontend.crt`

---

### 4. Enable HTTPS in Angular

**`angular.json`**

```json
"serve": {
  "options": {
    "ssl": true,
    "sslKey": "frontend.key",
    "sslCert": "frontend.crt"
  }
}
```

**Why this is required**

* Angular dev server runs on HTTP by default
* HTTPS is required for OAuth2 / OIDC authentication with Keycloak

**What it does**

* `ssl: true` → enables HTTPS
* `sslKey` → frontend private key
* `sslCert` → TLS certificate

---

## TLS for Keycloak

Keycloak must run on **HTTPS** to ensure secure authentication, cookies, and OAuth/OIDC flows.

---

### 1. Generate Keycloak Private Key

```bash
openssl genrsa -out keycloak.key 2048
```

**Why**

* Creates the private key for the Keycloak HTTPS server

**What it does**

* Generates a 2048-bit RSA private key

---

### 2. Create CSR for Keycloak

```bash
openssl req -new \
  -key keycloak.key \
  -out keycloak.csr
```

**Why**

* A CSR is required to request a CA-signed certificate for Keycloak

---

### 3. Create Extension File (SAN Support)

**`keycloak.ext`**

```ini
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
```

**Why this file is important**

* Modern browsers require **Subject Alternative Name (SAN)**
* Common Name (CN) alone is no longer sufficient

**What each field means**

* `CA:FALSE` → certificate is not a Certificate Authority
* `keyUsage` → allowed cryptographic operations
* `extendedKeyUsage = serverAuth` → valid for HTTPS servers
* `subjectAltName` → hostnames and IPs Keycloak runs on

---

### 4. Sign Keycloak Certificate with CA

```bash
openssl x509 -req \
  -in keycloak.csr \
  -CA myCA.crt \
  -CAkey myCA.key \
  -CAcreateserial \
  -out keycloak.crt \
  -days 825 \
  -sha256 \
  -extfile keycloak.ext
```

**Why**

* Creates a CA-trusted TLS certificate for Keycloak

**What it does**

* Signs the CSR using the internal CA
* Applies SAN configuration from `keycloak.ext`

---

### 5. Convert Certificate to PKCS12 Keystore

```bash
openssl pkcs12 -export \
  -inkey keycloak.key \
  -in keycloak.crt \
  -certfile myCA.crt \
  -out keycloak.p12 \
  -name keycloak \
  -password pass:keycloak1234
```

**Why this is required**

* Keycloak does not accept PEM files directly
* Requires a keystore in **PKCS12** format

**What it does**

* Combines private key, certificate, and CA certificate
* Produces `keycloak.p12`

---

## Running Keycloak with HTTPS (Docker)

```bash
docker run -d ^
  --name keycloak_24.0.1 ^
  -p 8080:8443 ^
  -e KEYCLOAK_ADMIN=admin ^
  -e KEYCLOAK_ADMIN_PASSWORD=admin ^
  -e KC_HOSTNAME=localhost ^
  -e KC_HOSTNAME_PORT=8080 ^
  -e KC_SPI_LOGIN_PROTOCOL_OPENID_CONNECT_CHECK_LOGIN_IFRAME=false ^
  -e KC_HTTPS_KEY_STORE_FILE=/opt/keycloak/conf/keycloak.p12 ^
  -e KC_HTTPS_KEY_STORE_PASSWORD=keycloak1234 ^
  -e KC_HTTPS_KEY_STORE_TYPE=PKCS12 ^
  -v "D:/CENNOX/keycloak-with-tls-old/auth-service/keycloak.p12:/opt/keycloak/conf/keycloak.p12" ^
  quay.io/keycloak/keycloak:24.0.1 ^
  start
```

### Why these options are used

| Option                        | Purpose                           |
| ----------------------------- | --------------------------------- |
| `-p 8080:8443`                | Maps host HTTPS port to container |
| `KC_HTTPS_KEY_STORE_FILE`     | Path to TLS keystore              |
| `KC_HTTPS_KEY_STORE_PASSWORD` | Keystore password                 |
| `KC_HTTPS_KEY_STORE_TYPE`     | Keystore format (PKCS12)          |
| `-v`                          | Mounts keystore into container    |

---

## Backend (Spring Boot) – Trusting the CA

The backend service must trust Keycloak's TLS certificate. Since Keycloak uses a certificate signed by an internal CA (`myCA`), the backend JVM must be configured with a custom truststore.

---

### Truststore Configuration

**`build.gradle`**

```gradle
bootRun {
    jvmArgs = [
        "-Djavax.net.ssl.trustStore=src/main/resources/truststore.p12",
        "-Djavax.net.ssl.trustStorePassword=trustpass",
        "-Djavax.net.ssl.trustStoreType=PKCS12"
    ]
}
````

---

### Why this is required

* The backend communicates with Keycloak over **HTTPS**
* Keycloak uses a certificate signed by **myCA**, not a public CA
* The JVM does **not trust internal CAs by default**
* Without this configuration, the backend will fail with:

  ```
  SSLHandshakeException
  ```

---

### What it does

* Loads a **custom truststore** at application startup
* The truststore contains `myCA.crt`
* Allows the backend to trust all certificates signed by the internal CA

---

### Result

* Secure HTTPS communication between Backend and Keycloak
* No SSL handshake or certificate validation errors
---


