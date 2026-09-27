# AVMS — Open Questions (genuinely undeterminable from code alone)

1. **Cutover window:** parallel-run vs big-bang per venture? Who signs off row-count + money/stock reconciliation? (Owner: business.)
2. **Password hashes:** dual-verify Django PBKDF2 then re-hash BCrypt, or force all-users reset at cutover? (Owner: security/business.)
3. **Forgot-password delivery:** Django sends no email (DEBUG-only `reset_url`). Does prod need real email/SMS now, or keep manual-reset flow? (Owner: business.)
4. **Refresh-token store:** stateful `REFRESH_TOKEN` table (revocable, recommended) vs stateless JWT-only? (Default: stateful; confirm ops cost OK.)
5. **File attachments:** none in code today — will bills/expenses need document upload in the new stack? (If yes, S3 design required.)
6. **Reporting load:** any SLA for 30d/1y report windows or multi-venture consolidation not in current code? (Affects Oracle indexing/materialization.)
7. **Microservice trigger:** any known near-term scale/compliance reason to split (e.g. payments PCI scope)? None found in code — confirm.
8. **Timezone:** Django runs `Asia/Kolkata`-local semantics. Confirm target = store UTC, display IST (recommended) before ETL.
9. **Legacy `VentureCodeCounter`:** retire after ETL (recommended) or keep dual-write? (Default: retire; keep table read-only one release.)
10. **Frontend hosting:** S3+CloudFront static vs ECS-served Nginx? (Default: S3+CloudFront; confirm AWS account/region.)

Everything else was determined by inspecting code — no other placeholders assumed.
