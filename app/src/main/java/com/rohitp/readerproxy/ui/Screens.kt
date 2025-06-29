package com.rohitp.readerproxy.ui

sealed class Screens(val route: String) {
    object Home : Screens("home")
    object Certificate : Screens("certificate")
}