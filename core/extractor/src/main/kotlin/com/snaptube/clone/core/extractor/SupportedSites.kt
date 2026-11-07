package com.snaptube.clone.core.extractor

/**
 * Catalogue of the social sites and generic sources the app advertises support
 * for, used to build the "supported sites" screen and the search source chips.
 *
 * Keeping this list data-driven (instead of one class per site) matches how the
 * analysed app ships an `assets/full_sites.json` catalogue.
 */
data class SupportedSite(
    val title: String,
    val host: String,
    val match: SiteMatch,
    val category: Category,
    val iconUrl: String = ""
) {
    enum class Category { SOCIAL, VIDEO, MUSIC, OTHER }
}

object SupportedSites {

    val all: List<SupportedSite> = listOf(
        SupportedSite("YouTube", "youtube.com", SiteMatch.YOUTUBE, SupportedSite.Category.VIDEO),
        SupportedSite("YouTube Music", "music.youtube.com", SiteMatch.YOUTUBE, SupportedSite.Category.MUSIC),
        SupportedSite("Instagram", "instagram.com", SiteMatch.INSTAGRAM, SupportedSite.Category.SOCIAL),
        SupportedSite("TikTok", "tiktok.com", SiteMatch.TIKTOK, SupportedSite.Category.SOCIAL),
        SupportedSite("Facebook", "facebook.com", SiteMatch.FACEBOOK, SupportedSite.Category.SOCIAL),
        SupportedSite("X (Twitter)", "x.com", SiteMatch.TWITTER, SupportedSite.Category.SOCIAL),
        SupportedSite("Vimeo", "vimeo.com", SiteMatch.VIMEO, SupportedSite.Category.VIDEO),
        SupportedSite("Dailymotion", "dailymotion.com", SiteMatch.DAILYMOTION, SupportedSite.Category.VIDEO),
        SupportedSite("Pinterest", "pinterest.com", SiteMatch.PINTEREST, SupportedSite.Category.SOCIAL),
        SupportedSite("Reddit", "reddit.com", SiteMatch.REDDIT, SupportedSite.Category.SOCIAL),
        SupportedSite("Twitch", "twitch.tv", SiteMatch.TWITCH, SupportedSite.Category.VIDEO),
        SupportedSite("SoundCloud", "soundcloud.com", SiteMatch.SOUNDCLOUD, SupportedSite.Category.MUSIC),
        SupportedSite("VK", "vk.com", SiteMatch.VK, SupportedSite.Category.SOCIAL),
        SupportedSite("Telegram", "t.me", SiteMatch.TELEGRAM, SupportedSite.Category.SOCIAL)
    )

    /** Any other web page is attempted with the generic HTML extractor. */
    val supportsGenericWeb: Boolean = true
}
