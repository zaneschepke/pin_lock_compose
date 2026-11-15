package xyz.teamgravity.pin_lock_compose

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** What the pin indicator shows while an entered pin is being handled. */
private enum class PinStatus {
    Idle,
    Error,
    Success,
}

// Long enough to see the result before the screen moves on or the dots clear
private const val SUCCESS_HOLD_MS = 160L
private const val ERROR_HOLD_MS = 420L

/**
 * Covers the whole screen and displays the PinLock. This composable makes the user enter pin if it already exists. If there is
 * no saved pin, it prompts the user to create pin.
 *
 * Colors default to the current [MaterialTheme], so the pin pad follows the app theme.
 *
 * @param title
 * Container for title text. It passes the pinExists boolean, so you can decide what title to show. For pinExists value, it passes true
 * if pin already exists, false if pin does not exist.
 * @param backgroundColor
 * Background color of container.
 * @param textColor
 * Color of digits, icons and empty pin indicators.
 * @param onPinCorrect
 * Gets called when entered pin matches genuine pin.
 * @param onPinIncorrect
 * Gets called when entered pin does not match genuine.
 * @param onPinCreated
 * Gets called when pin gets saved.
 * @param accentColor
 * Color of filled pin indicators and focused keys.
 * @param header
 * Optional content above the title, like an app icon.
 */
@Composable
fun PinLock(
    title: @Composable (pinExists: Boolean) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onPinCorrect: () -> Unit,
    onPinIncorrect: () -> Unit,
    onPinCreated: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    header: @Composable () -> Unit = {},
) {
    val numbers = rememberMutableStateListOf<Int>()
    val pinExists = remember { PinManager.pinExists() }
    var status by remember { mutableStateOf(PinStatus.Idle) }
    var errors by remember { mutableIntStateOf(0) }

    val currentOnPinCorrect by rememberUpdatedState(onPinCorrect)
    val currentOnPinIncorrect by rememberUpdatedState(onPinIncorrect)
    val currentOnPinCreated by rememberUpdatedState(onPinCreated)

    LaunchedEffect(numbers.size) {
        if (numbers.size < PinConst.PIN_LENGTH) return@LaunchedEffect
        when {
            !pinExists -> {
                PinManager.savePin(numbers)
                status = PinStatus.Success
                delay(SUCCESS_HOLD_MS)
                currentOnPinCreated()
            }

            PinManager.checkPin(numbers) -> {
                status = PinStatus.Success
                delay(SUCCESS_HOLD_MS)
                currentOnPinCorrect()
            }

            else -> {
                status = PinStatus.Error
                errors++
                currentOnPinIncorrect()
                delay(ERROR_HOLD_MS)
            }
        }
        numbers.clear()
        status = PinStatus.Idle
    }

    BasePinLock(
        title = { title(pinExists) },
        header = header,
        backgroundColor = backgroundColor,
        textColor = textColor,
        accentColor = accentColor,
        status = status,
        errors = errors,
        numbers = numbers,
        modifier = modifier,
        onNumberChange = { number ->
            if (status == PinStatus.Idle && numbers.size < PinConst.PIN_LENGTH) numbers.add(number)
        },
        onBackspace = { if (status == PinStatus.Idle) numbers.removeLastOrNull() },
        onClear = { if (status == PinStatus.Idle) numbers.clear() },
    )
}

/**
 * Covers the whole screen and displays the PinLock. This composable first makes the user enter original pin. Once user successfully
 * authenticates with his pin, it prompts the user to create new pin. It replaces the old pin. This composable is meant to be used for
 * changing pin when there is already saved pin. Don't use this composable if there is no saved pin yet.
 * Use [xyz.teamgravity.pin_lock_compose.PinLock] instead for creating pin for the first time.
 *
 * @param title
 * Container for title text. It passes the authenticated boolean, so you can decide what title to show. For authenticated value, it passes
 * true once user authenticates using his pin. It passes false if user is not authenticated yet.
 * @param backgroundColor
 * Background color of container.
 * @param textColor
 * Color of digits, icons and empty pin indicators.
 * @param onPinIncorrect
 * Gets called when entered pin does not match genuine pin.
 * @param onPinChanged
 * Gets called when pin is changed successfully.
 * @param accentColor
 * Color of filled pin indicators and focused keys.
 * @param header
 * Optional content above the title, like an app icon.
 */
@Composable
fun ChangePinLock(
    title: @Composable (authenticated: Boolean) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onPinIncorrect: () -> Unit,
    onPinChanged: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    header: @Composable () -> Unit = {},
) {
    var authenticated by rememberSaveable { mutableStateOf(false) }
    val numbers = rememberMutableStateListOf<Int>()
    var status by remember { mutableStateOf(PinStatus.Idle) }
    var errors by remember { mutableIntStateOf(0) }

    val currentOnPinIncorrect by rememberUpdatedState(onPinIncorrect)
    val currentOnPinChanged by rememberUpdatedState(onPinChanged)

    LaunchedEffect(numbers.size) {
        if (numbers.size < PinConst.PIN_LENGTH) return@LaunchedEffect
        when {
            authenticated -> {
                PinManager.savePin(numbers)
                status = PinStatus.Success
                delay(SUCCESS_HOLD_MS)
                currentOnPinChanged()
            }

            PinManager.checkPin(numbers) -> {
                status = PinStatus.Success
                delay(SUCCESS_HOLD_MS)
                authenticated = true
            }

            else -> {
                status = PinStatus.Error
                errors++
                currentOnPinIncorrect()
                delay(ERROR_HOLD_MS)
            }
        }
        numbers.clear()
        status = PinStatus.Idle
    }

    BasePinLock(
        title = { title(authenticated) },
        header = header,
        backgroundColor = backgroundColor,
        textColor = textColor,
        accentColor = accentColor,
        status = status,
        errors = errors,
        numbers = numbers,
        modifier = modifier,
        onNumberChange = { number ->
            if (status == PinStatus.Idle && numbers.size < PinConst.PIN_LENGTH) numbers.add(number)
        },
        onBackspace = { if (status == PinStatus.Idle) numbers.removeLastOrNull() },
        onClear = { if (status == PinStatus.Idle) numbers.clear() },
    )
}

/**
 * This is base pin lock composable. This is dumb composable that only displays PinLock.
 *
 * @param status
 * Colors the pin indicator for a wrong or accepted pin.
 * @param errors
 * Whenever this value changes, the pin indicator shakes.
 * @param numbers
 * List of int that represents entered pin.
 * @param onNumberChange
 * Gets called whenever number buttons are clicked. It also passes what number is clicked.
 * @param onBackspace
 * Gets called whenever backspace button is clicked.
 * @param onClear
 * Gets called when backspace button is long pressed.
 */
@Composable
private fun BasePinLock(
    title: @Composable () -> Unit,
    header: @Composable () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    status: PinStatus,
    errors: Int,
    numbers: List<Int>,
    modifier: Modifier,
    onNumberChange: (number: Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val view = LocalView.current
    val rootFocus = remember { FocusRequester() }
    val firstKeyFocus = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        when (status) {
            PinStatus.Error -> view.reject()
            PinStatus.Success -> view.confirm()
            PinStatus.Idle -> Unit
        }
    }
    // Lets a hardware keyboard type without touching the screen first
    LaunchedEffect(Unit) { rootFocus.requestFocus() }

    val onKey: (Int) -> Unit = { number ->
        view.tap()
        onNumberChange(number)
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = event.key.digit()
                when {
                    digit != null -> {
                        onKey(digit)
                        true
                    }

                    event.key == Key.Backspace || event.key == Key.Delete -> {
                        onBackspace()
                        true
                    }

                    // Focus search does not enter children from a focused parent, so a d-pad
                    // press on the container moves focus onto the keypad
                    rootFocused && event.key in DPAD_KEYS -> {
                        firstKeyFocus.requestFocus()
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { rootFocused = it.isFocused }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        // Fit three keys across and four down with room left for the title and indicator
        val byWidth = (maxWidth - 48.dp - KEY_GAP_HORIZONTAL * 2) / 3
        val byHeight = (maxHeight - 260.dp - KEY_GAP_VERTICAL * 3) / 4
        val keySize = minOf(byWidth, byHeight).coerceIn(MIN_KEY_SIZE, MAX_KEY_SIZE)
        val keyColor = textColor.copy(alpha = 0.08f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            header()
            title()
            Spacer(Modifier.height(24.dp))
            PinIndicator(
                count = numbers.size,
                status = status,
                errors = errors,
                emptyColor = textColor.copy(alpha = 0.38f),
                accentColor = accentColor,
                errorColor = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(40.dp))
            Column(verticalArrangement = Arrangement.spacedBy(KEY_GAP_VERTICAL)) {
                listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP_HORIZONTAL)) {
                        row.forEach { number ->
                            NumberKey(
                                number = number,
                                size = keySize,
                                containerColor = keyColor,
                                textColor = textColor,
                                accentColor = accentColor,
                                focusRequester = if (number == 1) firstKeyFocus else null,
                                onClick = onKey,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP_HORIZONTAL)) {
                    Spacer(Modifier.size(keySize))
                    NumberKey(
                        number = 0,
                        size = keySize,
                        containerColor = keyColor,
                        textColor = textColor,
                        accentColor = accentColor,
                        onClick = onKey,
                    )
                    PinKey(
                        size = keySize,
                        containerColor = Color.Transparent,
                        textColor = textColor,
                        accentColor = accentColor,
                        onClick = {
                            view.tap()
                            onBackspace()
                        },
                        onLongClick = {
                            view.reject()
                            onClear()
                        },
                    ) {
                        Icon(
                            imageVector = Backspace,
                            tint = textColor,
                            contentDescription = stringResource(id = R.string.cd_backspace),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Row of dots showing how many digits are entered. Each dot fills with a small spring, the row shakes on a wrong pin and
 * every dot takes the error or accent color while a pin is handled.
 */
@Composable
private fun PinIndicator(
    count: Int,
    status: PinStatus,
    errors: Int,
    emptyColor: Color,
    accentColor: Color,
    errorColor: Color,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errors) {
        if (errors == 0) return@LaunchedEffect
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 360
                -18f at 50
                16f at 110
                -12f at 170
                8f at 230
                -4f at 290
            },
        )
    }

    val description = stringResource(R.string.cd_pin_progress, count, PinConst.PIN_LENGTH)
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(PinConst.PIN_LENGTH) { index ->
            val fillColor = when (status) {
                PinStatus.Error -> errorColor
                PinStatus.Success, PinStatus.Idle -> accentColor
            }
            PinDot(
                filled = index < count,
                fillColor = fillColor,
                emptyColor = emptyColor,
            )
        }
    }
}

@Composable
private fun PinDot(
    filled: Boolean,
    fillColor: Color,
    emptyColor: Color,
) {
    val fill by animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "PinDotFill",
    )
    val color by animateColorAsState(targetValue = fillColor, label = "PinDotColor")
    val ringColor by animateColorAsState(
        targetValue = if (filled) fillColor else emptyColor,
        label = "PinDotRing",
    )

    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = 1.5.dp.toPx()
        val radius = size.minDimension / 2
        drawCircle(color = ringColor, radius = radius - stroke / 2, style = Stroke(width = stroke))
        if (fill > 0f) {
            drawCircle(color = color, radius = radius * fill, center = Offset(size.width / 2, size.height / 2))
        }
    }
}

@Composable
private fun NumberKey(
    number: Int,
    size: Dp,
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: (number: Int) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    PinKey(
        size = size,
        containerColor = containerColor,
        textColor = textColor,
        accentColor = accentColor,
        focusRequester = focusRequester,
        onClick = { onClick(number) },
    ) {
        Text(
            text = number.toString(),
            color = textColor,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

/**
 * Round key that shrinks a little while pressed and grows with an accent ring while focused, as on TV.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinKey(
    size: Dp,
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            focused -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "PinKeyScale",
    )
    val background by animateColorAsState(
        targetValue = if (pressed || focused) textColor.copy(alpha = 0.16f) else containerColor,
        label = "PinKeyBackground",
    )
    val ringColor by animateColorAsState(
        targetValue = if (focused) accentColor else Color.Transparent,
        label = "PinKeyRing",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .border(width = 2.dp, color = ringColor, shape = CircleShape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor),
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

private val KEY_GAP_HORIZONTAL = 24.dp
private val KEY_GAP_VERTICAL = 16.dp
private val MIN_KEY_SIZE = 56.dp
private val MAX_KEY_SIZE = 80.dp

private val DPAD_KEYS = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter)

private fun Key.digit(): Int? = when (this) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

private fun View.tap() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

private fun View.confirm() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.KEYBOARD_TAP,
    )
}

private fun View.reject() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS,
    )
}
