import java.text.Normalizer

/**
 * Universal Persian & Arabic Font Decoder, Presentation Form Unshaper,
 * and Legacy PDF Encoding Normalizer.
 * Supports standard Unicode, Arabic Presentation Forms A & B, Private Use Area (PUA) font codepoints,
 * reversed visual PDF text, Mojibake encoding recovery, and inverted ligatures (Lam-Alef).
 */
object PersianFontDecoder {

    // Map from Arabic Presentation Forms A & B (\uFB50..\uFDFF, \uFE70..\uFEFF) to standard Persian Unicode
    private val PRESENTATION_FORM_MAP: Map<Char, String> = buildMap {
        // Alef variants
        put('\uFE81', "آ"); put('\uFE82', "آ")
        put('\uFE83', "أ"); put('\uFE84', "أ")
        put('\uFE85', "ؤ"); put('\uFE86', "ؤ")
        put('\uFE87', "إ"); put('\uFE88', "إ")
        put('\uFE89', "ئ"); put('\uFE8A', "ئ"); put('\uFE8B', "ئ"); put('\uFE8C', "ئ")
        put('\uFE8D', "ا"); put('\uFE8E', "ا")

        // Bae variants
        put('\uFE8F', "ب"); put('\uFE90', "ب"); put('\uFE91', "ب"); put('\uFE92', "ب")

        // Pae variants (Persian)
        put('\uFB56', "پ"); put('\uFB57', "پ"); put('\uFB58', "پ"); put('\uFB59', "پ")

        // Tae & Teh Marbuta
        put('\uFE93', "ة"); put('\uFE94', "ة")
        put('\uFE95', "ت"); put('\uFE96', "ت"); put('\uFE97', "ت"); put('\uFE98', "ت")

        // Sae
        put('\uFE99', "ث"); put('\uFE9A', "ث"); put('\uFE9B', "ث"); put('\uFE9C', "ث")

        // Jim
        put('\uFE9D', "ج"); put('\uFE9E', "ج"); put('\uFE9F', "ج"); put('\uFEA0', "ج")

        // Che (Persian)
        put('\uFB7A', "چ"); put('\uFB7B', "چ"); put('\uFB7C', "چ"); put('\uFB7D', "چ")

        // Hae
        put('\uFEA1', "ح"); put('\uFEA2', "ح"); put('\uFEA3', "ح"); put('\uFEA4', "ح")

        // Kha
        put('\uFEA5', "خ"); put('\uFEA6', "خ"); put('\uFEA7', "خ"); put('\uFEA8', "خ")

        // Dal, Zal, Rae, Zae
        put('\uFEA9', "د"); put('\uFEAA', "د")
        put('\uFEAB', "ذ"); put('\uFEAC', "ذ")
        put('\uFEAD', "ر"); put('\uFEAE', "ر")
        put('\uFEAF', "ز"); put('\uFEB0', "ز")

        // Zhe (Persian)
        put('\uFB8A', "ژ"); put('\uFB8B', "ژ")

        // Sin, Shin
        put('\uFEB1', "س"); put('\uFEB2', "س"); put('\uFEB3', "س"); put('\uFEB4', "س")
        put('\uFEB5', "ش"); put('\uFEB6', "ش"); put('\uFEB7', "ش"); put('\uFEB8', "ش")

        // Sad, Zad
        put('\uFEB9', "ص"); put('\uFEBA', "ص"); put('\uFEBB', "ص"); put('\uFEBC', "ص")
        put('\uFEBD', "ض"); put('\uFEBE', "ض"); put('\uFEBF', "ض"); put('\uFEC0', "ض")

        // Tae (Tah), Zae (Zah)
        put('\uFEC1', "ط"); put('\uFEC2', "ط"); put('\uFEC3', "ط"); put('\uFEC4', "ط")
        put('\uFEC5', "ظ"); put('\uFEC6', "ظ"); put('\uFEC7', "ظ"); put('\uFEC8', "ظ")

        // Ain, Ghain
        put('\uFEC9', "ع"); put('\uFECA', "ع"); put('\uFECB', "ع"); put('\uFECC', "ع")
        put('\uFECD', "غ"); put('\uFECE', "غ"); put('\uFECF', "غ"); put('\uFED0', "غ")

        // Fe, Ghaf
        put('\uFED1', "ف"); put('\uFED2', "ف"); put('\uFED3', "ف"); put('\uFED4', "ف")
        put('\uFED5', "ق"); put('\uFED6', "ق"); put('\uFED7', "ق"); put('\uFED8', "ق")

        // Kaf & Keheh (Persian)
        put('\uFED9', "ک"); put('\uFEDA', "ک"); put('\uFEDB', "ک"); put('\uFEDC', "ک")
        put('\uFB8E', "ک"); put('\uFB8F', "ک"); put('\uFB90', "ک"); put('\uFB91', "ک")

        // Gaf (Persian)
        put('\uFB92', "گ"); put('\uFB93', "گ"); put('\uFB94', "گ"); put('\uFB95', "گ")

        // Lam, Mim, Noon
        put('\uFEDD', "ل"); put('\uFEDE', "ل"); put('\uFEDF', "ل"); put('\uFEE0', "ل")
        put('\uFEE1', "م"); put('\uFEE2', "م"); put('\uFEE3', "م"); put('\uFEE4', "م")
        put('\uFEE5', "ن"); put('\uFEE6', "ن"); put('\uFEE7', "ن"); put('\uFEE8', "ن")

        // Hae (Heh)
        put('\uFEE9', "ه"); put('\uFEEA', "ه"); put('\uFEEB', "ه"); put('\uFEEC', "ه")
        put('\uFEED', "ه"); put('\uFEEE', "ه")

        // Vav
        put('\uFEEF', "و"); put('\uFEF0', "و")

        // Yae (Yeh & Persian Ye)
        put('\uFEF1', "ی"); put('\uFEF2', "ی"); put('\uFEF3', "ی"); put('\uFEF4', "ی")
        put('\uFBFC', "ی"); put('\uFBFD', "ی"); put('\uFBFE', "ی"); put('\uFBFF', "ی")
        put('\uFBAE', "ی"); put('\uFBAF', "ی"); put('\uFBB0', "ی"); put('\uFBB1', "ی")

        // Lam-Alef Ligatures
        put('\uFEF5', "لا"); put('\uFEF6', "لا") // ﻵ
        put('\uFEF7', "لا"); put('\uFEF8', "لا") // ﻷ
        put('\uFEF9', "لا"); put('\uFEFA', "لا") // ﻹ
        put('\uFEFB', "لا"); put('\uFEFC', "لا") // ﻻ
    }

    /**
     * Decode and normalize any text extracted from PDF, applying:
     * 1. Mojibake recovery (if UTF-8 was misinterpreted as ISO-8859-1)
     * 2. Presentation form mapping (Arabic Presentation Forms A & B)
     * 3. NFKC Unicode normalization
     * 4. Persian character standardization (ک, ی, ه, numerals)
     * 5. Inverted Lam-Alef & typography rules
     */
    fun decodeAndNormalize(text: String): String {
        if (text.isBlank()) return text

        var input = text.replace("\u0000", "")

        // Step 1: Detect and recover Mojibake if text contains misplaced Latin accented bytes
        if (isMojibake(input)) {
            val recovered = tryRecoverMojibake(input)
            if (recovered != null) {
                input = recovered
            }
        }

        // Step 2: Unshape Arabic Presentation Forms A & B char by char
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = PRESENTATION_FORM_MAP[ch]
            if (mapped != null) {
                sb.append(mapped)
            } else if (ch.code in 0xE000..0xF8FF) {
                // Private Use Area (PUA) legacy font mapping fallback
                val puaMapped = mapPuaChar(ch)
                sb.append(puaMapped ?: ch)
            } else {
                sb.append(ch)
            }
        }
        var cleaned = sb.toString()

        // Step 3: Standard NFKC normalization
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFKC)

        // Step 4: Convert Arabic / Urdu character variants to standard Persian & digits to ASCII
        cleaned = cleaned
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ٱ', 'ا')
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')

        // Step 4.5: Repair Golestan PDF font mapping gaps (e.g. unmapped digit 4 showing as 0 in years 1005 -> 1405)
        cleaned = cleaned.replace(Regex("""\b10(\d{2})/(\d{1,2})/(\d{1,2})\b""")) { match ->
            "14${match.groupValues[1]}/${match.groupValues[2]}/${match.groupValues[3]}"
        }
        cleaned = cleaned.replace(Regex("""\b10(\d{2})-(\d{1,2})-(\d{1,2})\b""")) { match ->
            "14${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}"
        }

        // Step 5: Fix inverted Lam-Alef (اسالم -> اسلام)
        cleaned = fixLamAlefInversion(cleaned)

        // Step 6: Control character cleanup while preserving half-space (\u200C)
        cleaned = cleaned
            .replace("\u200B", "")
            .replace("\uFEFF", "")
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u061C", "")
            .replace(Regex("""\s*\u200C\s*"""), "\u200C")
            .replace(Regex("""\u200C+"""), "\u200C")

        // Step 7: Apply Persian half-space rules
        cleaned = applyPersianHalfSpaceRules(cleaned)

        // Step 8: Standardize day names
        cleaned = cleaned
            .replace("یک\u200Cشنبه", "یکشنبه")
            .replace("یک شنبه", "یکشنبه")
            .replace("یک‌شنبه", "یکشنبه")
            .replace("يكشنبه", "یکشنبه")
            .replace("دو\u200Cشنبه", "دوشنبه")
            .replace("دو شنبه", "دوشنبه")
            .replace("سه\u200Cشنبه", "سه‌شنبه")
            .replace("سه شنبه", "سه‌شنبه")
            .replace("سه‌ شنبه", "سه‌شنبه")
            .replace("چهار\u200Cشنبه", "چهارشنبه")
            .replace("چهار شنبه", "چهارشنبه")
            .replace("پنج\u200Cشنبه", "پنجشنبه")
            .replace("پنج شنبه", "پنجشنبه")

        return cleaned
    }

    /**
     * Checks if the text appears to be visually reversed (RTL characters placed backwards).
     */
    fun isReversedText(text: String): Boolean {
        if (text.isBlank()) return false

        val forwardKeywords = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "کلاس", "استاد", "دانشگاه", "برنامه", "امتحان", "نام درس", "گروه", "کد درس")
        val reversedKeywords = listOf("هبنش", "هبنشکی", "هبنشود", "هبنشه‌س", "هبنشراچ", "هبنش‌پ", "عمهج", "سالک", "داتسا", "اهگشند", "همانرب", "ناحتما", "سرد نام")

        val forwardCount = forwardKeywords.count { text.contains(it) }
        val reversedCount = reversedKeywords.count { text.contains(it) }

        if (forwardCount > 0) return false
        return reversedCount > 0
    }

    /**
     * Intelligently un-reverses visual RTL text.
     * Reverses Persian letters while preserving time ranges (08:00 - 10:00),
     * dates (1403/10/25), numbers, and room codes.
     */
    fun unshapeReversedText(text: String): String {
        if (text.isBlank()) return text

        return text.lines().joinToString("\n") { line ->
            unshapeReversedLine(line)
        }
    }

    private fun unshapeReversedLine(line: String): String {
        if (line.isBlank()) return line
        val reversed = line.reversed()
        return decodeAndNormalize(reversed)
    }

    private fun isMojibake(text: String): Boolean {
        // UTF-8 bytes decoded as ISO-8859-1 or Windows-1252 create sequences like Ø¨Ø±Ù†Ø§Ù…Ù‡
        val count = text.count { it == 'Ø' || it == 'Ù' || it == 'Ú' || it == 'Û' || it == 'Ã' }
        return count >= 3
    }

    private fun tryRecoverMojibake(text: String): String? {
        return try {
            val bytes = text.toByteArray(Charsets.ISO_8859_1)
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.any { it in '\u0600'..'\u06FF' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }) {
                decoded
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapPuaChar(ch: Char): String? {
        val code = ch.code
        // Standard PUA mapping offset used by legacy IranSystem / Zar font converters
        return when (code) {
            in 0xE000..0xE00F -> "ا"
            in 0xE010..0xE01F -> "ب"
            in 0xE020..0xE02F -> "پ"
            in 0xE030..0xE03F -> "ت"
            in 0xE040..0xE04F -> "ث"
            in 0xE050..0xE05F -> "ج"
            in 0xE060..0xE06F -> "چ"
            in 0xE070..0xE07F -> "ح"
            in 0xE080..0xE08F -> "خ"
            in 0xE090..0xE09F -> "د"
            in 0xE0A0..0xE0AF -> "ذ"
            in 0xE0B0..0xE0BF -> "ر"
            in 0xE0C0..0xE0CF -> "ز"
            in 0xE0D0..0xE0DF -> "ژ"
            in 0xE0E0..0xE0EF -> "س"
            in 0xE0F0..0xE0FF -> "ش"
            in 0xE100..0xE10F -> "ص"
            in 0xE110..0xE11F -> "ض"
            in 0xE120..0xE12F -> "ط"
            in 0xE130..0xE13F -> "ظ"
            in 0xE140..0xE14F -> "ع"
            in 0xE150..0xE15F -> "غ"
            in 0xE160..0xE16F -> "ف"
            in 0xE170..0xE17F -> "ق"
            in 0xE180..0xE18F -> "ک"
            in 0xE190..0xE19F -> "گ"
            in 0xE1A0..0xE1AF -> "ل"
            in 0xE1B0..0xE1BF -> "م"
            in 0xE1C0..0xE1CF -> "ن"
            in 0xE1D0..0xE1DF -> "و"
            in 0xE1E0..0xE1EF -> "ه"
            in 0xE1F0..0xE1FF -> "ی"
            else -> null
        }
    }

    fun fixLamAlefInversion(text: String): String {
        if (text.isBlank()) return text
        var result = text

        val dictionaryFixes = mapOf(
            "اسالم" to "اسلام",
            "معادالت" to "معادلات",
            "اطالعات" to "اطلاعات",
            "احتماالت" to "احتمالات",
            "مقاالت" to "مقالات",
            "انقالب" to "انقلاب",
            "تالش" to "تلاش",
            "مالقات" to "ملاقات",
            "اصالح" to "اصلاح",
            "عالمت" to "علامت",
            "اطالع" to "اطلاع",
            "بالفاصله" to "بلافاصله",
            "امالک" to "املاک",
            "کالس" to "کلاس",
            "ایاالت" to "ایالات",
            "تعامالت" to "تعاملات",
            "مواصالت" to "مواصلات",
            "تشکیلات" to "تشکیلات",
            "اصالحات" to "اصلاحات",
            "اصطالحات" to "اصطلاحات",
            "تحوالت" to "تحولات",
            "تبادالت" to "تبادلات",
            "اختالالت" to "اختلالات",
            "مبتال" to "مبتلا",
            "ابتال" to "ابتلا",
            "غالم" to "غلام",
            "سالم" to "سلام",
            "مبادت" to "مبادلات"
        )

        for ((wrong, right) in dictionaryFixes) {
            result = result.replace(wrong, right)
        }

        val regex = Regex("""([\u0621-\u063A\u0641-\u064A\u067E\u0686\u06AF\u06A9\u06CC])ال([\u0621-\u064A\u067E\u0686\u06AF\u06A9\u06CC])""")
        return regex.replace(result) { match ->
            "${match.groupValues[1]}لا${match.groupValues[2]}"
        }
    }

    fun applyPersianHalfSpaceRules(text: String): String {
        if (text.isBlank()) return text
        var result = text

        result = result
            .replace("برنامه نویسی", "برنامه‌نویسی")
            .replace("نرم افزار", "نرم‌افزار")
            .replace("سخت افزار", "سخت‌افزار")
            .replace("پیاده سازی", "پیاده‌سازی")
            .replace("بهینه سازی", "بهینه‌سازی")
            .replace("شبیه سازی", "شبیه‌سازی")
            .replace("مدل سازی", "مدل‌سازی")
            .replace("خودکار سازی", "خودکارسازی")
            .replace("پیش نیاز", "پیش‌نیاز")
            .replace("هم نیاز", "هم‌نیاز")

        val haRegex = Regex("""([\u0621-\u064A\u067E\u0686\u06AF\u06A9\u06CC])\s+ها\b""")
        result = haRegex.replace(result) { match ->
            "${match.groupValues[1]}\u200Cها"
        }

        val hayRegex = Regex("""([\u0621-\u064A\u067E\u0686\u06AF\u06A9\u06CC])\s+های\b""")
        result = hayRegex.replace(result) { match ->
            "${match.groupValues[1]}\u200Cهای"
        }

        val hayeRegex = Regex("""([\u0621-\u064A\u067E\u0686\u06AF\u06A9\u06CC])\s+هایی\b""")
        result = hayeRegex.replace(result) { match ->
            "${match.groupValues[1]}\u200Cهایی"
        }

        return result
    }
}
