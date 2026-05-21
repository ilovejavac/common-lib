Current checkpoint: `common-data-jpa` now uses `Long deleted` for soft-delete state, where `0` means active and soft delete writes the row `id` into `deleted`.

Evidence:
- Replaced entity, predicate, batch, update, and cascade-delete logic to use numeric `deleted` semantics.
- Updated serializer exclusion to hide `deleted` instead of `deletedAt`.
- Verification command: `mvn -pl common-data-jpa -Dtest=ScopedWriteProtectionIntegrationTest,DeleteBatchExecutionIntegrationTest,CascadeSoftDeleteIntegrationTest test`
- Verification result: 21 tests run, 0 failures, 0 errors, build success on 2026-05-21 Asia/Shanghai.
- Additional verification:
  - `mvn -pl common-data-jpa test` -> 108 tests run, 0 failures, 0 errors, build success.
  - `mvn -pl common-data-datalake -Dtest=DatalakeRepositoryWritePluginTest test` -> 4 tests run, 0 failures, 0 errors, build success.

Open risk:
- This change intentionally removes deletion timestamp semantics from JPA entities; any downstream code that depended on deletion time must be redesigned separately.

Exact next task:
- None for this scope unless the user wants a wider regression pass across other modules that build on `JpaEntity`.
