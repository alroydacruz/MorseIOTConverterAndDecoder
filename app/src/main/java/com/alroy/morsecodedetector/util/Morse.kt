package com.alroy.morsecodedetector.util

class Morse {

//    fun main(args: Array<String>) {
//        textToMorse("Hello")
//    }
//
//    fun textToMorse(s: String) : String {
//        val morseArr = s.map{value -> convertCharToMorse(value)}
//        println(morseArr.joinToString(" "))
//
//        return s
//    }

    fun convertCharToMorse(code: String): String = when (code) {
        ".-" -> "a"
        "-..." -> "b"
        "-.-." -> "c"
        "-.." -> "d"
        "." -> "e"
        "..-." -> "f"
        "--." -> "g"
        "...." -> "h"
        ".." -> "i"
        ".---" -> "j"
        "-.-" -> "k"
        ".-.." -> "l"
        "--" -> "m"
        "-." -> "n"
        "---" -> "o"
        ".--." -> "p"
        "--.-" -> "q"
        ".-." -> "r"
        "..." -> "s"
        "-" -> "t"
        "..-" -> "u"
        "...-" -> "v"
        ".--" -> "w"
        "-..-" -> "x"
        "-.--" -> "y"
        "--.." -> "z"
        ".----" -> "1"
        "..---" -> "2"
        "...--" -> "3"
        "....-" -> "4"
        "....." -> "5"
        "-...." -> "6"
        "--..." -> "7"
        "---.." -> "8"
        "----." -> "9"
        "-----" -> "0"
        "--..--" -> ","
        ".-.-.-" -> "."
        "-...-" -> "="
        "..--.." -> "?"
        else -> ""
    }

    val listOfMorseCharsSupported = listOf(
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i",
        "j",
        "k",
        "l",
        "m",
        "n",
        "o",
        "p",
        "q",
        "r",
        "s",
        "t",
        "u",
        "v",
        "w",
        "x",
        "y",
        "z",
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "0",
        ",",
        ".",
        "?",
        "=",
        " "
    )

    fun checkValidString(message: String): ArrayList<String> {
        var letterAllowed: Boolean
        val unsupportedLetters = arrayListOf<String>()
        message.forEach { letter ->
            letter.toString()
            letterAllowed = Morse().listOfMorseCharsSupported.contains(letter.toString())
            if (!letterAllowed) {
                unsupportedLetters.add(letter.toString())
            }
        }
        return unsupportedLetters
    }

}