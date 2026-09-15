# IFA-HUD Control Matrix

This matrix maps security controls against executable tests to verify authorization boundaries and prevent resource leaks.

| Control ID | Threat Mitigated | Implementation | Executable Verification Test | Status |
|---|---|---|---|---|
| **CTRL-AUTH-01** | Cross-domain privilege escalation | `HudAuthorizationPolicy` resolves and enforces context permissions. | `AdaptiveHudSecurityTest` | Passed |
| **CTRL-ENUM-02** | ID enumeration / discovery | Exceptions mapped to homogeneous `404 NOT_FOUND` responses. | `AdaptiveHudControllerTest.testGetFamilyHud_AccessDenied` | Passed |
| **CTRL-LEAK-03** | Accidental data leak | Separated contracts `FamilyHudView` and `ProfessionalHudView`. | `AdaptiveHudArchitectureTest` | Passed |
| **CTRL-RACE-04** | Client-side race conditions | RxJS snoop prevention via `switchMap` and local cache clear. | Angular Compile Verification | Passed |
| **HUD-ERR-TECH-01** | Enmascaramiento de fallos técnicos como 404 | Propagación de excepciones de puertos de consulta | `AdaptiveHudControllerTest.testGetFamilyHud_InfrastructureFailure` | Passed |
