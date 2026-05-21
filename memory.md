Current checkpoint: `common-storage` now auto-selects the only fully configured backend, fails fast on ambiguous multi-backend config without `app.storage.type`, and persists the resolved backend type into storage metadata.

Evidence:
- Added `StorageBackendSelectionTest` covering local-only auto-selection, minio-only auto-selection, ambiguous multi-backend failure, and explicit-type-missing-config failure.
- Verification command: `mvn -pl common-storage -Dtest=StorageBackendSelectionTest test`
- Verification result: 4 tests run, 0 failures, 0 errors, build success on 2026-05-21 Asia/Shanghai.

Open risk:
- `rustfs` is recognized by config resolution but still has no runtime storage adapter implementation in `common-storage`.

Exact next task:
- None for this scope unless the user wants `rustfs` implemented as a real storage backend.
