@file:OptIn(ExperimentalFoundationApi::class)
package com.example.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.calculator.ui.theme.CalculatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.Exception
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

var OPENEDBRACKET:Int = 0
var BRACKETPRIORITY:Int = 10
var DONE: Boolean = false
var ANS: Answer = Answer()

sealed class Token {

    data class NUM(
        var value: String
    ) : Token() {
        override fun toString(): String = value
    }


    data class OPERATOR(
        var priority: Int,
        val value: String,
        var calculate: (ArrayList<Token>, Int) -> Unit,
        var needNumBefore: Boolean = true,
        val needNumAfter: Boolean = true
    ) : Token() {
        override fun toString(): String = value
    }

    data class FUNCTION(
        val value: String,
        val calculate: (ArrayList<Token>, Int) -> Unit
    ) : Token() {
        override fun toString(): String = value
    }

    data class OpenBracket(val value: String = "(") : Token() {
        override fun toString(): String = value
    }

    data class CloseBracket(val value: String = ")") : Token() {
        override fun toString(): String = value
    }

    fun copy(): Token {
        return when (this) {
            is FUNCTION -> FUNCTION(this.value, this.calculate)
            is OPERATOR -> OPERATOR(this.priority, this.value, this.calculate, this.needNumBefore, this.needNumAfter)
            is NUM -> NUM(this.value)
            is OpenBracket -> OpenBracket(this.value)
            is CloseBracket -> CloseBracket(this.value)
        }
    }
}

data class CalcButton(
    var displayStr: String,
    var value: Token = Token.NUM(value = displayStr),
    val icon: Painter? = null,
    val onClick: (
        list: ArrayList<Token>,
        tokenStr: MutableState<String>,
        token: Token,
        buttons: MutableList<CalcButton>
    ) -> Unit = ::addToken,
)

data class Answer(
    val solved: Boolean = false,
    val token: Token.NUM = Token.NUM(""),
    var error: Exception? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenList: ArrayList<Token> = arrayListOf()

        setContent {
            CalculatorTheme {
                val devideIcon = painterResource(R.drawable.divide)
                val multiplIcon = painterResource(R.drawable.mulltipl)
                val plusIcon = painterResource(R.drawable.plus)
                val minusIcon = painterResource(R.drawable.minus)
                val backspaceIcon = painterResource(R.drawable.backspace)
                val calcButtonsMain = remember {
                    mutableListOf(
                        CalcButton(displayStr = "AC", onClick = ::removeAll),
                        CalcButton(displayStr = "( )", value = Token.OpenBracket()),
                        CalcButton(
                            displayStr = "%",
                            value = Token.OPERATOR(
                                priority = 2,
                                value = "%",
                                calculate = ::percent,
                                needNumAfter = false
                            )
                        ),
                        CalcButton(
                            displayStr = "÷",
                            icon = devideIcon,
                            value = Token.OPERATOR(
                                priority = 1,
                                value = "÷",
                                calculate = ::divide
                            )
                        ),
                        CalcButton(displayStr = "7"),
                        CalcButton(displayStr = "8"),
                        CalcButton(displayStr = "9"),
                        CalcButton(
                            displayStr = "×",
                            icon = multiplIcon,
                            value = Token.OPERATOR(
                                priority = 1,
                                value = "×",
                                calculate = ::multiply
                            )
                        ),
                        CalcButton(displayStr = "4"),
                        CalcButton(displayStr = "5"),
                        CalcButton(displayStr = "6"),
                        CalcButton(
                            displayStr = "-",
                            icon = minusIcon,
                            value = Token.OPERATOR(
                                priority = 0,
                                value = "-",
                                calculate = ::subtract
                            )
                        ),
                        CalcButton(displayStr = "1"),
                        CalcButton(displayStr = "2"),
                        CalcButton(displayStr = "3"),
                        CalcButton(
                            displayStr = "+",
                            icon = plusIcon,
                            value = Token.OPERATOR(
                                priority = 0,
                                value = "+",
                                calculate = ::addition
                            )
                        ),
                        CalcButton(displayStr = "0"),
                        CalcButton(displayStr = "."),
                        CalcButton(
                            displayStr = "",
                            icon = backspaceIcon,
                            onClick = ::removeToken
                        ),
                        CalcButton(displayStr = "=", onClick = ::printAnswer)
                    )
                }
                val calcButtonsAdditional = remember {
                    mutableListOf (
                        CalcButton(displayStr = "AC", onClick = ::removeAll),
                        CalcButton(displayStr = "( )", value = Token.OpenBracket()),
                        CalcButton(
                            displayStr = "!",
                            value = Token.OPERATOR(8, "!", ::factorial, needNumAfter = false)
                        ),
                        CalcButton(
                            displayStr = "÷",
                            icon = devideIcon,
                            value = Token.OPERATOR(
                                priority = 1,
                                value = "÷",
                                calculate = ::divide
                            )
                        ),
                        CalcButton(displayStr = "inv", onClick = ::swap),
                        CalcButton(
                            displayStr = "π",
                            value = Token.OPERATOR(9, "π", ::piNum, false, false)
                        ),
                        CalcButton(
                            displayStr = "√",
                            value = Token.OPERATOR(7, "√", ::squareRoot, false, true)
                        ),
                        CalcButton(
                            displayStr = "×",
                            icon = multiplIcon,
                            value = Token.OPERATOR(
                                priority = 1,
                                value = "×",
                                calculate = ::multiply
                            )
                        ),
                        CalcButton(displayStr = "sin", value = Token.FUNCTION("sin(", ::sinus)),
                        CalcButton(
                            displayStr = "e",
                            value = Token.OPERATOR(9, "e", ::eNum, false, false)
                        ),
                        CalcButton(displayStr = "^", value = Token.OPERATOR(7, "^", ::power)),
                        CalcButton(
                            displayStr = "-",
                            icon = minusIcon,
                            value = Token.OPERATOR(
                                priority = 0,
                                value = "-",
                                calculate = ::subtract
                            )
                        ),
                        CalcButton(displayStr = "cos", value = Token.FUNCTION("cos(", ::cosinus)),
                        CalcButton(displayStr = "ln", value = Token.FUNCTION("ln(", ::lnFun)),
                        CalcButton(displayStr = "log", value = Token.FUNCTION("log(", ::logFun)),
                        CalcButton(
                            displayStr = "+",
                            icon = plusIcon,
                            value = Token.OPERATOR(
                                priority = 0,
                                value = "+",
                                calculate = ::addition
                            )
                        ),
                        CalcButton(displayStr = "tan", value = Token.FUNCTION("tan(", ::tangens)),
                        CalcButton(displayStr = "."),
                        CalcButton(
                            displayStr = "",
                            icon = backspaceIcon,
                            onClick = ::removeToken
                        ),
                        CalcButton(displayStr = "=", onClick = ::printAnswer)
                    )
                }

                val tokenString = remember { mutableStateOf("") }
                val previewAns = remember { mutableStateOf("") }
                val scroll = rememberScrollState()
                scrollToEnd(scroll)


                Column (
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    verticalArrangement = Arrangement.SpaceBetween
                ){
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 20.dp, bottom = 20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = tokenString.value,
                                fontSize = 80.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .horizontalScroll(scroll)
                            )
                            Text(
                                text = previewAns.value,
                                fontSize = 40.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                            )
                        }
                    }
                    val configuration = LocalConfiguration.current
                    val screenWidthDp = with(LocalDensity.current) { configuration.screenWidthDp }

                    val lazyListState = rememberLazyListState()
                    val snapBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
                    LazyRow (
                        state = lazyListState,
                        flingBehavior = snapBehavior,
                        modifier = Modifier
                            .weight(1.5f)
                    ) {
                        item {
                            GenerateButtons(
                                buttons = calcButtonsMain,
                                displayedStr = tokenString,
                                list = tokenList,
                                previewAns = previewAns,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(screenWidthDp.dp)
                            )
                        }
                        item {
                            GenerateButtons(
                                buttons = calcButtonsAdditional,
                                displayedStr = tokenString,
                                list = tokenList,
                                previewAns = previewAns,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(screenWidthDp.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


//Operator functions

fun divide(list: ArrayList<Token>, index:Int) {
    val result = (list[index-1] as Token.NUM).value.toDouble() / (list[index+1] as Token.NUM).value.toDouble()
    list[index] = Token.NUM(value = result.toString())
    list.removeAt(index+1)
    list.removeAt(index-1)
}

fun multiply(list: ArrayList<Token>, index:Int) {
    val result = (list[index-1] as Token.NUM).value.toDouble() * (list[index+1] as Token.NUM).value.toDouble()
    list[index] = Token.NUM(value = result.toString())
    list.removeAt(index+1)
    list.removeAt(index-1)
}

fun subtract(list:ArrayList<Token>, index:Int) {
    val result = (list[index-1] as Token.NUM).value.toDouble() - (list[index+1] as Token.NUM).value.toDouble()
    list[index] = Token.NUM(value = result.toString())
    list.removeAt(index+1)
    list.removeAt(index-1)
}

fun addition(list:ArrayList<Token>, index:Int) {
    val result = (list[index-1] as Token.NUM).value.toDouble() + (list[index+1] as Token.NUM).value.toDouble()
    list[index] = Token.NUM(value = result.toString())
    list.removeAt(index+1)
    list.removeAt(index-1)
}

fun percent(list: ArrayList<Token>, index: Int) {
    if (index >= 3 && (list[index - 2] is Token.OPERATOR && ((list[index - 2] as Token.OPERATOR).value == "+" || (list[index - 2] as Token.OPERATOR).value == "-"))) {
        list[index] = Token.NUM(value = ((list[index-3] as Token.NUM).value.toDouble() / 100 * (list[index - 1] as Token.NUM).value.toDouble()).toString())
        list.removeAt(index - 1)
    } else {
        list[index] = Token.NUM(value = ((list[index-1] as Token.NUM).value.toDouble() / 100).toString())
        list.removeAt(index - 1)
    }
}

fun unaryMinus(list:ArrayList<Token>, index:Int) {
    val result = (list[index+1] as Token.NUM).value.toDouble() * -1
    list[index] = Token.NUM(value = result.toString())
    list.removeAt(index+1)
}

fun factorial(list:ArrayList<Token>, index:Int) {
    val num = (list[index-1] as Token.NUM).value.toInt()
    var result = 1
    for (j in 1..num) {
        result *= j
    }
    list[index] = Token.NUM(result.toString())
    list.removeAt(index-1)
}

fun power(list: ArrayList<Token>, index: Int) {
    val result = Math.pow (
        (list[index - 1] as Token.NUM).value.toDouble(),
        (list[index + 1] as Token.NUM).value.toDouble()
    )
    list[index] = Token.NUM(result.toString())
    list.removeAt(index+1)
    list.removeAt(index-1)
}

fun squareRoot(list:ArrayList<Token>, index:Int) {
    val result = sqrt((list[index + 1] as Token.NUM).value.toDouble())
    list[index] = Token.NUM(result.toString())
    list.removeAt(index+1)
}

fun piNum(list:ArrayList<Token>, index:Int) {
    if ((index - 1) >= 0 && (list[index-1] is Token.NUM)) {
        val prev = (list[index-1] as Token.NUM)
        prev.value = (prev.value.toDouble() * Math.PI).toString()
        list.removeAt(index)
        println("AAAAAH!")
    } else {
        list[index] = Token.NUM(Math.PI.toString())
    }
}

fun eNum(list:ArrayList<Token>, index:Int) {
    if ((index - 1) >= 0 && (list[index-1] is Token.NUM)) {
        println(list.toString())
        val prev = (list[index-1] as Token.NUM)
        prev.value = (prev.value.toDouble() * Math.PI).toString()
        list.removeAt(index)
        println(list.toString())
    } else {
        list[index] = Token.NUM(Math.E.toString())
    }
}


//Function functions? : )


fun sinus(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = sin((list[index] as Token.NUM).value.toDouble()).toString()
}

fun cosinus(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = cos((list[index] as Token.NUM).value.toDouble()).toString()
}

fun tangens(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = tan((list[index] as Token.NUM).value.toDouble()).toString()
}

fun arcsinus(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = asin((list[index] as Token.NUM).value.toDouble()).toString()
}

fun arccosinus(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = acos((list[index] as Token.NUM).value.toDouble()).toString()
}

fun arctangens(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = atan((list[index] as Token.NUM).value.toDouble()).toString()
}

fun lnFun(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = ln((list[index] as Token.NUM).value.toDouble()).toString()
}

fun logFun(list:ArrayList<Token>, index:Int) {
    (list[index] as Token.NUM).value = log((list[index] as Token.NUM).value.toDouble(), 10.0).toString()
}


//onClick functions


fun addToken(list: ArrayList<Token>, tokenStr: MutableState<String>, tokenOrig: Token, buttons: List<CalcButton>) {
    var token = tokenOrig.copy()
    val behavior: IntArray = intArrayOf(6,0)

    if (list.isNotEmpty()) {
        when (val last: Token = list.last()) {
            is Token.NUM -> {
                behavior[0] = 0
            }
            is Token.OPERATOR -> {
                behavior[0] = if (last.needNumAfter) 1 else 2
            }
            is Token.FUNCTION -> {
                behavior[0] = 3
            }
            is Token.OpenBracket -> {
                behavior[0] = 4
            }
            is Token.CloseBracket -> {
                behavior[0] = 5
            }
        }
    }

    when (token) {
        is Token.NUM -> {
            behavior[1] = 0
        }
        is Token.OPERATOR -> {
            if (token.value == "-" && (behavior[0] == 6 || behavior[0] == 4 || behavior[0] == 1)) {
                token.priority = 3
                token.calculate = ::unaryMinus
                token.needNumBefore = false
            } else if (behavior[0] == 0 && (token.value == "π" || token.value == "e")) {
                token = token.copy()
                (token as Token.OPERATOR).needNumBefore = true
            }
            behavior[1] = if (token.needNumBefore) 1 else 2
        }
        is Token.FUNCTION -> {
            behavior[1] = 3
        }
        is Token.OpenBracket -> {
            behavior[1] = 4
        }
        else -> {

        }
    }
    if (behavior[1] == 0) {
        val curr: Token.NUM = (token as Token.NUM)
        if (behavior[0] != 0 && curr.value == ".") {
            curr.value = "0."
        }
        when (behavior[0]) {
            0 -> {
                val last: Token.NUM = (list.last() as Token.NUM)
                if (curr.value != "." || "." !in last.value) last.value += curr.value
            }
            5, 2 -> {
                list.add(Token.OPERATOR(
                    priority = 1 + OPENEDBRACKET * BRACKETPRIORITY,
                    value = "×",
                    calculate = ::multiply
                ))
                list.add(curr)
            }
            else -> {
                list.add(curr)
            }
        }
    } else if (behavior[1] == 1) {
        (token as Token.OPERATOR).priority += OPENEDBRACKET * BRACKETPRIORITY
        when (behavior[0]) {
            0, 2, 5 -> {
                list.add(token)
            }
            1 -> {
                list.removeLast()
                list.add(token)
            }
        }
    } else if (behavior[1] == 2) {
        (token as Token.OPERATOR).priority += OPENEDBRACKET * BRACKETPRIORITY
        when (behavior[0]) {
            3, 4, 6 -> {
                list.add(token)
            }
            1 -> {
                /*
                list.add(Token.OpenBracket())
                OPENEDBRACKET++
                 */
                list.add(token)
            }
            else -> {
                list.add(Token.OPERATOR(
                    priority = 1 + OPENEDBRACKET * BRACKETPRIORITY,
                    value = "×",
                    calculate = ::multiply
                ))
                list.add(Token.OpenBracket())
                OPENEDBRACKET++
                list.add(token)
            }
        }
    } else if (behavior[1] == 3 || behavior[1] == 4) {
        if (behavior[0] == 6) {
            if (behavior[1] == 3) list.add(token)
            else {
                list.add(Token.OpenBracket())
            }
            OPENEDBRACKET++
        } else {
            val last = list.last()
            val curr: Token =
                if (token is Token.OpenBracket) {
                    if ((last is Token.NUM || last is Token.CloseBracket || (last is Token.OPERATOR && !last.needNumAfter)) && OPENEDBRACKET != 0) {
                        Token.CloseBracket()
                    } else {
                        Token.OpenBracket()
                    }
                } else {
                    token
                }
            if (curr is Token.CloseBracket) {
                list.add(curr)
                OPENEDBRACKET--
            } else {
                if (last is Token.NUM || last is Token.CloseBracket || (last is Token.OPERATOR && !last.needNumAfter)) {
                    list.add(
                        Token.OPERATOR(
                            priority = 1 + OPENEDBRACKET * BRACKETPRIORITY,
                            value = "×",
                            calculate = ::multiply
                        )
                    )
                }
                list.add(curr)
                OPENEDBRACKET++
            }
        }
    }
    tokenStr.value = list.joinToString(separator = "") { it.toString() }
    DONE = true
}

fun removeToken(list: ArrayList<Token>, tokenStr: MutableState<String>, token: Token, buttons: List<CalcButton>) {
    if (list.isNotEmpty()){
        val last:Token =  list.last()
        if (last is Token.NUM && last.value.isNotEmpty()){
            last.value = last.value.dropLast(1)
            if (last.value.isEmpty()){
                list.removeLast()
            }
        } else if (last is Token.CloseBracket){
            OPENEDBRACKET++
            list.removeLast()
        } else if ((last is Token.OpenBracket) || (last is Token.FUNCTION)){
            OPENEDBRACKET--
            list.removeLast()
        } else {
            list.removeLast()
        }
        tokenStr.value = list.joinToString(separator = "") { it.toString() }
    }
}

fun removeAll(list: ArrayList<Token>, tokenStr: MutableState<String>, token: Token, buttons: List<CalcButton>) {
    list.clear()
    tokenStr.value = ""
    OPENEDBRACKET = 0
}

fun swap(list: ArrayList<Token>, tokenStr: MutableState<String>, token: Token, buttons: MutableList<CalcButton> ) {
    if ((buttons[8].value as Token.FUNCTION).value == "sin(") {
        buttons[16] = CalcButton("atan",Token.FUNCTION("atan(", ::arctangens))
        buttons[12] = CalcButton("acos" ,Token.FUNCTION("acos(", ::arccosinus))
        buttons[8] = CalcButton("asin", Token.FUNCTION("asin(", ::arcsinus))
    } else {
        buttons[16] = CalcButton("tan", Token.FUNCTION("tan(", ::tangens))
        buttons[12] = CalcButton("cos",Token.FUNCTION("cos(", ::cosinus))
        buttons[8] = CalcButton("sin", Token.FUNCTION("sin(", ::sinus))
    }
    tokenStr.value += " "
    tokenStr.value.dropLast(1)
}

fun printAnswer(list: ArrayList<Token>, tokenStr: MutableState<String>, token: Token, buttons: List<CalcButton>) {
    val ans: Answer = calculate(list)
    ANS = Answer(ans.solved, ans.token, ans.error)
    if (ans.solved) {
        list.clear()
        list.add(ans.token)
        (list[0] as Token.NUM).value = ansToString(ans)
        tokenStr.value = (list[0] as Token.NUM).value
    }
}

//Other functions

fun calculate(tokenList: ArrayList<Token>): Answer {
    if (tokenList.size<2) return Answer(solved = false)
    val list: ArrayList<Token> = arrayListOf()
    for (i in tokenList) {
        list.add(i.copy())
    }
    var  openedBracket: Int = OPENEDBRACKET
    while (openedBracket != 0) {
        list.add(Token.CloseBracket())
        openedBracket--
    }
    var i = 0
    while (i < list.size) {
        if (list[i] is Token.NUM) {
            i = removeBrackets(list, i)
        }
        i++
    }
    val operators = list.filterIsInstance<Token.OPERATOR>().toTypedArray()
    operators.sortByDescending { it.priority }
    for (operator in operators) { val index = list.indexOf(operator)
        val correction: Int = if (operator.needNumBefore) 1 else 0
        try {
            operator.calculate(list, index)
            removeBrackets(list, index - correction)
        } catch (e: Exception) {
            println(list[index].toString())
            return Answer(error = e, solved = false)
        }
    }
    println(tokenList.toString())
    if (list[0] !is Token.NUM) return Answer(token = Token.NUM("0"), solved = false)
    return Answer(token = (list[0] as Token.NUM), solved = true)
}

fun removeBrackets(list:ArrayList<Token>, index:Int): Int {
    var cursor = index
    while (cursor>0) {
        if ((!((list[cursor-1] is Token.OpenBracket) || (list[cursor-1] is Token.FUNCTION))) || (list[cursor+1] !is Token.CloseBracket)) {
            break
        }
        if (list[cursor-1] is Token.FUNCTION) {
            (list[cursor-1] as Token.FUNCTION).calculate(list, cursor)
        }
        list.removeAt(cursor+1)
        list.removeAt(cursor-1)
        cursor--
    }
    return cursor
}

fun ansToString(ans:Answer): String {
    val result = ans.token.value.toDouble()
    val roundedResult:String = if (result % 1 == 0.0) {
        result.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.6f", result).trimEnd('0').dropLastWhile { it == '.' }
    }
    return roundedResult
}

@SuppressLint("CoroutineCreationDuringComposition")
fun scrollToEnd(scroll: ScrollState) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            if (DONE) {
                scroll.scrollTo(scroll.maxValue)
                DONE = false
            }
        } catch (_: Exception) {

        }
    }
}

@Composable
fun GenerateButtons(buttons: MutableList<CalcButton>, displayedStr: MutableState<String>, list: ArrayList<Token>, previewAns : MutableState<String>, modifier: Modifier) {
    Column (
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier

    ) {
        Column(modifier = Modifier.padding(10.dp))
        {
            for (i in 0..4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (j in 0..3) {
                        val button: CalcButton = buttons[i * 4 + j]
                        Button(
                            onClick = {
                                println(asin(0.1))
                                ANS = Answer()
                                button.onClick(list, displayedStr, button.value, buttons)
                                println(list.toString())
                                if (ANS.solved) {
                                    previewAns.value = if (ANS.error == null) {
                                        "Error"
                                    } else {
                                        "Error"
                                    }
                                } else {
                                    try {
                                        val ans = calculate(list)
                                        if (list.size > 1) {
                                            if (ans.error == null) {
                                                previewAns.value = ansToString(ans)
                                            }
                                        } else {
                                            previewAns.value = ""
                                        }
                                    } catch (_: Exception) {

                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onSecondary),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.8f)
                                .padding(start = 5.dp, end = 5.dp)
                        ) {
                            if (button.icon != null) {
                                Icon(
                                    painter = button.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val size = remember { mutableDoubleStateOf(100.0) }
                                val ready = remember { mutableStateOf(false)}
                                Text(
                                    text = button.displayStr,
                                    modifier = Modifier.drawWithContent {
                                        if (ready.value) drawContent()
                                    },
                                    overflow = TextOverflow.Clip,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) size.doubleValue *= 0.8
                                        else ready.value = true
                                    },
                                    fontSize = size.doubleValue.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
