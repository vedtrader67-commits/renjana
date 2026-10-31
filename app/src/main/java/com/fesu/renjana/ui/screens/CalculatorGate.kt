package com.fesu.renjana.ui.screens

import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorGate(onUnlocked: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("0") }
    val keys = listOf(
        listOf("AC", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("⌫", "0", ".", "=")
    )
    val ink = Color(0xFF493D50)
    val pink = Color(0xFFFF8FBA)
    val purple = Color(0xFFB69AE8)

    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF8FC)).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
            Box(Modifier.size(9.dp).background(pink.copy(alpha = 0.7f), CircleShape))
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(6.dp).background(purple.copy(alpha = 0.7f), CircleShape))
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(4.dp).background(Color(0xFFFFC96B), CircleShape))
        }
        Spacer(Modifier.weight(1f))
        Text(
            input.replace("*", "×").replace("/", "÷").replace("-", "−"),
            Modifier.fillMaxWidth(), color = ink.copy(alpha = 0.48f),
            fontSize = 19.sp, textAlign = TextAlign.End, maxLines = 1
        )
        Text(display, Modifier.fillMaxWidth(), color = ink, fontSize = 52.sp,
            fontWeight = FontWeight.Light, textAlign = TextAlign.End, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                row.forEach { key ->
                    val operator = key in listOf("÷", "×", "−", "+", "=")
                    val utility = key in listOf("AC", "±", "%", "⌫")
                    val keyColor = when {
                        key == "=" -> pink
                        operator -> purple.copy(alpha = 0.30f)
                        utility -> Color(0xFFF0E8F5)
                        else -> Color.White
                    }
                    CalculatorKey(
                        label = key, color = keyColor,
                        textColor = if (key == "=") Color.White else ink,
                        modifier = Modifier.weight(1f)
                    ) {
                        when (key) {
                            "AC" -> { input = ""; display = "0" }
                            "⌫" -> { input = input.dropLast(1); display = input.ifBlank { "0" } }
                            "±" -> {
                                input = if (input.startsWith("-")) input.drop(1) else if (input.isNotEmpty()) "-$input" else "-"
                                display = input
                            }
                            "=" -> {
                                if (input == "6767") onUnlocked()
                                else {
                                    val result = calculate(input)
                                    if (result != null) { display = result; input = result }
                                    else if (input.isNotBlank()) { display = "Error"; input = "" }
                                }
                            }
                            else -> {
                                input += when (key) { "×" -> "*"; "÷" -> "/"; "−" -> "-"; else -> key }
                                display = input.replace("*", "×").replace("/", "÷").replace("-", "−")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CalculatorKey(
    label: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 650f),
        label = "keyPressScale"
    )
    Box(
        modifier.height(68.dp).scale(scale).background(color, RoundedCornerShape(23.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 23.sp, fontWeight = FontWeight.Medium)
    }
}

private fun calculate(raw: String): String? {
    if (raw.isBlank() || !raw.matches(Regex("[0-9+*/%.()\\- ]+"))) return null
    return try {
        val tokens = Regex("(?<=[+*/%()\\-])|(?=[+*/%()\\-])")
            .split(raw).filter { it.isNotBlank() }
        var index = 0
        lateinit var expression: () -> Double
        lateinit var term: () -> Double
        lateinit var factor: () -> Double
        expression = {
            var value = term()
            while (index < tokens.size && (tokens[index] == "+" || tokens[index] == "-")) {
                val op = tokens[index++]
                val rhs = term()
                value = if (op == "+") value + rhs else value - rhs
            }
            value
        }
        term = {
            var value = factor()
            while (index < tokens.size && tokens[index] in listOf("*", "/", "%")) {
                val op = tokens[index++]
                val rhs = factor()
                value = when (op) { "*" -> value * rhs; "/" -> value / rhs; else -> value % rhs }
            }
            value
        }
        factor = {
            when {
                index >= tokens.size -> throw IllegalArgumentException("Missing number")
                tokens[index] == "-" -> { index++; -factor() }
                tokens[index] == "(" -> {
                    index++
                    val value = expression()
                    require(tokens.getOrNull(index) == ")")
                    index++
                    value
                }
                else -> tokens[index++].toDouble()
            }
        }
        val value = expression()
        require(index == tokens.size && value.isFinite())
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    } catch (_: Exception) { null }
}
