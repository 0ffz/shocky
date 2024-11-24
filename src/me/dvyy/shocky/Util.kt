package me.dvyy.shocky

internal fun runCommand(vararg args: String) {
    ProcessBuilder(*args).apply {
//        inheritIO()
    }.start().waitFor()
}
