package jp.miya.simple_dentaku_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.util.*
import kotlin.text.iterator

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var formula = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.displayText)

        // AdMob 初期化
        MobileAds.initialize(this)

        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)


        // --- 数字ボタン ---
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn00
        )
        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                val input = (it as Button).text.toString()
                formula += input
                display.text = formula
            }
        }

        // --- 演算子ボタン ---
        val operatorButtons = listOf(R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide)
        for (id in operatorButtons) {
            findViewById<Button>(id).setOnClickListener {
                if (formula.isEmpty()) return@setOnClickListener
                val input = (it as Button).text.toString()
                // 直前が演算子なら置き換え
                if ("+-×÷".contains(formula.last())) {
                    formula = formula.dropLast(1)
                }
                formula += input
                display.text = formula
            }
        }

        // --- クリア ---
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            formula = ""
            display.text = ""
        }

        // --- パーセント ---
        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            if (formula.isEmpty()) return@setOnClickListener
            // 直前の数字だけを % に変換
            val match = Regex("(\\d+\\.?\\d*)\$").find(formula)
            if (match != null) {
                val number = match.value.toDouble() / 100
                formula = formula.dropLast(match.value.length) + number.toString()
                display.text = formula
            }
        }

        // --- イコール ---
        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            val result = evaluateExpression(formula)
            if (result.isNaN()) {
                display.text = "Error"
                formula = ""
            } else {
                display.text = formatResult(result)
                formula = result.toString()
            }
        }
    }

    // 四則演算計算
    private fun evaluateExpression(expr: String): Double {
        if (expr.isBlank()) return Double.NaN

        // 記号変換
        val tokens = tokenize(expr.replace("×", "*").replace("÷", "/"))
        val values = Stack<Double>()
        val ops = Stack<Char>()

        fun applyOp() {
            if (values.size < 2 || ops.isEmpty()) return
            val b = values.pop()
            val a = values.pop()
            when (ops.pop()) {
                '+' -> values.push(a + b)
                '-' -> values.push(a - b)
                '*' -> values.push(a * b)
                '/' -> values.push(a / b)
            }
        }

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> values.push(token.toDouble())
                token.length == 1 && "+-*/".contains(token[0]) -> {
                    val op = token[0]
                    while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(op)) {
                        applyOp()
                    }
                    ops.push(op)
                }
            }
        }
        while (ops.isNotEmpty()) applyOp()
        return if (values.isNotEmpty()) values.pop() else Double.NaN
    }

    private fun precedence(op: Char): Int {
        return when (op) {
            '+', '-' -> 1
            '*', '/' -> 2
            else -> 0
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var number = ""
        for (c in expr) {
            if (c.isDigit() || c == '.') {
                number += c
            } else if ("+-*/".contains(c)) {
                if (number.isNotEmpty()) {
                    tokens.add(number)
                    number = ""
                }
                tokens.add(c.toString())
            }
        }
        if (number.isNotEmpty()) tokens.add(number)
        return tokens
    }

    private fun formatResult(value: Double): String {
        return if (value % 1 == 0.0) value.toInt().toString() else value.toString()
    }
}
