

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
