# Attestra Android onboarding UI

This app currently opens an **onboarding screen gallery**. It implements the visual states from the pinned `design.attestra` submodule in Compose; it does not create accounts, confirm emails, register passkeys, or verify IDs. Tap a gallery item to inspect a screen, then use Back to return to the gallery.

## Screen groups

| Group | Included views |
| --- | --- |
| Email | Start, wait for email, manual six-digit code, wrong code, attempt limit, unusable link, confirmed email with missing session |
| Shared loading | Email confirmation/resend, passkey manager/registration, ID extraction/submission, provider decision pending; `LoadingTask` supplies the title and text |
| Passkey | Start, interrupted/cancelled, unsupported device |
| Optional ID check | Entry, editable document data, unreadable images, submission failure, approved outcome, provisional unsuccessful outcome |

The capture plugin supplies its own camera screens. The unsuccessful identity outcome uses the shared error layout because the design submodule has no mockup for the final provider rejection yet. The six-slot code presentation uses one underlying field for paste and autofill.

## Integration points

Screen composables in `app/src/main/java/com/apexfission/android/attestra/auth/ui/onboarding/` take callbacks and external result state. `OnboardingBusinessActions` holds intentionally empty functions for signup, resend, A+B or B+C confirmation, session recovery, passkeys, and ID plugin work. Replace these hooks with a host that owns navigation and backend state. Do not navigate to success on a button tap: only a confirmed response may display a completed step. The gallery is for visual review and its example email/status values are illustrative.

The design submodule is pinned by this repository. Initialize it with `git submodule update --init` (use an HTTPS URL override when SSH access is unavailable). See `design.attestra/auth/onboarding/architecture.md` and `design.attestra/auth/onboarding/ui-reference/README.md` for the flow and screen inventory.
