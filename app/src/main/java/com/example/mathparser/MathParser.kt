package com.example.mathparser

import androidx.arch.core.internal.SafeIterableMap.SupportRemove
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MathParser {

    data class Operator (
        val isParenthesis: Boolean = false,
        val priority: Int = 0,
        val value: String,
        val needNumBefore: Boolean = true,
        val needNumAfter: Boolean = true,
    )
    data class Token (
        var isOperator:Boolean = false,
        var value:String = ""
    )
    data class CalcToken (
        var isOperator:Boolean = false,
        var value:Double = 0.0
    )

    val parenthesisPriority:Int = 16
    val operatorsList:List<Operator> = listOf(
        Operator(isParenthesis = true, needNumBefore = false, needNumAfter = false, value = "(", priority = 1),                          //0
        Operator(isParenthesis = true, needNumBefore = false, needNumAfter = false, value = ")", priority = -1),                         //1
        Operator(priority = 0, value = "+"),                                                                                             //2
        Operator(priority = 0, value = "-"),                                                                                             //3
        Operator(priority = 1, value = "×"),                                                                                             //4
        Operator(priority = 1, value = "÷"),                                                                                             //5
        Operator(priority = 2, value = "^"),                                                                                             //6
        Operator(priority = 2, value = "√", needNumBefore = false),                                                                      //7
        Operator(priority = 3, value = "%", needNumAfter = false),                                                                       //8
        Operator(priority = 5, value = "!", needNumAfter = false),                                                                       //9
        Operator(priority = 4, value = "sin(", needNumBefore = false, isParenthesis = true),                                             //10
        Operator(priority = 4, value = "cos(", needNumBefore = false, isParenthesis = true),                                             //11
        Operator(priority = 4, value = "tan(", needNumBefore = false, isParenthesis = true),                                             //12
        Operator(priority = 4, value = "ln(", needNumBefore = false, isParenthesis = true),                                              //13
        Operator(priority = 4, value = "log(", needNumBefore = false, isParenthesis = true),                                             //14
        Operator(priority = 0, value = "-", needNumBefore = false),                                                                      //15
        Operator(priority = 6, value = "π", needNumBefore = false, needNumAfter = false),                                                //16
        Operator(priority = 6, value = "e", needNumBefore = false, needNumAfter = false),                                                //17
        Operator(priority = 5, value = "²", needNumAfter = false),                                                                       //18
        Operator(priority = 4, value = "sin⁻¹(", needNumBefore = false, isParenthesis = true),                                           //19
        Operator(priority = 4, value = "cos⁻¹(", needNumBefore = false, isParenthesis = true),                                           //20
        Operator(priority = 4, value = "tan⁻¹(", needNumBefore = false, isParenthesis = true),                                           //21
        Operator(priority = 4, value = "exp(", needNumBefore = false, isParenthesis = true),                                             //22

    )

    var useDegrees:Boolean = false
    var cursor:IntArray = intArrayOf(0,0)
    var openedParenthesis:Int = 0
    var tokenList:ArrayList<Token> = arrayListOf()
    var lastAns:Double? = null
    var lastError:String = ""
    var mainString:String = ""
    var previewString:String = ""


    private fun getOperator(token: Token) :Operator {
        return operatorsList[token.value.toInt()]
    }
    private fun getOperator(token: CalcToken) :Operator {
        return operatorsList[token.value.toInt()]
    }
    /*
    fun setCursor(rawCursor: Int): Int {
        var counter = 0
        val newCursor = intArrayOf(0,0)
        for (i in tokenList) {
            var tmp: Int
            if (i.isOperator) {
                tmp = getOperator(i).value.length
                if (counter + tmp > rawCursor) {
                    break
                } else {
                    counter += tmp
                    newCursor[0]++
                    if (counter + tmp == rawCursor) {
                        break
                    }
                }
            }
            else {
                tmp = i.value.length
                counter += tmp
                newCursor[0]++
                if (counter + tmp > rawCursor) {
                    newCursor[1] = tmp
                    break
                } else {
                    if (counter + tmp == rawCursor) {
                        break
                    }
                }
            }
        }
        cursor = newCursor
        return counter
    }
    */
    /*
    fun validateInput(token: Token) :Token {



        when(input) {

        }

        return outputToken
    }
    */
    fun previewCalculate() {
        val error = calculate()
        if (error == null) {
            if (lastAns != null) {
                previewString = formatAnswer(lastAns!!)
                lastAns = null
            }
            else {
                previewString=""
            }
        } else if (error == "Too big value") previewString = ""
    }

    fun addToken(isOperator: Boolean, value: String) {

        val token = Token(isOperator, value)


        //Determining the type of the input element:
        val input = if (!token.isOperator) {
            1
        } else {
            val operator = getOperator(token)
            if (!operator.isParenthesis) {
                if (operator.needNumBefore) {
                    2
                } else 3
            } else {
                if (operator.needNumAfter) 3
                else 4
            }
        }

        //Save answer if operator, else clear answer
        if (lastAns != null) {
            val tmpAns = lastAns
            clear()
            if (input == 2 || input == 3) {
                tokenList.add(Token(false,formatAnswer(tmpAns!!)))
                cursor[0]+=1
                cursor[1]+=tmpAns.toString().length-1
            }
        }

        //Determining the type of the last element:
        var curr: Int
        if (cursor[1] == 0) {
            curr = 0
            if (tokenList.isNotEmpty()) {
                val currElement = tokenList[cursor[0]-1]
                val operator = getOperator(currElement)
                curr = if (!operator.isParenthesis || operator.needNumAfter) {
                    if (operator.needNumAfter) {
                        2
                    } else 3
                } else if (operator.priority == 1) {
                    4
                } else 5
            }
        } else {
            curr = 1
        }

        //Adding a token depending on types
        when (input) {
            1 -> {
                when (curr) {
                    0-> {
                        tokenList.add(token)
                        cursor[1]++
                        cursor[0]++
                    }

                    1 -> {
                        val str = tokenList[cursor[0]-1].value
                        if (str.contains("E")) {
                            tokenList[cursor[0] - 1].value = token.value
                            cursor[1]=1
                        } else {
                            if (token.value != "." || "." !in str.substring(0, cursor[1])) {
                                tokenList[cursor[0] - 1].value = (str.substring(0, cursor[1]) + token.value + str.substring(cursor[1]))
                                cursor[1]++
                            }
                        }
                    }

                    else -> {
                        tokenList.add(cursor[0], token)
                        cursor[0]++
                        cursor[1]=1
                    }
                }
            }

            2 -> {
                when (curr) {
                    1, 3, 5 -> {
                        tokenList.add(cursor[0], token)
                        cursor[1] = 0
                        cursor[0]++
                    }

                    2 -> {
                        if (getOperator(tokenList[cursor[0]-1]).needNumBefore) {
                            tokenList[cursor[0]-1] = token
                            cursor[1] = 0
                        }
                    }
                }
            }

            3 -> {
                tokenList.add(cursor[0], token)
                if (getOperator(token).isParenthesis) {
                    openedParenthesis++
                }
                cursor[1] = 0
                cursor[0]++
            }

            4 -> {
                when (curr) {
                    0, 2, 4 -> {
                        tokenList.add(cursor[0], token)
                        openedParenthesis++
                        cursor[1] = 0
                        cursor[0]++
                    }

                    else -> {
                        if (openedParenthesis > 0) {
                            token.value = "1"
                            openedParenthesis--
                        } else {
                            openedParenthesis++
                        }
                        tokenList.add(cursor[0], token)

                        cursor[1] = 0
                        cursor[0]++
                    }
                }
            }
        }
        mainString = printExpression()
        val error = calculate()
        if (error == null) {
            if (lastAns != null) {
                previewString = formatAnswer(lastAns!!)
                lastAns = null
            }
            else {
                previewString=""
            }
        } else if (error == "Too big value") previewString = ""
    }

    fun removeToken() {

        if (lastAns != null) {
            clear()
            lastAns = null
        }

        var flag = false
        if (tokenList.isNotEmpty()) {
            if (cursor[1] == 0) {
                if (tokenList[cursor[0]-1].value == "1") {
                    openedParenthesis++
                }
                if (getOperator(tokenList[cursor[0]-1]).isParenthesis && (tokenList[cursor[0]-1].value == "0" || getOperator(tokenList[cursor[0]-1]).needNumAfter)) {
                    openedParenthesis--
                }
                tokenList.removeAt(cursor[0]-1)
                if (cursor[0] > 0) {
                    cursor[0]--
                    flag = true
                }
            } else {
                val numberStr = tokenList[cursor[0]-1].value
                if (numberStr.length == 1 || numberStr.contains("E")) {
                    tokenList.removeAt(cursor[0]-1)
                    cursor[1] = 0
                    if (cursor[0] > 0) {
                        cursor[0]--
                        flag = true
                    }
                } else {
                    tokenList[cursor[0]-1].value = numberStr.removeRange(cursor[1]-1, cursor[1])
                    cursor[1]--
                }
            }
        }
        if (flag && tokenList.isNotEmpty() && !tokenList[cursor[0]-1].isOperator) {
            cursor[1] = tokenList[cursor[0]-1].value.length
        }
        mainString = printExpression()
        if (calculate() == null) {
            if (lastAns != null) {
                previewString = formatAnswer(lastAns!!)
                lastAns = null
            }
            else {
                previewString=""
            }
        }
    }

    fun clear() {
        cursor = intArrayOf(0,0)
        openedParenthesis = 0
        tokenList.clear()
        mainString = ""
        previewString = ""
        lastAns=null
    }

    fun writeAnswer() {
        val error = calculate()
        if (error == null) {
            if (lastAns != null) mainString = formatAnswer(lastAns!!)
        } else {
            mainString = error
        }
        previewString = ""
    }

    private fun calculate(): String? {

        if (tokenList.size<2) {
            lastAns = null
            return null
        }

        val tmp:ArrayList<CalcToken?> = arrayListOf()
        for (i in tokenList) {
            tmp.add(CalcToken(i.isOperator, i.value.toDouble()))
        }

        for (i in 0..<openedParenthesis) {
            if (!(tmp.last()!!.isOperator && getOperator(tmp.last()!!).needNumAfter)) tmp.add(CalcToken(true, 1.0))
        }


        val priorityList = ArrayList<Pair<Int, Int>>()

        var tmpPriority = 0
        var index = 0

//        while (index < tmp.size) {
//            if (!tmp[index]!!.isOperator) {
//                val i = removeExtraBrackets(tmp, index)
//                while (i < index) {
//                    index--
//                }
//            }
//            index++
//        }

//        index = 0

        while (index < tmp.size) {
//            if (index+1 < tmp.size) {
//                if (!tmp[index]!!.isOperator || (!getOperator(tmp[index]!!).needNumAfter) || (getOperator(tmp[index]!!).isParenthesis && tmp[index]!!.value == 1.0)) {
//                    if (!tmp[index+1]!!.isOperator || (!getOperator(tmp[index+1]!!).needNumBefore && tmp[index+1]!!.value != 15.0) || (getOperator(tmp[index+1]!!).isParenthesis && tmp[index+1]!!.value == 0.0)) {
//                        tmp.add(index+1,CalcToken(true, 4.0))
//                    }
//                }
//            }

            if (tmp[index]!!.isOperator) {
                val operator = getOperator(tmp[index]!!)
                if (operator.isParenthesis && (operator.priority == 1 || operator.needNumAfter)) {
                    if (operator.needNumAfter) priorityList.add(Pair(index, getOperator(tmp[index]!!).priority + tmpPriority))
                    tmpPriority += parenthesisPriority
                } else if (operator.isParenthesis && operator.priority == -1) tmpPriority -= parenthesisPriority
                else {
                    priorityList.add(Pair(index, getOperator(tmp[index]!!).priority + tmpPriority))
                }
            } else {
                val i = removeExtraBrackets(tmp, index)
                while (i < index) {
                    index--
                    tmpPriority--
                }
            }
            index++
        }


        if (priorityList.size < 1) {
            lastAns = null
            return null
        }

        priorityList.sortByDescending { it.second }

        for (i in 0..<priorityList.size) {
            if (tmp[priorityList[i].first] != null && tmp[priorityList[i].first]!!.isOperator) {
                val error = operatorAction(tmp, priorityList[i].first)
                if (error != null) {
                    return error
                }
            }
        }

        var test = true
        lastAns = 1.0
        for (i in tmp) {
            if (i != null) {
                if (!i.isOperator) {
                    lastAns = lastAns!! * i.value
                    test = false
                }
                else {
                    lastAns = null
                    return "Unknown error"
                }
            }
        }
        if (test) {
            lastAns = null
            return "Unknown error"
        }
        if (lastAns == Double.POSITIVE_INFINITY || lastAns == Double.POSITIVE_INFINITY) {
            lastAns = null
            return "Too big value"
        }

        return null
    }

    private fun formatAnswer(value: Double, decimalPlaces: Int = 4, maxDigitsBeforeE: Int = 6, epsilon: Double = 0.0000000001): String {
        if (value.absoluteValue < epsilon) {
            return "0"
        }
        var bigDecimal = BigDecimal(value)
        bigDecimal = bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP)
        var result = bigDecimal.stripTrailingZeros().toPlainString()
        val integerPartLength = result.indexOf('.').let { if (it == -1) result.length else it }
        if (integerPartLength > maxDigitsBeforeE || (value < 1e-3 && value > 0) || (value > -1e-3 && value < 0)) {
            val decimalFormat = DecimalFormat("0.#####E0")
            result = decimalFormat.format(value)
        }

        return result
    }

    private fun printExpression():String {
        var str:String = ""
        for (i in tokenList) {
            if (i.isOperator) {
                str += getOperator(i).value
            } else {
                str += i.value
            }
        }
        return str
    }

    private fun removeExtraBrackets(list:ArrayList<CalcToken?>, i:Int, remove:Boolean=true):Int {

        var prev = getPrev(list,i)
        var next = getNext(list,i)

        if (prev!=-1 && next!=-1 && list[prev]!!.isOperator && list[next]!!.isOperator) {
            var prevToken = getOperator(list[prev]!!)
            var nextToken = getOperator(list[next]!!)

            var index:Int = i
            while (prev!=-1 && next!=-1 && list[prev]!!.isOperator && list[next]!!.isOperator && prevToken.isParenthesis && (list[prev]!!.value == 0.0 || (prevToken.needNumAfter && !prevToken.needNumBefore)) && nextToken.isParenthesis && list[next]!!.value == 1.0) {
                prevToken = getOperator(list[prev]!!)
                nextToken = getOperator(list[next]!!)
                if (remove) {
                    list.removeAt(next)
                    if (list[prev]!!.value == 0.0) list.removeAt(prev)
                    index--
                } else {
                    if (list[prev]!!.value == 0.0) list[prev] = null
                    list[next] = null
                }
                prev = getPrev(list, index)
                next = getNext(list, index)
            }
            return ++index
        }
        else return i
    }

    private fun getPrev(list: ArrayList<CalcToken?>, index: Int): Int {
        var prevIndex = index - 1
        if (prevIndex < 0) return -1
        while (list[prevIndex] == null) {
            prevIndex--
            if (prevIndex < 0) return -1
        }
        return prevIndex
    }

    private fun getNext(list: ArrayList<CalcToken?>, index: Int): Int {
        var nextIndex = index + 1
        if (nextIndex >= list.size) return -1
        while (list[nextIndex] == null) {
            nextIndex++
            if (nextIndex >= list.size) return -1
        }
        return nextIndex
    }

    private fun operatorAction(list: ArrayList<CalcToken?>, index: Int):String? {

        val prev = getPrev(list, index)
        val next = getNext(list, index)


        if (getOperator(list[index]!!).needNumAfter) {
            if (next == -1) {
                return "Format error"
            }
            if (list[next]!!.isOperator && getOperator(list[next]!!).needNumAfter) {
                val error = operatorAction(list,next)
                if (error != null) {
                    return error
                }
            }
        }
        if (getOperator(list[index]!!).needNumBefore) {
            if (prev == -1) {
                return "Format error"
            }
            if (list[prev]!!.isOperator && getOperator(list[prev]!!).needNumBefore) {
                val error = operatorAction(list,prev)
                if (error != null) {
                    return error
                }
            }
        }

        when (list[index]!!.value.toInt()) {
            2 -> { // +
                var collect = getPrev(list,prev)
                while (collect != -1 && list[collect] != null && !list[collect]!!.isOperator) {
                    list[prev]!!.value *= list[collect]!!.value
                    list[collect] = null
                    collect = getPrev(list,collect)
                }
                collect = getNext(list,next)
                while (collect != -1 && list[collect] != null && !list[collect]!!.isOperator) {
                    list[next]!!.value *= list[collect]!!.value
                    list[collect] = null
                    collect = getNext(list,collect)
                }
                list[prev]!!.value += list[next]!!.value
                list[index] = null
                list[next] = null
                removeExtraBrackets(list,prev,false)
            }
            3 -> { // -
                list[prev]!!.value -= list[next]!!.value
                list[index] = null
                list[next] = null
                removeExtraBrackets(list,prev,false)
            }
            4 -> { // ×
                list[prev]!!.value *= list[next]!!.value
                list[index] = null
                list[next] = null
                removeExtraBrackets(list,prev,false)
            }
            5 -> { // ÷
                if (!list[next]!!.isOperator && list[next]!!.value == 0.0) return "Can`t divide by 0"
                list[prev]!!.value /= list[next]!!.value
                list[index] = null
                list[next] = null
                removeExtraBrackets(list,prev,false)
            }
            6 -> { // ^
                list[prev]!!.value = list[prev]!!.value.pow(list[next]!!.value)
                list[index] = null
                list[next] = null
                removeExtraBrackets(list,prev,false)
            }
            7 -> { // √
                if (!list[next]!!.isOperator && list[next]!!.value < 0.0) return "Keep it real"
                list[index] = CalcToken(false,sqrt(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            8 -> { // %
                val doublePrev = getPrev(list,prev)
                if (doublePrev > 0 && list[doublePrev]!!.isOperator && (list[doublePrev]!!.value == 2.0 || list[doublePrev]!!.value == 3.0)) {
                    list[prev]!!.value *= list[getPrev(list, doublePrev)]!!.value / 100
                } else {
                    list[prev]!!.value /= 100
                }
                list[index] = null
                removeExtraBrackets(list,prev,false)
            }
            9 -> { // !
                list[prev] = CalcToken(false,factorial(list[prev]!!.value.toInt()))
                list[index] = null
                removeExtraBrackets(list,prev,false)
            }
            10 -> { // sin
                list[index] = CalcToken(false, sin(if (useDegrees) toRadians(list[next]!!.value) else list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            11 -> { // cos
                list[index] = CalcToken(false, cos(if (useDegrees) toRadians(list[next]!!.value) else list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            12 -> { // tan
                list[index] = CalcToken(false, tan(if (useDegrees) toRadians(list[next]!!.value) else list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            13 -> { // ln
                if (!list[next]!!.isOperator && list[next]!!.value < 0.0) return "Domain error"
                list[index] = CalcToken(false,ln(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            14 -> { // log
                if (!list[next]!!.isOperator && list[next]!!.value < 0.0) return "Domain error"
                list[index] = CalcToken(false,log10(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            15 -> { // unary minus

                var collect = getNext(list,next)
                while (collect != -1 && list[collect] != null && !list[collect]!!.isOperator) {
                    list[next]!!.value *= list[collect]!!.value
                    list[collect] = null
                    collect = getNext(list,collect)
                }
                if (prev != -1 && !list[prev]!!.isOperator) {
                    collect = getPrev(list,prev)
                    while (collect != -1 && list[collect] != null && !list[collect]!!.isOperator) {
                        list[prev]!!.value *= list[collect]!!.value
                        list[collect] = null
                        collect = getPrev(list,collect)
                    }
                    list[prev]!!.value -= list[next]!!.value
                    list[index] = null
                    list[next] = null
                    removeExtraBrackets(list,prev,false)
                } else {
                    list[index] = CalcToken(false, list[next]!!.value * -1)
                    list[next] = null
                    removeExtraBrackets(list, index, false)
                }
            }
            16 -> { // π
                list[index] = CalcToken(false, Math.PI)
                removeExtraBrackets(list, index, false)
            }
            17 -> { // e
                list[index] = CalcToken(false, Math.E)
                removeExtraBrackets(list, index, false)
            }
            18 -> { //²
                list[prev] = CalcToken(false, list[prev]!!.value*list[prev]!!.value)
                list[index] = null
                removeExtraBrackets(list,prev,false)
            }
            19 -> { // sin⁻¹
                if (!list[next]!!.isOperator && list[next]!!.value.absoluteValue > 1.0) return "Domain error"
                list[index] = CalcToken(false, if (useDegrees) toDegrees(asin(list[next]!!.value)) else asin(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            20 -> { // cos⁻¹
                if (!list[next]!!.isOperator && list[next]!!.value.absoluteValue > 1.0) return "Domain error"
                list[index] = CalcToken(false, if (useDegrees) toDegrees(acos(list[next]!!.value)) else acos(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            21 -> { // tan⁻¹
                list[index] = CalcToken(false, if (useDegrees) toDegrees(atan(list[next]!!.value)) else atan(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
            22 -> { //exp
                list[index] = CalcToken(false, Math.E.pow(list[next]!!.value))
                list[next] = null
                removeExtraBrackets(list,index,false)
            }
        }
        return null
    }

    private fun factorial(n: Int): Double {
        var result:Double = 1.0
        for (i in 2..n) {
            result *= i
        }
        return result
    }

}