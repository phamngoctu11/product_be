# Order timestamp cleanup

Trạng thái: `VERIFIED` theo review và chấp nhận của chủ dự án ngày 28/10/2026.

Date: 2026-10-24

This implementation removes redundant order-level timestamps while retaining the dates that still drive a user-facing or operational rule.

This is the current entity/API contract and supersedes older phase notes that list the removed Order timestamp fields.

## Removed from `Order`

- `endOrderTime`: list sorting now uses `deliveredAt`; cancellation remains represented by `cancelledAt`.
- `orderLookupTokenCreatedAt`: token expiry is calculated directly when the scoped token is issued.
- `orderLookupTokenRevokedAt`: revocation invalidates the token by clearing its hash, scope, and expiry.
- `guestWorkflowStartedAt`: workflow state and process-instance ID already represent whether startup succeeded; failures retain `guestWorkflowError`.
- `managerApprovedAt` / `managerRejectedAt`: review decisions remain on the order, and durable transition/audit history records when the decision happened. `confirmationDueAt` is still calculated from the approval instant in the same transaction.
- `userSubmittedAt`: the submitted `OrderAgreement.submittedAt` is now the canonical record. Timeout selection, the final timeout check, and the event handler consult agreement submissions instead of a duplicate timestamp on `Order`.
- `orderAcceptedAt`: it was only written and had no consumer.
- `readyToShipAt`: no UI or business rule consumed this separate column; the transition audit records its occurrence, and the ready-to-ship event correlates the workflow without carrying a timestamp.

Liquibase `16-order-timestamp-cleanup.xml` drops these nine columns. The migration uses one guarded changeset per column so schemas where a legacy column is already absent can still advance.

## Retained on `Order`

- `startOrderTime`: order creation date shown to users/staff and used to order open work queues.
- `orderLookupTokenExpiresAt`: security boundary for public guest-order links.
- `confirmationDueAt`: deadline for automatic cancellation of unsubmitted custom agreements.
- `productionStartedAt`: anchor for production deadlines and checkpoint eligibility.
- `shippedAt`: anchor for the expected delivery date shown to users.
- `deliveredAt`: delivery history and time-bounded delivered-order reporting.
- `cancelledAt`: cancellation result/history and support traceability.

`OrderAgreement.submittedAt` and workflow/status audit timestamps remain because they represent actual agreement submissions and state transitions, rather than duplicate aggregate fields.

## Verification

- Backend main-source compilation: passed.
- Frontend production-development build: passed.
- Full test compilation remains blocked by pre-existing test fixtures that still pass `double` values to the now-`BigDecimal` money model. Timestamp-related stale test references found during this refactor were updated.
