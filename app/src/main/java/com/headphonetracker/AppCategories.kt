package com.headphonetracker

/**
 * Single source of truth for app categorization.
 * Used by StatsFragment (category breakdown) and HeadphoneTrackingService (known app matching).
 */
object AppCategories {

    enum class Category(val label: String) {
        MUSIC("Music"),
        VIDEO("Video"),
        PODCASTS("Podcasts"),
        SOCIAL("Social"),
        GAMING("Gaming"),
        CALLS("Calls"),
        BROWSER("Browser"),
        OTHER("Other")
    }

    /** Package prefix → category mapping. Uses startsWith matching. */
    private val prefixToCategory: List<Pair<String, Category>> = listOf(
        // ── Calls / VoIP ──
        "com.google.android.dialer" to Category.CALLS,
        "com.samsung.android.dialer" to Category.CALLS,
        "com.samsung.android.incallui" to Category.CALLS,
        "com.android.phone" to Category.CALLS,
        "com.android.dialer" to Category.CALLS,
        "com.android.incallui" to Category.CALLS,
        "com.whatsapp" to Category.CALLS,
        "org.telegram" to Category.CALLS,
        "com.discord" to Category.CALLS,
        "com.skype" to Category.CALLS,
        "us.zoom" to Category.CALLS,
        "com.google.android.apps.meetings" to Category.CALLS,
        "com.microsoft.teams" to Category.CALLS,
        "com.facebook.orca" to Category.CALLS,
        "com.viber" to Category.CALLS,
        "com.linecorp.LGSDK" to Category.CALLS,
        "jp.naver.line.android" to Category.CALLS,
        "com.google.android.apps.messaging" to Category.CALLS,

        // ── Music ──
        "com.spotify" to Category.MUSIC,
        "com.apple.android.music" to Category.MUSIC,
        "com.amazon.mp3" to Category.MUSIC,
        "com.google.android.apps.youtube.music" to Category.MUSIC,
        "com.soundcloud" to Category.MUSIC,
        "deezer.android" to Category.MUSIC,
        "com.pandora.android" to Category.MUSIC,
        "com.gaana" to Category.MUSIC,
        "com.jio.media" to Category.MUSIC,
        "com.hungama" to Category.MUSIC,
        "com.wynk" to Category.MUSIC,
        "com.tidal" to Category.MUSIC,
        "com.anghami" to Category.MUSIC,
        "com.audiomack" to Category.MUSIC,
        "com.shazam" to Category.MUSIC,

        // ── Video ──
        "com.google.android.youtube" to Category.VIDEO,
        "com.netflix" to Category.VIDEO,
        "com.disney" to Category.VIDEO,
        "com.hbo" to Category.VIDEO,
        "tv.twitch" to Category.VIDEO,
        "com.amazon.avod" to Category.VIDEO,
        "com.vlc" to Category.VIDEO,
        "com.mxtech.videoplayer" to Category.VIDEO,
        "org.videolan.vlc" to Category.VIDEO,
        "com.vanced" to Category.VIDEO,
        "app.revanced" to Category.VIDEO,

        // ── Podcasts ──
        "com.google.android.apps.podcasts" to Category.PODCASTS,
        "com.bambuna.podcastaddict" to Category.PODCASTS,
        "com.podcast" to Category.PODCASTS,
        "com.stitcher" to Category.PODCASTS,
        "com.pocketcasts" to Category.PODCASTS,
        "au.com.shiftyjelly.pocketcasts" to Category.PODCASTS,
        "fm.castbox" to Category.PODCASTS,
        "com.audible" to Category.PODCASTS,

        // ── Social ──
        "com.instagram.android" to Category.SOCIAL,
        "com.zhiliaoapp.musically" to Category.SOCIAL,  // TikTok
        "com.snapchat.android" to Category.SOCIAL,
        "com.facebook.katana" to Category.SOCIAL,
        "com.twitter.android" to Category.SOCIAL,
        "com.reddit" to Category.SOCIAL,

        // ── Gaming ──
        "com.supercell" to Category.GAMING,
        "com.kiloo" to Category.GAMING,
        "com.mojang" to Category.GAMING,
        "com.innersloth" to Category.GAMING,
        "com.roblox" to Category.GAMING,

        // ── Browsers ──
        "com.android.chrome" to Category.BROWSER,
        "org.mozilla.firefox" to Category.BROWSER,
        "com.brave.browser" to Category.BROWSER,
        "com.opera" to Category.BROWSER,
        "com.samsung.android.app.sbrowser" to Category.BROWSER,
        "com.microsoft.emmx" to Category.BROWSER,
    )

    /** Classify a package name into a category. */
    fun categorize(packageName: String): Category {
        for ((prefix, category) in prefixToCategory) {
            if (packageName.startsWith(prefix)) return category
        }
        return Category.OTHER
    }

    /**
     * All known media/audio package prefixes (for audio attribution).
     * Derived from the category map — everything that isn't OTHER.
     */
    val knownAudioAppPrefixes: List<String> by lazy {
        prefixToCategory.map { it.first }
    }

    /**
     * Prefixes used for foreground-app fallback attribution (Strategy 2).
     * Excludes CALLS apps — those should only be attributed when USAGE_VOICE_COMMUNICATION
     * is detected directly via AudioPlaybackConfiguration (Strategy 1), not by checking
     * which communication app was last in the foreground.
     */
    val mediaAttributionPrefixes: List<String> by lazy {
        prefixToCategory
            .filter { (_, category) ->
                category != Category.CALLS && category != Category.SOCIAL
            }
            .map { it.first }
    }

    /**
     * System/utility packages to always skip during attribution.
     * These never produce user-initiated audio.
     */
    val skipPackages: Set<String> = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.packageinstaller",
        "com.android.settings",
        "com.android.vending",
        "com.google.android.permissioncontroller",
        "com.google.android.googlequicksearchbox",
        "com.google.android.gms",
        "com.android.providers.media",
        "com.google.android.gm",
        "com.google.android.contacts",
        "com.google.android.calendar",
        "com.google.android.apps.maps",
        "com.google.android.apps.photos",
        "com.google.android.documentsui",
        "com.google.android.apps.docs",
        "com.android.camera",
        "com.google.android.GoogleCamera",
        "com.android.calculator2",
        "com.android.deskclock",
        "com.android.providers.downloads",
    )
}
