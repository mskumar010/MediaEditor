# Hybrid Android Design Guide
### Build with M3. Steal from Apple & Fluent. No BS.

---

## What This Is

A practical, copy-paste engineering guide for building Android apps in Jetpack Compose using **Material 3 as the structural skeleton**, while deliberately borrowing specific visual ideas from Apple HIG and Microsoft Fluent 2.

**Hard rule:** You are not building three design systems. You are building one (M3) with targeted overrides.

---

## Part 1 — Understand What You're Stealing and Why

### From Material 3 (Your Foundation — Keep Everything)
M3 is non-negotiable on Android because it handles:
- OS-level accessibility (touch targets, contrast, semantics)
- Adaptive layouts for different screen sizes
- Dark/light mode system integration
- Component state management (pressed, focused, disabled)
- Dynamic Color on Android 12+ devices

**Do not fight these.** Override aesthetics only. Never disable accessibility or break system integration.

### From Apple HIG (Steal: Visual Discipline)
Apple's design value is not its components — it's their **design philosophy**:

| What Apple Does | What to Steal |
|---|---|
| Radical negative space — content breathes | Use generous, consistent padding (16dp baseline, 24dp sections) |
| Strict typographic hierarchy | Only 2-3 font sizes per screen. Clear dominant > secondary > caption |
| Content-first layout | Navigation never competes with content visually |
| Clarity over decoration | No gratuitous gradients, no drop shadows on everything |
| Fixed spacing grid (4pt system) | Use multiples of 4dp: 4, 8, 12, 16, 24, 32, 48dp |

**What NOT to steal from Apple:**
- iOS navigation patterns (back swipe, tab bar at bottom in Apple style)
- SF Symbols (copyrighted, not for Android)
- iOS-specific interactions that break Android conventions

### From Fluent 2 (Steal: Depth and Surface)
Fluent's value is its **layered surface system**:

| What Fluent Does | What to Steal |
|---|---|
| Depth via elevation, not just shadows | Use crisp 1dp borders + subtle elevation together |
| Key + ambient shadow system | Two-layer shadows: sharp directional + soft diffuse |
| Acrylic/Mica frosted glass materials | Translucent surfaces on overlays, bottom sheets, nav bars |
| Tight information density | Pack data. No wasted whitespace on data-heavy screens |
| Neutral color palettes with one accent | Deep zinc/slate backgrounds. One stark accent color |

**What NOT to steal from Fluent:**
- Mouse-hover interactions (irrelevant on touch)
- Reveal lighting effects (pointer-dependent)
- Windows-specific navigation patterns

---

## Part 2 — The Theme Setup

### 2.1 Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // M3 — your foundation
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    
    // Compose Cupertino — Apple-style components on Android (optional, use selectively)
    // github.com/alexzhirkevich/compose-cupertino
    implementation("io.github.alexzhirkevich:cupertino-adaptive:0.1.0-alpha04")
    
    // Blur backport for Fluent glass effects (works pre-Android 12)
    // github.com/skydoves/Cloudy
    implementation("com.github.skydoves:cloudy:0.2.0")
}
```

> **Note on Compose Cupertino:** It is in alpha. APIs will change. Use it for specific components (pickers, action sheets) — not as a full theme replacement.

---

### 2.2 Color Tokens

M3 maps all colors through roles, not hex values. Override the roles, not individual components.

```kotlin
// Theme.kt

// === DARK SCHEME (Fluent-inspired zinc palette) ===
private val HybridDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFFFFFFF),   // Stark white — primary actions
    onPrimary        = Color(0xFF000000),   // Black text on white buttons
    primaryContainer = Color(0xFF1C1C1E),   // Subtle container surfaces
    onPrimaryContainer = Color(0xFFF5F5F5),

    secondary        = Color(0xFF8E8E93),   // iOS-like secondary gray
    onSecondary      = Color(0xFFFFFFFF),

    background       = Color(0xFF000000),   // True black (AMOLED-friendly)
    onBackground     = Color(0xFFFFFFFF),

    surface          = Color(0xFF1C1C1E),   // iOS dark surface gray
    onSurface        = Color(0xFFEBEBF5),
    surfaceVariant   = Color(0xFF2C2C2E),   // Slightly elevated surface
    onSurfaceVariant = Color(0xFFAEAEB2),

    outline          = Color(0xFF38383A),   // 1dp border lines (Fluent-style depth)
    outlineVariant   = Color(0xFF2C2C2E),   // Subtle separator
    
    error            = Color(0xFFFF453A),   // iOS red
    onError          = Color(0xFFFFFFFF),
    
    scrim            = Color(0x99000000)    // Semi-transparent modal backdrop
)

// === LIGHT SCHEME ===
private val HybridLightColorScheme = lightColorScheme(
    primary          = Color(0xFF000000),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2F2F7),   // iOS grouped background
    onPrimaryContainer = Color(0xFF1C1C1E),

    secondary        = Color(0xFF6E6E73),
    onSecondary      = Color(0xFFFFFFFF),

    background       = Color(0xFFF2F2F7),   // iOS system grouped background
    onBackground     = Color(0xFF000000),

    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF000000),
    surfaceVariant   = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6E6E73),

    outline          = Color(0xFFC6C6C8),
    outlineVariant   = Color(0xFFE5E5EA),
    
    error            = Color(0xFFFF3B30),
    onError          = Color(0xFFFFFFFF)
)
```

---

### 2.3 Shape Tokens

M3 has **five shape sizes**, not three. Override all five or defaults leak through.

```kotlin
// Shape.kt

private val HybridShapes = Shapes(
    // M3 default extraSmall = 4dp — keep it tight for chips/tags
    extraSmall = RoundedCornerShape(4.dp),
    
    // M3 default small = 8dp — used for text fields, small cards
    small = RoundedCornerShape(8.dp),
    
    // M3 default medium = 12dp — used for cards, dialogs (we clamp the bubbly default)
    medium = RoundedCornerShape(10.dp),
    
    // M3 default large = 16dp — used for bottom sheets
    large = RoundedCornerShape(14.dp),
    
    // M3 default extraLarge = 28dp — used for FAB, large containers (was 28dp bubble)
    extraLarge = RoundedCornerShape(16.dp)
)
```

---

### 2.4 Typography

```kotlin
// Type.kt
// Use Inter or the system default. Do NOT use SF Pro (Apple copyright).
// Inter is the closest openly licensed equivalent.

private val HybridTypography = Typography(
    // Large display text — used sparingly, one per screen
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,       // Apple Large Title equivalent
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp  // Apple-style tight tracking on large text
    ),
    // Section headers
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    // Body — the majority of your text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,       // Apple body size
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    // Labels, captions, secondary info
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
```

---

### 2.5 The Theme Wrapper

```kotlin
// Theme.kt

@Composable
fun HybridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HybridDarkColorScheme else HybridLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = HybridShapes,
        typography = HybridTypography,
        content = content
    )
}
```

Usage in your app:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HybridTheme {
                // Your app content
            }
        }
    }
}
```

---

## Part 3 — Components

### 3.1 Card (Fluent Depth Model)

Fluent uses **1dp border + mild elevation** instead of heavy drop shadows.

```kotlin
@Composable
fun HybridCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val borderColor = MaterialTheme.colorScheme.outline

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        Column(
            modifier = cardModifier
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            content = content
        )
    } else {
        Column(
            modifier = cardModifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            content = content
        )
    }
}
```

---

### 3.2 Button (Enforced 48dp Touch Target)

The 48dp minimum touch target is a hard M3/Android accessibility requirement. Never go below it.

```kotlin
@Composable
fun HybridPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,  // Tight 8dp radius — Fluent-style
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(48.dp)    // Hard minimum — do not reduce
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun HybridSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}
```

---

### 3.3 List Item (Apple HIG Density)

Apple's lists use a clear visual hierarchy: primary label + secondary label, clean separator lines, no icons cluttering every row unless necessary.

```kotlin
@Composable
fun HybridListItem(
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp    // Hair-thin Apple-style divider
            )
        }
    }
}
```

---

### 3.4 Frosted Glass Surface (Fluent Acrylic / Apple Vibrancy)

**Critical limitation:** True background blur (content behind the element is blurred) is extremely difficult on Android. What's achievable:

- `Modifier.blur()` — blurs the **content inside** the composable, not behind it. Android 12+ only.
- The Cloudy library — backports blur to pre-Android 12 (blurs content inside).
- Simulated frosted glass — semi-transparent overlay + subtle noise pattern (all Android versions).

For **real Acrylic/backdrop blur** (blur what's behind the surface), use `graphicsLayer { renderEffect = BlurEffect(...) }` — but this is **API 31+ only** and applies to what is inside the element, not behind it. True backdrop blur is not reliably achievable on Android without significant custom rendering work.

```kotlin
// Simulated frosted glass — works on ALL Android versions
// This is the practical approach for production apps
@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    alpha: Float = 0.75f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = alpha)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        content = content
    )
}

// Real blur (Android 12+ ONLY — use with version check)
@Composable
fun BlurredSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .blur(blurRadius)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
                .padding(16.dp),
            content = content
        )
    } else {
        // Fallback for Android 11 and below
        FrostedGlassSurface(modifier = modifier, content = content)
    }
}
```

---

### 3.5 Section Header (Apple Grouped Layout)

```kotlin
@Composable
fun HybridSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}
```

---

## Part 4 — Layout Principles (Apple Discipline, Applied to Android)

### Spacing Grid
Always use multiples of 4dp. Never use arbitrary values like 7dp or 13dp.

```
4dp  — minimum internal padding (icon labels, tight chips)
8dp  — standard internal element spacing
12dp — padding inside compact cards
16dp — screen edge margin (most screens)
24dp — section separation
32dp — major screen section breaks
48dp — minimum touch target height (non-negotiable)
```

### Screen Structure Template

```kotlin
@Composable
fun HybridScreen(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // your screen content
        }
    }
}
```

---

## Part 5 — Motion (Fluent Principles)

Fluent's motion rules: fast, purposeful, never decorative. Use these exact specs.

```kotlin
// Motion.kt

object HybridMotion {
    // Standard state transitions (button press, card tap)
    val StandardDuration = 200
    
    // Screen transitions, entering/exiting elements
    val NavigationDuration = 300
    
    // Enter: elements coming into view
    val EnterEasing = FastOutSlowInEasing
    
    // Exit: elements leaving view
    val ExitEasing = FastOutLinearInEasing
    
    // Shared element, persistent elements repositioning
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

// Animated visibility with Fluent-style motion
@Composable
fun HybridAnimatedVisibility(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = HybridMotion.NavigationDuration,
                easing = HybridMotion.EnterEasing
            )
        ) + slideInVertically(
            animationSpec = tween(HybridMotion.NavigationDuration, easing = HybridMotion.EnterEasing),
            initialOffsetY = { it / 4 }  // Subtle slide, not dramatic
        ),
        exit = fadeOut(
            animationSpec = tween(HybridMotion.StandardDuration, easing = HybridMotion.ExitEasing)
        ),
        content = content
    )
}
```

---

## Part 6 — What Uses Compose Cupertino (and What Doesn't)

Compose Cupertino gives you iOS-style components. Use them **only for these specific cases** where Android's M3 equivalent genuinely feels wrong to users:

| Use Cupertino | Use M3 |
|---|---|
| Date/Time pickers (spinning wheel, feels natural) | Buttons |
| Action sheets (bottom contextual menu) | Cards |
| Toggle switches (if you want iOS visual) | Navigation |
| Segmented controls | Text fields |
| Alert dialogs (optional) | Top app bar |

```kotlin
// Example: Cupertino action sheet vs M3 bottom sheet
// Use cupertino-adaptive which automatically uses M3 on Android if preferred

@Composable
fun ExampleAdaptiveButton() {
    // This renders as M3 on Android, iOS-style on iOS
    // Only useful if you're also targeting iOS (KMP)
    AdaptiveButton(onClick = {}) {
        AdaptiveText("Action")
    }
}
```

> **Honest note:** If you are Android-only, Compose Cupertino is of minimal value. Its biggest benefit is for **Kotlin Multiplatform** projects targeting both Android and iOS. On a pure Android app, just override M3 tokens — you'll get 90% of the aesthetic without the alpha library dependency risk.

---

## Part 7 — What to Avoid

| Don't Do This | Why |
|---|---|
| Dynamic Color (wallpaper-based) | Destroys your carefully chosen palette. Disable it. |
| M3's default shape tokens | Huge bubbly radii (extraLarge = 28dp) look like a consumer toy app |
| Drop shadows on everything | Muddy on dark backgrounds. Use 1dp borders instead |
| iOS back-swipe gesture as primary nav | Breaks Android predictive back. Use Android nav properly |
| SF Symbols | Copyrighted by Apple. Use Material Symbols or Lucide instead |
| Dynamic typography scale | Pick your scale and lock it. System font scaling is fine; custom scaling is not |
| More than 3 font sizes per screen | Creates visual noise. Apple's discipline works because it's restrictive |

---

## Part 8 — File Structure

```
app/src/main/java/your/app/
├── ui/
│   ├── theme/
│   │   ├── Theme.kt          ← HybridTheme wrapper
│   │   ├── Color.kt          ← All color tokens
│   │   ├── Shape.kt          ← Shape overrides
│   │   ├── Type.kt           ← Typography
│   │   └── Motion.kt         ← Animation constants
│   ├── components/
│   │   ├── HybridCard.kt
│   │   ├── HybridButton.kt
│   │   ├── HybridListItem.kt
│   │   ├── HybridSectionHeader.kt
│   │   └── FrostedGlassSurface.kt
│   └── screens/
│       └── (your feature screens, all wrapped in HybridTheme)
```

---

## Quick Reference Summary

| Layer | Decision |
|---|---|
| Foundation | Material 3 + Jetpack Compose |
| Colors | Custom tokens (zinc dark palette, iOS-inspired light palette) |
| Shapes | Clamped radii: 4/8/10/14/16dp (not M3 defaults) |
| Typography | 5 styles max. Tight letter-spacing on large text |
| Depth | 1dp border + surfaceVariant elevation. No heavy drop shadows |
| Glass effects | Simulated (all Android) or `Modifier.blur` (API 31+) |
| Motion | 200ms standard / 300ms navigation / FastOutSlowIn easing |
| Spacing | 4dp grid: 4/8/12/16/24/32/48dp — nothing else |
| Touch targets | 48dp minimum height. Hard rule |
| Apple components | Compose Cupertino, selectively, for pickers/action sheets only |
| Icons | Material Symbols (free) — not SF Symbols (Apple copyright) |

---

*All libraries referenced are real and open source as of 2025. Verify latest versions at Maven Central and the respective GitHub repos before adding to production.*
