package com.snaptube.clone.core.extractor

import android.net.Uri

/**
 * Best-effort URL classification used to route an incoming link or clipboard
 * entry to the right extractor before any network work is done.
 */
object UrlMatcher {

    private val youtubeHosts = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be",
        "music.youtube.com", "youtube-nocookie.com"
    )
    private val instagramHosts = setOf("instagram.com", "www.instagram.com", "instagr.am")
    private val tiktokHosts = setOf("tiktok.com", "www.tiktok.com", "vm.tiktok.com", "vt.tiktok.com")
    private val facebookHosts = setOf("facebook.com", "www.facebook.com", "m.facebook.com", "fb.watch", "fb.com")
    private val twitterHosts = setOf("twitter.com", "x.com", "www.twitter.com")
    private val vimeoHosts = setOf("vimeo.com", "www.vimeo.com", "player.vimeo.com")
    private val dailymotionHosts = setOf("dailymotion.com", "www.dailymotion.com", "dai.ly")
    private val pinterestHosts = setOf("pinterest.com", "www.pinterest.com", "pin.it")
    private val redditHosts = setOf("reddit.com", "www.reddit.com", "v.redd.it", "redd.it")
    private val twitchHosts = setOf("twitch.tv", "www.twitch.tv", "clips.twitch.tv")
    private val soundcloudHosts = setOf("soundcloud.com", "www.soundcloud.com", "m.soundcloud.com")
    private val vkHosts = setOf("vk.com", "www.vk.com", "vkvideo.ru")
    private val telegramHosts = setOf("t.me", "telegram.me")

    /** Any http(s) link that looks like a media page. */
    private val genericMediaExtension = setOf("mp4", "webm", "mkv", "mov", "m4v", "mp3", "m4a", "aac", "flac", "wav", "ogg")

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$").containsMatchIn(trimmed) -> "https://$trimmed"
            else -> return null
        }
        return runCatching { Uri.parse(withScheme).toString() }.getOrNull()
    }

    fun hostOf(url: String): String? =
        runCatching { Uri.parse(url).host?.lowercase() }.getOrNull()

    fun snapshot(url: String): SiteMatch {
        val host = hostOf(url) ?: return SiteMatch.UNKNOWN
        return when {
            matches(host, youtubeHosts) -> SiteMatch.YOUTUBE
            matches(host, instagramHosts) -> SiteMatch.INSTAGRAM
            matches(host, tiktokHosts) -> SiteMatch.TIKTOK
            matches(host, facebookHosts) -> SiteMatch.FACEBOOK
            matches(host, twitterHosts) -> SiteMatch.TWITTER
            matches(host, vimeoHosts) -> SiteMatch.VIMEO
            matches(host, dailymotionHosts) -> SiteMatch.DAILYMOTION
            matches(host, pinterestHosts) -> SiteMatch.PINTEREST
            matches(host, redditHosts) -> SiteMatch.REDDIT
            matches(host, twitchHosts) -> SiteMatch.TWITCH
            matches(host, soundcloudHosts) -> SiteMatch.SOUNDCLOUD
            matches(host, vkHosts) -> SiteMatch.VK
            matches(host, telegramHosts) -> SiteMatch.TELEGRAM
            else -> {
                val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
                if (ext in genericMediaExtension) SiteMatch.DIRECT_MEDIA else SiteMatch.GENERIC_WEB
            }
        }
    }

    private fun matches(host: String, set: Set<String>): Boolean =
        set.contains(host) || set.any { host.endsWith(".$it") }
}

enum class SiteMatch {
    YOUTUBE,
    INSTAGRAM,
    TIKTOK,
    FACEBOOK,
    TWITTER,
    VIMEO,
    DAILYMOTION,
    PINTEREST,
    REDDIT,
    TWITCH,
    SOUNDCLOUD,
    VK,
    TELEGRAM,
    DIRECT_MEDIA,
    GENERIC_WEB,
    UNKNOWN
}
