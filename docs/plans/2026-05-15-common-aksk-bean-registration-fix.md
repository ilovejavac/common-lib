# Common AK/SK Bean Registration Fix Plan

Date: 2026-05-15
Current phase: Phase 4 delivery and review for AK/SK duplicate bean startup failure.

## Confirmed requirements
- Fix the startup failure caused by duplicate `AkskProperties` beans in `common-aksk`.
- Apply the fix in the library, not only in downstream consumers.
- Preserve existing AK/SK runtime behavior and defaults.
- Add regression coverage that proves a single `AkskProperties` bean is registered.

## Root cause
- `AkskAutoConfiguration` could be loaded through two paths when a consumer broadly scanned `com.dev.lib.aksk`: Boot auto-configuration import and normal configuration component scanning.
- The old `@EnableConfigurationProperties(AkskProperties.class)` registration then created two `AkskProperties` beans with different names (`akskProperties` and `app.aksk-com.dev.lib.aksk.config.AkskProperties`).
- Other AK/SK runtime classes also mixed stereotype annotations with auto-configuration beans, which created duplicate service beans before the properties ambiguity surfaced.

## Implemented fix
1. Added a regression integration test that reproduces a consumer broad-scanning `com.dev.lib.aksk` and asserts single `AkskProperties` and `AkskRequestBodyFilter` beans.
2. Removed stereotype annotations from AK/SK runtime classes that are already provided by `AkskAutoConfiguration`.
3. Replaced `@EnableConfigurationProperties(AkskProperties.class)` with a conditional `@Bean` method annotated with `@ConfigurationProperties(prefix = "app.aksk")`, so both registration paths converge on the same bean definition name and `@ConditionalOnMissingBean` prevents duplication.

## Verification evidence
- RED reproduction: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -pl common-aksk -Dtest=AkskWebAutoConfigurationIntegrationTest test` failed before the fix because broad package scanning created duplicate `AkskService` beans, then duplicate `AkskProperties` beans.
- GREEN focused verification: same command passed after the registration cleanup and explicit properties bean wiring.
- Module verification: `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -pl common-aksk test` -> 58 tests passed, 0 failures, 0 errors.
- Diff hygiene: `git diff --check` -> exit 0.

## Risks and follow-up
- This fix addresses duplicate runtime bean registration inside `common-aksk` for both normal Boot auto-configuration and broad package scanning consumers.
- Downstream `datalake` should refresh to the rebuilt `common-aksk` artifact before re-running startup verification.
