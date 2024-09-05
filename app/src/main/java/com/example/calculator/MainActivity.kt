@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculator.ui.theme.CalculatorTheme
import com.example.mathparser.MathParser

class CalculatorViewModel : ViewModel() {
    private val calculator = MathParser()
    private val _mainString = mutableStateOf("")
    val mainString get() = _mainString
    private val _additionalString = mutableStateOf("")
    val additionalString get() = _additionalString
    private val _radOrDeg = mutableStateOf("RAD")
    val radOrDeg get() = _radOrDeg
    private val _inv = mutableStateOf(false)
    val  inv get() = _inv

    private fun updateValues() {
        _mainString.value = calculator.mainString
        _additionalString.value = calculator.previewString
    }

    fun onDigitClick(input: String) {
        calculator.addToken(false,input)
        updateValues()
    }

    fun onOperatorClick(input: String) {
        calculator.addToken(true,input)
        updateValues()
    }

    fun onBackspace() {
        calculator.removeToken()
        updateValues()
    }

    fun onCalculate() {
        calculator.writeAnswer()
        updateValues()
    }

    fun onClear() {
        calculator.clear()
        updateValues()
    }
    fun switchDegRad() {
        calculator.useDegrees = !calculator.useDegrees
        if (calculator.useDegrees) _radOrDeg.value = "DEG"
        else _radOrDeg.value = "RAD"
        calculator.previewCalculate()
        updateValues()
    }
    fun invert() {
        _inv.value = !inv.value
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorTheme {
                enableEdgeToEdge()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    val viewModel: CalculatorViewModel = viewModel()
    CalculatorApp(viewModel = viewModel)
}

@Composable
fun CalculatorApp(viewModel: CalculatorViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    var rowHeight by remember { mutableStateOf(0.dp) }

    val targetHeight = if (expanded) rowHeight * 2 + 20.dp else 0.dp
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 300)
    )


    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                .background(MaterialTheme.colorScheme.onSecondary)
        ) {
            Box(contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
            ) {
                /*
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                )
                 */
            }
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxWidth()
            ) {
                CalcTextField(
                    text = viewModel.mainString,
                    textColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = viewModel.additionalString.value,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.End,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize*1.6,
                    fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                /*
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.inverseSurface,
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.BottomCenter)
                )
                 */
            }
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp)
            ) {
                Column (
                    modifier = Modifier
                        .weight(1f)
                ) {
                    val density = LocalDensity.current
                    Row (
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp)
                            .onGloballyPositioned { coordinates ->
                                rowHeight = with(density) { coordinates.size.height.toDp() }
                            }
                    ){
                        if (viewModel.inv.value) {
                            MiniButton("x²", onClick = { viewModel.onOperatorClick("18") })
                        } else {
                            MiniButton("√", onClick = { viewModel.onOperatorClick("7") })
                        }
                        MiniButton("π", onClick = {viewModel.onOperatorClick( "16") })
                        MiniButton("^", onClick = {viewModel.onOperatorClick( "6") })
                        MiniButton("!", onClick = {viewModel.onOperatorClick( "9") })
                    }
                }
                Column (
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .weight(0.1f)
                ) {
                    Button(
                        onClick = { expanded = !expanded },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .size(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSecondary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            tint = MaterialTheme.colorScheme.secondary,
                            contentDescription = "More",
                            modifier = Modifier.graphicsLayer {
                                this.rotationX = rotation
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier
                    .height(animatedHeight)
                    .verticalScroll(state = ScrollState(0))) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 10.dp)
                            ) {
                                MiniButton(viewModel.radOrDeg.value, onClick = {viewModel.switchDegRad()})
                                if (viewModel.inv.value) {
                                    MiniButton("sin⁻¹", onClick = { viewModel.onOperatorClick("19") })
                                    MiniButton("cos⁻¹", onClick = { viewModel.onOperatorClick("20") })
                                    MiniButton("tan⁻¹", onClick = { viewModel.onOperatorClick("21") })
                                } else {
                                    MiniButton("sin", onClick = { viewModel.onOperatorClick("10") })
                                    MiniButton("cos", onClick = { viewModel.onOperatorClick("11") })
                                    MiniButton("tan", onClick = { viewModel.onOperatorClick("12") })
                                }
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .weight(0.1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 10.dp)
                            ) {
                                MiniButton("INV", onClick = {viewModel.invert()})
                                MiniButton("e", onClick = {viewModel.onOperatorClick( "17")})
                                if (viewModel.inv.value) {
                                    MiniButton("e  ͯ", onClick = {viewModel.onOperatorClick( "22")})
                                    MiniButton("10  ͯ", onClick = {
                                        viewModel.onDigitClick("10")
                                        viewModel.onOperatorClick( "6")
                                    })
                                } else {
                                    MiniButton("ln", onClick = {viewModel.onOperatorClick( "13")})
                                    MiniButton("log", onClick = {viewModel.onOperatorClick( "14")})
                                }
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .weight(0.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ButtonGrid(viewModel)
        }
    }
}


@Composable
fun MiniButton(text: String, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(2.dp),
        modifier = Modifier
            .width(70.dp)
            .padding(horizontal = 3.dp)
        ) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.headlineLarge.fontSize*0.8,
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
        )
    }
}

@Composable
fun CalculatorButton(text: String, backColor: Color, onColor: Color, viewModel: CalculatorViewModel) {
    Button(
        onClick = {
            when(text) {
                "( )" -> {
                    viewModel.onOperatorClick( "0")
                }
                "%" -> {
                    viewModel.onOperatorClick( "8")
                }
                "÷" -> {
                    viewModel.onOperatorClick( "5")
                }
                "×" -> {
                    viewModel.onOperatorClick( "4")
                }
                "-" -> {
                    viewModel.onOperatorClick( "15")
                }
                "+" -> {
                    viewModel.onOperatorClick( "2")
                }
                "AC" -> {
                    viewModel.onClear()
                }
                "=" -> {
                    viewModel.onCalculate()
                }
                "⌫" -> {
                    viewModel.onBackspace()
                }
                else -> {
                    viewModel.onDigitClick(text)
                }
            }
        },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .size(83.dp)
            .padding(start = 2.dp, end = 2.dp, top = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.headlineLarge.fontSize,
            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
            color = onColor
        )
    }
}

@Composable
fun ButtonGrid(viewModel: CalculatorViewModel) {
    val buttons = listOf(
        listOf("AC", "( )", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )
    Column(modifier = Modifier) {
        for (row in buttons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { button ->
                    var backColor = MaterialTheme.colorScheme.onSecondary
                    var onColor = MaterialTheme.colorScheme.secondary
                    when (button) {
                        "( )", "%", "÷", "×", "-", "+" -> {
                            backColor = MaterialTheme.colorScheme.secondaryContainer
                            onColor = MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        "AC" -> {
                            backColor = MaterialTheme.colorScheme.tertiaryContainer
                            onColor = MaterialTheme.colorScheme.onTertiaryContainer
                        }

                        "=" -> {
                            backColor = MaterialTheme.colorScheme.primaryContainer
                            onColor = MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    }
                    CalculatorButton(button, backColor, onColor, viewModel)
                }
            }
        }
    }
}

@Composable
fun CalcTextField(modifier: Modifier, textColor: Color, text: MutableState<String>) {
    val typography = MaterialTheme.typography.displayLarge
    val startSize: TextUnit = typography.fontSize * 1.5f
    val minSize = startSize/2f
    var fontSize by remember { mutableStateOf(startSize) }

    val textMeasurer = rememberTextMeasurer()

    val scrollState = rememberScrollState()

    val animatedFontSize by animateFloatAsState(
        targetValue = fontSize.value,
        animationSpec = tween(durationMillis = 50)
    )

    CompositionLocalProvider(
        LocalTextInputService provides null
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.CenterEnd,
            modifier = modifier
        ) {
            val boxWidth = constraints.maxWidth.toFloat()

            LaunchedEffect(text.value) {
                var tmpFontSize = startSize
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(text.value),
                    style = TextStyle(
                        fontSize = tmpFontSize,
                        fontFamily = typography.fontFamily
                    )
                )
                val textWidth = textLayoutResult.size.width.toFloat()
                if (textWidth > boxWidth) tmpFontSize *= (boxWidth / textWidth)
                fontSize = if (tmpFontSize > minSize) tmpFontSize else minSize
            }

            LaunchedEffect(scrollState.maxValue) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .horizontalScroll(scrollState)
            ) {
                BasicTextField(
                    value = text.value,
                    onValueChange = {
                        text.value = it
                    },
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = animatedFontSize.sp,
                        fontFamily = typography.fontFamily,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .padding(0.dp)
                        .width(IntrinsicSize.Max)
                )
            }
        }
    }
}
