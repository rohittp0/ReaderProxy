package com.rohitp.readerproxy.ui.theme

import androidx.compose.ui.graphics.Color

/*  ---  Light theme  ---  */
val Almond = Color(0xFFEAD4B4)   // surface / background
val BlackPrimary = Color(0xFF000000)   // primary / on-surface
val Charcoal = Color(0xFF45403A)   // secondary
val Taupe = Color(0xFF867E73)   // tertiary

/*  Optional dark-theme derivations  */
val AlmondDark = Color(0xFF2B2620)
val BlackLight = Color(0xFFEAEAEA)

/*  ---  Primary, secondary, tertiary colors  ---  */

val PrimaryLight = BlackPrimary
val OnPrimaryLight = Almond        // text/icon on primary buttons if needed

val BackgroundLight = Almond
val SurfaceLight = Almond
val OnBackgroundLight = BlackPrimary
val OnSurfaceLight = BlackPrimary

val SecondaryLight = Charcoal
val OnSecondaryLight = Almond        // contrast on secondary chip

val TertiaryLight = Taupe
val OnTertiaryLight = Almond

/*  ---  Dark theme  ---  */

val PrimaryDark = BlackLight
val OnPrimaryDark = AlmondDark

val BackgroundDark = AlmondDark
val SurfaceDark = AlmondDark
val OnBackgroundDark = BlackLight
val OnSurfaceDark = BlackLight

val SecondaryDark = Taupe
val OnSecondaryDark = AlmondDark

val TertiaryDark = Charcoal
val OnTertiaryDark = AlmondDark
