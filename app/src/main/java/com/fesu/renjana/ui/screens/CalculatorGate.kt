package com.fesu.renjana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorGate(onUnlocked: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("0") }
    var message by remember { mutableStateOf("CALCULATOR") }
    val keys = listOf("AC", "⌫", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−", "1", "2", "3", "+", "00", "0", ".", "=")
    val bg = Color(0xFF080B12)
    val keyBg = Color(0xFF171D2A)
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CALC SPACE", color = Color(0xFF8FAEFF), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Text(message, color = Color(0xFF77839A), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        Text(display, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        Text("Enter your access code", color = Color(0xFF64718A), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        Spacer(Modifier.height(10.dp))
        for (row in keys.chunked(4)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val special = key in listOf("÷", "×", "−", "+", "=")
                    Button(onClick = {
                        when (key) {
                            "AC" -> { input = ""; display = "0"; message = "CALCULATOR" }
                            "⌫" -> { input = input.dropLast(1); display = input.ifBlank { "0" } }
                            "=" -> {
                                if (input == "6767") { onUnlocked(); return@Button }
                                val result = calculate(input)
                                if (result != null) { display = result; input = result; message = "RESULT" }
                                else { display = "Error"; input = ""; message = "CALCULATOR" }
                            }
                            else -> { input += when(key) { "×" -> "*"; "÷" -> "/"; "−" -> "-"; else -> key }; display = input; message = "CALCULATOR" }
                        }
                    }, modifier = Modifier.weight(1f).height(66.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = if (special) Color(0xFF315FEA) else keyBg, contentColor = Color.White), contentPadding = PaddingValues(0.dp)) {
                        Text(key, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Private space • Calculator", color = Color(0xFF46536A), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun calculate(raw: String): String? {
    if (raw.isBlank() || !raw.matches(Regex("[0-9+*/%.()\\- ]+"))) return null
    return try {
        val tokens = Regex("(?<=[+*/%()\\-])|(?=[+*/%()\\-])").split(raw).filter { it.isNotBlank() }.toMutableList()
        var index = 0
        // Small recursive-descent parser with standard operator precedence.
        lateinit var parseExpr: () -> Double
        lateinit var parseTerm: () -> Double
        lateinit var parseFactor: () -> Double
        parseExpr = {
            var v = parseTerm()
            while (index < tokens.size && (tokens[index] == "+" || tokens[index] == "-")) { val op=tokens[index++]; val r=parseTerm(); v=if(op=="+") v+r else v-r }
            v
        }
        parseTerm = {
            var v = parseFactor()
            while (index < tokens.size && (tokens[index] == "*" || tokens[index] == "/" || tokens[index] == "%")) { val op=tokens[index++]; val r=parseFactor(); v=when(op){"*"->v*r;"/"->v/r;else->v%r} }
            v
        }
        parseFactor = {
            if (index < tokens.size && tokens[index] == "-") { index++; -parseFactor() }
            else if (index < tokens.size && tokens[index] == "(") { index++; val v=parseExpr(); require(tokens.getOrNull(index)==")"); index++; v }
            else tokens[index++].toDouble()
        }
        val value = parseExpr()
        require(index == tokens.size && value.isFinite())
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    } catch (_: Exception) { null }
}
