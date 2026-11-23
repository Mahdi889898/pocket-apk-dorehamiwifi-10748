package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class GameCategory(val titleFa: String) {
    ALL("همه ۱۲ بازی"),
    CLASSIC("تخته‌ای و کلاسیک"),
    STRATEGY("فکری و استراتژیک"),
    PARTY("هیجانی و دورهمی")
}

enum class GameType(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val rulesFa: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val category: GameCategory,
    val accentStart: Color,
    val accentEnd: Color,
    val icon: ImageVector,
    @DrawableRes val bannerRes: Int
) {
    LUDO(
        id = "ludo",
        titleFa = "منچ سلطنتی",
        subtitleFa = "مسابقه کلاسیک ۴ مهره تا خانه وسط",
        rulesFa = "با آوردن ۶ مهره را وارد زمین کنید، یک بار دیگر تاس بریزید و مهره حریفان را بزنید!",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.CLASSIC,
        accentStart = Color(0xFFF5B841),
        accentEnd = Color(0xFFEA580C),
        icon = Icons.Rounded.Casino,
        bannerRes = R.drawable.img_cat_classic
    ),
    SNAKES_LADDERS(
        id = "snakes_ladders",
        titleFa = "مار و پله جادویی",
        subtitleFa = "از نردبان بالا برو و از مارها فرار کن",
        rulesFa = "اولین نفری که دقیقاً به خانه ۱۰۰ برسد برنده است. نردبان‌ها صعود سریع و مارها سقوط می‌آورند!",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.CLASSIC,
        accentStart = Color(0xFF10B981),
        accentEnd = Color(0xFF0D9488),
        icon = Icons.Rounded.Route,
        bannerRes = R.drawable.img_cat_classic
    ),
    ULTIMATE_DOOZ(
        id = "ultimate_dooz",
        titleFa = "دوز حرفه‌ای ۵×۵",
        subtitleFa = "چهار علامت هم‌ردیف در صفحه ۵×۵",
        rulesFa = "در صفحه ۵ در ۵، اولین بازیکنی که ۴ علامت پشت‌سرهم افقی، عمودی یا قطری بسازد برنده می‌شود.",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.STRATEGY,
        accentStart = Color(0xFF22D3EE),
        accentEnd = Color(0xFF3B82F6),
        icon = Icons.Rounded.Grid4x4,
        bannerRes = R.drawable.img_cat_strategy
    ),
    CONNECT_FOUR(
        id = "connect_four",
        titleFa = "چهار در یک ردیف",
        subtitleFa = "مهره‌ها را در ستون‌ها بینداز و ردیف کن",
        rulesFa = "مهره شما تا پایین‌ترین خانه خالی ستون سقوط می‌کند. ۴ مهره متوالی بسازید تا پیروز شوید!",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.STRATEGY,
        accentStart = Color(0xFF6366F1),
        accentEnd = Color(0xFFEC4899),
        icon = Icons.Rounded.ViewColumn,
        bannerRes = R.drawable.img_cat_strategy
    ),
    DOTS_BOXES(
        id = "dots_boxes",
        titleFa = "نقطه و خط نئونی",
        subtitleFa = "مربع‌ها را ببند و امتیاز جایزه بگیر",
        rulesFa = "بین دو نقطه مجاور خط بکشید. هرکس ضلع چهارم مربعی را ببندد امتیاز و یک نوبت اضافه می‌گیرد!",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.PARTY,
        accentStart = Color(0xFFF43F5E),
        accentEnd = Color(0xFFF59E0B),
        icon = Icons.Rounded.Hub,
        bannerRes = R.drawable.img_cat_party
    ),
    OTHELLO(
        id = "othello",
        titleFa = "اتللو (ریورسی)",
        subtitleFa = "مهره‌های حریف را محاصره و تصرف کن",
        rulesFa = "مهره خود را طوری قرار دهید که مهره‌های حریف بین دو مهره شما محاصره شده و تغییر رنگ دهند.",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.STRATEGY,
        accentStart = Color(0xFF059669),
        accentEnd = Color(0xFF14B8A6),
        icon = Icons.Rounded.Contrast,
        bannerRes = R.drawable.img_cat_strategy
    ),
    CHECKERS(
        id = "checkers",
        titleFa = "چکرز (دام سلطنتی)",
        subtitleFa = "حرکت مورب، پرش و تاج‌گذاری شاه",
        rulesFa = "مهره‌ها به صورت مورب حرکت می‌کنند و با پرش از روی مهره حریف آن را حذف می‌کنند. در خط آخر شاه شوید!",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.CLASSIC,
        accentStart = Color(0xFFD97706),
        accentEnd = Color(0xFF92400E),
        icon = Icons.Rounded.Layers,
        bannerRes = R.drawable.img_cat_classic
    ),
    BATTLESHIP(
        id = "battleship",
        titleFa = "نبرد دریایی راداری",
        subtitleFa = "ناوگان پنهان حریف را شناسایی و غرق کن",
        rulesFa = "روی رادار دشمن شلیک کنید تا ۴ کشتی پنهان حریف را قبل از غرق شدن ناوگان خودتان نابود کنید!",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.STRATEGY,
        accentStart = Color(0xFF0EA5E9),
        accentEnd = Color(0xFF1D4ED8),
        icon = Icons.Rounded.DirectionsBoat,
        bannerRes = R.drawable.img_cat_strategy
    ),
    MEMORY_MATCH(
        id = "memory_match",
        titleFa = "تطبیق کارت ایرانی",
        subtitleFa = "رقابت حافظه روی ۲۰ کارت طرح‌دار",
        rulesFa = "دو کارت برگردانید؛ اگر جفت باشند امتیاز گرفته و دوباره نوبت شما می‌شود!",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.PARTY,
        accentStart = Color(0xFFA855F7),
        accentEnd = Color(0xFFEC4899),
        icon = Icons.Rounded.Extension,
        bannerRes = R.drawable.img_cat_party
    ),
    HEX_PATH(
        id = "hex_path",
        titleFa = "مسیر شش‌ضلعی",
        subtitleFa = "دو سمت مقابل صفحه را به هم وصل کن",
        rulesFa = "بازیکن قرمز باید بالا را به پایین و بازیکن فیروزه‌ای باید راست را به چپ متصل کند.",
        minPlayers = 2,
        maxPlayers = 2,
        category = GameCategory.STRATEGY,
        accentStart = Color(0xFF14B8A6),
        accentEnd = Color(0xFF06B6D4),
        icon = Icons.Rounded.Hexagon,
        bannerRes = R.drawable.img_cat_strategy
    ),
    PIG_DICE(
        id = "pig_dice",
        titleFa = "تاس طلایی طمع",
        subtitleFa = "ریسک کن یا امتیاز را ذخیره کن!",
        rulesFa = "هرچقدر می‌خواهید تاس بریزید و جمع کنید؛ اما اگر ۱ بیاید تمام امتیاز این دور می‌سوزد! هدف: ۵۰ امتیاز.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.PARTY,
        accentStart = Color(0xFFFBBF24),
        accentEnd = Color(0xFFEF4444),
        icon = Icons.Rounded.AutoAwesome,
        bannerRes = R.drawable.img_cat_party
    ),
    REFLEX_DUEL(
        id = "reflex_duel",
        titleFa = "دوئل واکنش سریع",
        subtitleFa = "مسابقه سرعت‌عمل در ۵ راند نفس‌گیر",
        rulesFa = "منتظر روشن شدن نور طلایی بمانید و سریع‌تر از همه ضربه بزنید! ضربه زودهنگام خطا محسوب می‌شود.",
        minPlayers = 2,
        maxPlayers = 4,
        category = GameCategory.PARTY,
        accentStart = Color(0xFFFB7185),
        accentEnd = Color(0xFF8B5CF6),
        icon = Icons.Rounded.Bolt,
        bannerRes = R.drawable.img_cat_party
    );

    companion object {
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: LUDO
    }
}

enum class RoomMode(val titleFa: String) {
    WIFI_HOST("میزبان اتاق وای‌فای"),
    WIFI_CLIENT("مهمان اتاق وای‌فای"),
    LOCAL_PASS_AND_PLAY("بازی گروهی روی یک گوشی"),
    SOLO_VS_BOT("بازی با هوش مصنوعی")
}

data class PlayerInfo(
    val id: String,
    val name: String,
    val avatarIndex: Int = 0,
    val colorIndex: Int = 0,
    val isHost: Boolean = false,
    val isBot: Boolean = false,
    val isReady: Boolean = true,
    val isSpeaking: Boolean = false,
    val voiceLevel: Float = 0f,
    val ipAddress: String = ""
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderColorIndex: Int,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isReaction: Boolean = false
)

data class DiscoveredRoom(
    val roomId: String,
    val roomName: String,
    val hostName: String,
    val hostIp: String,
    val tcpPort: Int,
    val voicePort: Int,
    val gameType: GameType,
    val playerCount: Int,
    val maxPlayers: Int,
    val isPlaying: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class VoiceChatState(
    val isMicActive: Boolean = false,
    val isOpenMic: Boolean = false,
    val isPushToTalkPressed: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val localMicLevel: Float = 0f,
    val activeSpeakerName: String? = null,
    val hasRecordPermission: Boolean = false,
    val errorMessage: String? = null
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatarIndex: Int,
    val favoriteColorIndex: Int,
    val totalWins: Int,
    val gamesPlayed: Int
)

sealed interface AppScreen {
    data object Home : AppScreen
    data object WifiHub : AppScreen
    data object Profile : AppScreen
    data object RoomLobby : AppScreen
    data object ActiveGame : AppScreen
}

val AvatarSymbols = listOf("👑", "🦁", "🦅", "🦊", "🐉", "⚡", "🌙", "💎")

val QuickPersianReactions = listOf(
    "آفرین! 👏",
    "عجب حرکتی! 🔥",
    "شانس آوردی! 😅",
    "نوبت توئه! 🎲",
    "ای بابا! 😱",
    "بریم دست بعد! 🏆"
)
