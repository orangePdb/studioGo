package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Android Studio "Islands Dark" inspired core palette
val StudioBgDark = Color(0xFF13151B)
val StudioSurfaceDark = Color(0xFF1B1E26)
val StudioSurfaceVariantDark = Color(0xFF232733)
val StudioSurfaceElevatedDark = Color(0xFF2B303E)
val StudioBorderDark = Color(0xFF323848)
val StudioTextPrimaryDark = Color(0xFFE6EAF2)
val StudioTextSecondaryDark = Color(0xFF94A3B8)

// Android Studio Light palette
val StudioBgLight = Color(0xFFF5F7FB)
val StudioSurfaceLight = Color(0xFFFFFFFF)
val StudioSurfaceVariantLight = Color(0xFFEBF0F7)
val StudioSurfaceElevatedLight = Color(0xFFE2E8F0)
val StudioBorderLight = Color(0xFFCBD5E1)
val StudioTextPrimaryLight = Color(0xFF0F172A)
val StudioTextSecondaryLight = Color(0xFF475569)

// Brand & IDE Functional Accents
val AndroidEmerald = Color(0xFF3DDC84)
val AndroidEmeraldDark = Color(0xFF16A34A)
val StudioBlue = Color(0xFF4C8BF5)
val KotlinViolet = Color(0xFFA78BFA)
val BuildAmber = Color(0xFFF59E0B)
val ErrorCrimson = Color(0xFFF87171)
val XmlTeal = Color(0xFF2DD4BF)

// Syntax Highlighting Tokens (Dark Editor)
data class EditorSyntaxColors(
    val editorBg: Color,
    val gutterBg: Color,
    val currentLineBg: Color,
    val lineNumber: Color,
    val plainText: Color,
    val keyword: Color,
    val type: Color,
    val annotation: Color,
    val stringLiteral: Color,
    val numberLiteral: Color,
    val comment: Color,
    val functionCall: Color,
    val xmlTag: Color,
    val xmlAttribute: Color,
)

val DarkSyntaxColors = EditorSyntaxColors(
    editorBg = Color(0xFF13151B),
    gutterBg = Color(0xFF181B23),
    currentLineBg = Color(0xFF1E222D),
    lineNumber = Color(0xFF5A6478),
    plainText = Color(0xFFDFE5F0),
    keyword = Color(0xFFCF8E6D),
    type = Color(0xFF67D5B5),
    annotation = Color(0xFFF2C55C),
    stringLiteral = Color(0xFF6AAB73),
    numberLiteral = Color(0xFF2AACB8),
    comment = Color(0xFF6E7787),
    functionCall = Color(0xFF56A8F5),
    xmlTag = Color(0xFFD5B778),
    xmlAttribute = Color(0xFFBABABA),
)

val LightSyntaxColors = EditorSyntaxColors(
    editorBg = Color(0xFFFAFBFD),
    gutterBg = Color(0xFFF1F5F9),
    currentLineBg = Color(0xFFE8EEF7),
    lineNumber = Color(0xFF94A3B8),
    plainText = Color(0xFF1E293B),
    keyword = Color(0xFF0033B3),
    type = Color(0xFF067D17),
    annotation = Color(0xFF9E880D),
    stringLiteral = Color(0xFF067D17),
    numberLiteral = Color(0xFF1750EB),
    comment = Color(0xFF8C8C8C),
    functionCall = Color(0xFF00627A),
    xmlTag = Color(0xFF0033B3),
    xmlAttribute = Color(0xFF174AD4),
)
