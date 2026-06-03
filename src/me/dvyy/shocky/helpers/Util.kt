package me.dvyy.shocky

internal fun runCommand(args: List<String>) {
    ProcessBuilder(args).apply {
        redirectError(ProcessBuilder.Redirect.INHERIT)
    }.start().waitFor()
}
