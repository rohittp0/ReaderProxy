# ReaderProxy 📖

<img src="docs/screen-recording.gif" height="500">

A VPN-backed local proxy for Android that intercepts HTTP(S) traffic and injects **reader mode** into web pages — on demand.

Forget the popups, autoplay videos, cookie banners, newsletter modals.  
**ReaderProxy** lets you browse normally and tap a floating button to switch to a clean, readable view — powered by Readability.js under the hood.

---

## ✨ Features

- 🔒 Intercepts both **HTTP and HTTPS** using a local MITM proxy (no root required)
- 📖 Uses **Readability4J** to extract clean article content
- 💡 Injects a floating “Reader Mode” button instead of auto-replacing
- 🌙 Reader view comes with **dark mode CSS**, full offline rendering
- 📦 Built entirely in Kotlin — lightweight, modern, hackable


## 🔧 How it works

1. ReaderProxy runs as a **VpnService** and starts a local MITM proxy
2. Chrome or any browser routes requests through it (via system proxy config)
3. We decrypt HTTPS with a user-installed CA certificate (manual one-time setup)
4. For each response:
    - If it’s HTML, we inject a 📖 button into the page
    - Clicking it swaps the DOM with a clean reader-mode version
    - All other responses (images, scripts, etc) are untouched
5. Uses [Readability4J](https://github.com/chimbori/Readability) + [Jsoup](https://jsoup.org/) + custom CSS

## 📱 Requirements

- One-time install of the custom CA certificate
- Browser that respects VPN-based system proxies (Chrome, Firefox, etc.)

---

## Development Setup

### Setup Certificate Authority

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