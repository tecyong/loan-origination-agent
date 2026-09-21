# Acme Cloud Developer Portal - API Integration FAQ

**Service Version:** v3.2  
**Protocol:** RESTful HTTPS with JSON Payload  

---

## 1. Authentication & API Keys
- **Header:** All API requests must include an authorization bearer token:  
  `Authorization: Bearer <YOUR_ACME_API_KEY>`
- **API Key Generation:** Generate personal or service account keys in the Acme Cloud Console under `Project Settings > Credentials`.
- **Key Expiry:** By default, production service account keys rotate every 90 days.

## 2. Rate Limits & Quotas
- **Sandbox Environment:** 60 requests per minute (RPM) per IP address.
- **Production Standard:** 1,200 requests per minute (20 RPS) with burst capacity up to 50 RPS.
- **Rate Limit Headers:**
  - `X-RateLimit-Limit`: Maximum allowable requests in current window.
  - `X-RateLimit-Remaining`: Remaining request allowance.
  - `X-RateLimit-Reset`: Unix timestamp when quota window resets.
- When exceeding rate limits, the server returns HTTP `429 Too Many Requests` with a `Retry-After` header.

## 3. Webhooks & Event Notifications
- Acme supports real-time event webhooks for order creation (`order.created`), status changes (`order.shipped`), and customer profile updates.
- All webhook payloads are signed using HMAC-SHA256 with your webhook secret, passed in `X-Acme-Signature`.
- Endpoints must respond with HTTP `200 OK` within 5 seconds, or Acme will retry with exponential backoff up to 5 attempts.

## 4. Error Codes & Troubleshooting
- `400 Bad Request`: Payload validation failed or required JSON field missing.
- `401 Unauthorized`: API key is missing, invalid, or revoked.
- `403 Forbidden`: API key lacks required RBAC scope for the requested resource.
- `404 Not Found`: Resource (customer ID, order ID, product SKU) does not exist.
- `500 Internal Error`: Transient server error; clients should retry with jitter.
