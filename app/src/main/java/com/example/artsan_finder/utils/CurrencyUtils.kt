package com.example.artsan_finder.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    /**
     * Formats a double value into a professional Tanzanian Shilling string.
     * Example: 50000.0 -> "Tsh. 50,000"
     */
    fun formatTsh(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US) // Use US locale for comma separators
        return "Tsh. ${formatter.format(amount.toLong())}"
    }
}
