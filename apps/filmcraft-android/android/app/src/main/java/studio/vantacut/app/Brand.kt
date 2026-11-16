package studio.vantacut.app

/**
 * VantaCut Studio's brand, in one place: the product's names and its identity pieces. Screens read
 * from here (or from [Vc], the palette) instead of hardcoding names or colours, so the product can
 * be renamed before release by changing this file and the resources it points at.
 */
object Brand {
    /** The product's name, shown on Home, About and share sheets. */
    const val APP_NAME: String = "VantaCut Studio"

    /** The short name (notifications, toasts). */
    const val SHORT_NAME: String = "VantaCut"

    /**
     * The name a new project gets before the user types one (used as an export file name too).
     */
    const val DEFAULT_PROJECT_NAME: String = "VantaCut project"

    /** The string before a video's name in share/open intents ("Open in VantaCut"). */
    const val OPEN_WITH: String = "Open in VantaCut"

    /**
     * The engine this app is built on, credited in About. VantaCut Studio is built on FilmCraft's
     * Rust engine (MIT OR Apache-2.0); its attribution must stay visible.
     */
    const val ENGINE_CREDIT: String = "Built on the FilmCraft Rust engine. MIT OR Apache-2.0."

    /** The repository this app's source lives in. */
    const val SOURCE_URL: String = "https://github.com/artcraft-android/filmcraft"
}
