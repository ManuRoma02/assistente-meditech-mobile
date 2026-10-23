# MeDiTech Mobile: Motion & UI/UX Architecture Improvement Plan

## 1. Executive Summary & Design Read
**Design Read**: Native Android healthcare / clinical assistant mobile app for healthcare professionals & researchers (MeDiTech / SUPSI / EOC), with an Apple HIG + Emil Kowalski precision motion language, leaning toward Jetpack Compose + tactile physics springs + clean clinical hierarchy.

- **`DESIGN_VARIANCE: 4`**: Rigorous clinical clarity, high trust, consistent card geometry.
- **`MOTION_INTENSITY: 6`**: Physical springs, snappy response, purposeful entrances, zero unneeded animations.
- **`VISUAL_DENSITY: 6`**: Medical data density, distinct hierarchy, ergonomic touch targets (minimum 44x44dp).

---

## 2. Emil Kowalski Frequency Gate Audit

| Frequency | Target Elements | Current Flaw | Target Spec |
|:---|:---|:---|:---|
| **100+ times/day** | Chat text field, send/mic toggle, message bubbles | Bubble entrance uses `CartoonyHighBouncySpring` (`DampingRatioHighBouncy` = 0.2, 4 oscillations) + `initialScale = 0.88f`. | Scale from `0.94f` with `MeditechClinicalSpring` (`damping = 0.78f`, `stiffness = 450f`). Instant settled feel. |
| **Tens of times/day** | Tab switcher, event card taps, email row clicks | `emilPressScale` scales down to `0.90f - 0.93f` without haptic tick. Distorts card boundaries. | `pressedScale = 0.975f` for cards, `0.94f` for FAB/circular buttons + `HapticFeedbackType.TextHandleMove`. |
| **Occasional** | Bottom sheets (Email config, Quick Actions, Settings) | Standard sheet transitions lack unified spring physics. | `MeditechSheetSpring` (`damping = 0.82f`, `stiffness = 380f`). |
| **Accessibility** | All motion paths | No reduced motion bypass. | `LocalReducedMotion` fallback to instant 0ms / 100ms alpha fade. |

---

## 3. Concrete Implementation Steps

### Step 1: Refactor `AnimationUtils.kt`
- Introduce unified tokens:
  - `MeditechClinicalSpring` (`dampingRatio = 0.78f`, `stiffness = 450f`)
  - `MeditechTactilePressSpring` (`dampingRatio = 0.72f`, `stiffness = 600f`)
  - `MeditechSheetSpring` (`dampingRatio = 0.82f`, `stiffness = 380f`)
- Update `emilPressScale` with haptic feedback synchronization and calibrated default scale `0.975f`.
- Provide `LocalReducedMotion` support via system `Settings.Global.TRANSITION_ANIMATION_SCALE`.

### Step 2: Calibrate `MeditechAiChatScreen.kt`
- Update `itemsIndexed` chat bubble entrance:
  - Replace `CartoonyHighBouncySpring` with `MeditechClinicalSpring`.
  - Raise `initialScale` from `0.88f` to `0.94f` to eliminate visual pop.

### Step 3: Normalize Press Scales in `AgendaClinicaScreen.kt` & `EmailHubScreen.kt`
- Update `IconButton` in `AgendaClinicaScreen` from `pressedScale = 0.90f` to `0.94f`.
- Standardize `EmailCard` and `EventCard` press interaction to `0.98f` with tactile haptic response.

### Step 4: Polish `LiquidFluidTabDock` in `MeditechApp.kt`
- Refine sliding pill shadow and border gradients with Apple HIG Liquid Glass standards (multi-stop border, subtle depth).

---

## 4. Verification & Testing
1. Run local test suite: `ota run test` or `./gradlew testDebugUnitTest`.
2. Verify interactive motion curves in the Generative UI Lab artifact: `mobile_ui_motion_lab.html`.
