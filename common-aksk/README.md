# common-aksk

`common-aksk` provides AccessKey/SecretKey credential management and signed-request verification for third-party API access.

Use it when a caller cannot use the normal user token flow but still needs a stable, revocable identity with scoped permissions. The module owns credential lifecycle, signing primitives, `@Aksk`, scope checks, Spring MVC AK/SK interception, request body caching, and writes successful AK/SK identities into `SecurityContextHolder`.

`common-aksk` uses the `common-core` MVC interceptor hook and expects requests to already run inside a request-scoped `SecurityContextHolder` filter. `common-security` supplies that filter; if a service depends on `common-aksk` directly, register an equivalent scope wrapper before MVC dispatch.

## Dependencies

For AK/SK-only services that already provide a `SecurityContextHolder` request scope, depend on `common-aksk` directly:

```xml
<dependency>
    <groupId>io.github.ilovejavac</groupId>
    <artifactId>common-aksk</artifactId>
</dependency>
```

For services that also need token/admin/permission security, depend on `common-security`. It depends on `common-aksk`, registers only its internal/token auth interceptors through the same `common-core` hook, and provides the request-scoped `SecurityContextHolder` filter that AK/SK writes into after verification.

```xml
<dependency>
    <groupId>io.github.ilovejavac</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

## Create Credentials

Management APIs are exposed under `/admin/aksk`. When `common-security` is enabled, `/admin/**` requires a logged-in user with role `admin`; `@Anonymous` and whitelist configuration do not bypass the admin rule.

Create a credential:

```http
POST /admin/aksk/create_aksk
Content-Type: application/json

{
  "subjectName": "Datares Partner",
  "subjectCode": "datares-partner",
  "contactName": "Ops",
  "contactPhone": "10086",
  "description": "Partner API credential",
  "scopes": ["datares:ads:query"],
  "properties": {
    "tenant": "datares"
  },
  "expireAt": "2026-12-31T23:59:59"
}
```

The response contains the only plain SecretKey copy:

```json
{
  "data": {
    "id": "100000000000001",
    "accessKey": "ak_xxx",
    "secretKey": "sk_xxx"
  }
}
```

Store the SecretKey immediately. List/detail APIs never return the plain SecretKey. Resetting a credential through `POST /admin/aksk/reset_secret_aksk/{id}` returns a new plain SecretKey once.

Other management APIs:

- `POST /admin/aksk/update_aksk`
- `POST /admin/aksk/delete_aksk/{id}`
- `POST /admin/aksk/enable_aksk/{id}`
- `POST /admin/aksk/disable_aksk/{id}`
- `POST /admin/aksk/query_list_aksk`
- `GET /admin/aksk/detail_aksk/{id}`

## Protect An Endpoint

Add `@Aksk` to a method or controller class. Method annotations override class annotations.

```java
@Aksk(scopes = {"datares:ads:query"}, headerPrefix = "X-Datares")
@PostMapping("/datalake/api/ads/datares/{code}/query")
public ServerResponse<?> query(
        @PathVariable String code,
        @RequestBody Map<String, Object> query
) {
    return ServerResponse.success(...);
}
```

With `headerPrefix = "X-Datares"`, the caller must send:

```http
X-Datares-Access-Key: ak_xxx
X-Datares-Timestamp: 1788191999000
X-Datares-Signature: 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
```

Default headers use `app.aksk.header-prefix`, which defaults to `X-Aksk`:

- `X-Aksk-Access-Key`
- `X-Aksk-Timestamp`
- `X-Aksk-Signature`

You can also configure exact header names globally:

```yaml
app:
  aksk:
    header-prefix: X-Aksk
    access-key-header: X-App-AK
    timestamp-header: X-App-Timestamp
    signature-header: X-App-Signature
    timestamp-tolerance: 5m
    max-cached-body-bytes: 1048576
```

Annotation-level exact header names take priority over annotation prefix, which takes priority over configured exact names, which take priority over configured prefix.

## Signing Format

The timestamp is Unix epoch milliseconds. The server accepts timestamps within `app.aksk.timestamp-tolerance`, default `5m`.

Build the signing text with four lines:

```text
HTTP_METHOD
REQUEST_URI
TIMESTAMP
SHA256_HEX_OF_RAW_BODY_BYTES
```

Rules:

- `HTTP_METHOD` is the request method, for example `POST`.
- `REQUEST_URI` is the path from `HttpServletRequest#getRequestURI()`, without query string.
- `TIMESTAMP` must be the same value sent in the timestamp header.
- `SHA256_HEX_OF_RAW_BODY_BYTES` is SHA-256 hex of the raw request body bytes. Empty body uses SHA-256 of an empty byte array.
- Signature is lowercase hex `HmacSHA256(secretKey, signingText)`.

Example signing text:

```text
POST
/datalake/api/ads/datares/ad-001/query
1788191999000
0a3bb3fd4f6d0d4f41f40f5743bff2a821d0b26b1d092b9efad7d534c0f78d42
```

## Java Signing Example

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AkskClientSigner {

    private AkskClientSigner() {
    }

    public static String sign(String secretKey, String method, String path, long timestamp, byte[] body)
            throws Exception {

        byte[] sourceBody = body == null ? new byte[0] : body;
        String bodySha256 = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(sourceBody)
        );
        String signingText = String.join("\n", method, path, String.valueOf(timestamp), bodySha256);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(signingText.getBytes(StandardCharsets.UTF_8)));
    }
}
```

## Scope Behavior

`@Aksk(scopes = {...})` uses any-match authorization. A credential with any one required scope is accepted.

```java
@Aksk(scopes = {"order:read", "order:export"})
```

The example above accepts a credential that has `order:read` or `order:export`. Empty annotation scopes mean identity verification only.

After successful verification, `SecurityContextHolder` contains a validated non-admin `UserDetails`:

- The user role is `AKSK`.
- Credential scopes are mirrored as permissions.
- `validated` is `true`.
- AK/SK fields such as access key, credential id, subject code, and properties are available from `UserDetails.extra`.

## Security Notes

- V1 does not implement nonce replay protection. It only validates the timestamp window, signature, credential status, expiration time, and scopes.
- HTTPS is still required. AK/SK signing does not protect headers or bodies from network disclosure.
- Keep the timestamp tolerance as small as your client clock drift allows.
- Rotate a leaked SecretKey with `POST /admin/aksk/reset_secret_aksk/{id}` and disable unused credentials.
- POST, PUT, and PATCH request bodies are cached for signature verification up to `app.aksk.max-cached-body-bytes`. Oversized signed requests are rejected before business code runs.
