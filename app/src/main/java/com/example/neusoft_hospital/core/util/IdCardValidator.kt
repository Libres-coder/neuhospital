package com.example.neusoft_hospital.core.util

object IdCardValidator {
    private val WEIGHT = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    private val CHECK_CODES = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')

    fun isValid(idCard: String): Boolean {
        val id = idCard.uppercase()
        if (!Regex("^[0-9]{17}[0-9X]$").matches(id)) return false

        val sum = id.substring(0, 17).mapIndexed { i, c -> (c - '0') * WEIGHT[i] }.sum()
        val checkCode = CHECK_CODES[sum % 11]
        return id[17] == checkCode
    }

    fun extractBirthDate(idCard: String): String? {
        if (idCard.length != 18) return null
        return try {
            "${idCard.substring(6, 10)}-${idCard.substring(10, 12)}-${idCard.substring(12, 14)}"
        } catch (e: Exception) {
            null
        }
    }

    fun extractGender(idCard: String): Int? {
        if (idCard.length != 18) return null
        return try {
            (idCard[16] - '0') % 2
        } catch (e: Exception) {
            null
        }
    }
}
