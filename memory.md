Current phase: Task 16 common-lib fix complete; downstream datalake Maven startup verification is blocked before Spring starts.

Confirmed requirement: fix `AkskVerifier` Spring bean construction so downstream apps using the installed `common-aksk-1.5.1-RC0.jar` can start without adding business-side configuration.

Decision: do not add a no-arg constructor because it would allow an invalid verifier with missing dependencies; select the existing public dependency constructor for Spring injection instead.

Evidence so far: datalake failed with `No default constructor found`; `javap` showed the installed `AkskVerifier` has a public 3-arg constructor and package-private 4-arg test constructor but no constructor injection annotation. The new RED test `AkskVerifierSpringContextTest` reproduced the same failure: Spring attempted `AkskVerifier.<init>()`.

Open risks: after this constructor fix, datalake startup may expose a separate downstream configuration or dependency issue; local Maven install is required before datalake can consume the fix.

Checkpoint: `AkskVerifierSpringContextTest` now passes after adding `@Autowired` to the existing public dependency constructor. Full `common-aksk` tests passed with 45 tests, `common-security` targeted regression passed with 17 tests, `git diff --check` passed, and updated `common-aksk`/`common-data-jpa` artifacts were installed locally.

Downstream blocker: datalake startup from Maven cannot reach Spring because `datalake-server` resolves `com.ware4u:datalake-bom:1.0` as `datalake-bom-1.0.jar`; the local reactor module is a POM, and the internal HTTP repository resolution hung.

Next task: rerun datalake from the IDE or fix the datalake dependency packaging/resolution issue if command-line startup verification is required.
