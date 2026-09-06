package com.iykyk.facestory.util

import android.util.Log

object UniversalLogger {

    private const val TAG_PREFIX = "FaceStory"

    fun i(name: String, message: String) {
        val tag = formatTag(name)
        val formattedMessage = formatMessage(name, message)
        Log.i(tag, formattedMessage)
    }

    fun w(name: String, message: String, throwable: Throwable? = null) {
        val tag = formatTag(name)
        val formattedMessage = formatMessage(name, message)
        if (throwable != null) {
            Log.w(tag, formattedMessage, throwable)
        } else {
            Log.w(tag, formattedMessage)
        }
    }

    fun e(name: String, message: String, throwable: Throwable? = null) {
        val tag = formatTag(name)
        val formattedMessage = formatMessage(name, message)
        if (throwable != null) {
            Log.e(tag, formattedMessage, throwable)
        } else {
            Log.e(tag, formattedMessage)
        }
    }

    fun log(name: String, message: String) {
        i(name, message)
    }

    private fun formatTag(name: String): String {
        return "${TAG_PREFIX}_$name".take(23)
    }

    private fun formatMessage(name: String, message: String): String {
        return "[$name] $message"
    }
}
