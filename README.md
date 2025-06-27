## Setup Certificate Authority

```bash
# 1) Generate a new RSA private key (PEM, PKCS#8 format)
openssl genpkey \
  -algorithm RSA \
  -out ca_key.pem \
  -pkeyopt rsa_keygen_bits:2048

# 2) Create a self-signed certificate from that key
openssl req -x509 \
  -new \
  -key ca_key.pem \
  -sha256 \
  -days 3650 \
  -out ca_cert.pem \
  -subj "/CN=ReaderProxy CA"
```

Place the generated `ca_key.pem` and `ca_cert.pem` files in the `app/assets` directory.