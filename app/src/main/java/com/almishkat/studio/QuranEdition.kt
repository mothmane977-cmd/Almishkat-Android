package com.almishkat.studio

/**
 * A Quran project must bind to exactly one riwayah edition.
 * Never silently substitute or normalize text between editions.
 */
enum class QuranRiwayah(
    val arabicName: String,
    val reciterTradition: String,
    val editionKey: String
) {
    HAFS(
        arabicName = "حفص عن عاصم",
        reciterTradition = "حفص",
        editionKey = "kfqc-hafs"
    ),
    WARSH(
        arabicName = "ورش عن نافع",
        reciterTradition = "ورش",
        editionKey = "kfqc-warsh"
    )
}

data class QuranEditionMetadata(
    val riwayah: QuranRiwayah,
    val sourceName: String,
    val sourceVersion: String,
    val textChecksum: String,
    val fontFamily: String,
    val verified: Boolean
)

data class RiwayahProjectSettings(
    val riwayah: QuranRiwayah,
    val sourceEditionKey: String,
    val timingsKey: String
)

object QuranEditionPolicy {
    fun settingsFor(riwayah: QuranRiwayah) = RiwayahProjectSettings(
        riwayah = riwayah,
        sourceEditionKey = riwayah.editionKey,
        timingsKey = "timings-${riwayah.name.lowercase()}"
    )

    fun mayReuseTimings(
        source: RiwayahProjectSettings,
        target: RiwayahProjectSettings
    ): Boolean = source.riwayah == target.riwayah &&
        source.sourceEditionKey == target.sourceEditionKey
}
