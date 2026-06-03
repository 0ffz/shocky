package me.dvyy.shocky

fun shocky(
    init: Site.() -> Unit,
): Shocky = Shocky(init)