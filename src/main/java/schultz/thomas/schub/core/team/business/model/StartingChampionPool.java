package schultz.thomas.schub.core.team.business.model;

import java.util.List;
import java.util.Map;

// Sélection de l'équipe Miam du dev (30/09), complétée des champions courants qui y manquaient (Schub#69).
// Clés Data Dragon. À relire à chaque grosse mise à jour de méta.
public final class StartingChampionPool {

    public static final Map<GameRole, List<String>> SELECTION = Map.of(
            GameRole.TOP, List.of(
                    "Shen", "Ambessa", "Sion", "Yasuo", "Yone", "MonkeyKing", "Urgot", "TahmKench", "Renekton",
                    "Ornn", "Mordekaiser", "Gnar", "Garen", "Gangplank", "Galio", "DrMundo", "Darius", "Illaoi",
                    "Irelia", "Kled", "KSante", "Malphite", "Nasus", "Aatrox", "Camille", "Fiora", "Gwen", "Jax",
                    "Jayce", "Riven", "Sett", "Teemo", "Volibear", "Kennen"),
            GameRole.JGL, List.of(
                    "Shen", "LeeSin", "Zed", "Viego", "Rengar", "Nocturne", "Fizz", "DrMundo", "Kindred", "Sylas",
                    "Kayn", "Ekko", "Fiddlesticks", "Jax", "Teemo", "Lillia", "Darius", "JarvanIV", "MasterYi",
                    "Warwick", "Nasus", "Ivern", "Malphite", "Morgana", "Amumu", "Shyvana", "MonkeyKing", "Khazix",
                    "Volibear", "Rammus", "Brand", "Diana", "Nunu", "Maokai", "Trundle", "Skarner", "Elise", "Jayce",
                    "Gragas", "Hecarim", "Aatrox", "Chogath", "Briar", "Belveth", "Gwen", "Karthus", "Mordekaiser",
                    "Naafiri", "RekSai", "Pantheon", "Olaf", "Sejuani", "Shaco", "Taliyah", "Vi", "Zac", "Zaahen",
                    "Zyra", "Graves", "Nidalee", "XinZhao", "Evelynn", "Talon"),
            GameRole.MID, List.of(
                    "Lux", "Akali", "Azir", "Akshan", "Veigar", "Seraphine", "Neeko", "Ahri", "Orianna", "Velkoz",
                    "AurelionSol", "Fizz", "Zilean", "Heimerdinger", "Ziggs", "Tristana", "Morgana", "Xerath",
                    "Annie", "Anivia", "Aurora", "Brand", "Chogath", "Cassiopeia", "Diana", "Ezreal", "Hwei", "Gwen",
                    "Galio", "Jayce", "Leblanc", "Kennen", "Kayle", "Kassadin", "Lissandra", "Locke", "Malzahar",
                    "Mel", "Sylas", "Syndra", "Taliyah", "Vex", "Viktor", "Vladimir", "Yone", "Zoe", "Zyra", "Yasuo",
                    "Zed", "Katarina", "Talon", "Qiyana", "TwistedFate", "Ryze"),
            GameRole.ADC, List.of(
                    "Ezreal", "MissFortune", "Jinx", "Xayah", "Senna", "Kaisa", "Caitlyn", "Twitch", "Ashe",
                    "Tristana", "Smolder", "Vayne", "Varus", "Zeri", "Ziggs", "Yunara", "Veigar", "Teemo", "Sivir",
                    "Nilah", "Mel", "Kalista", "Jhin", "Aphelios", "Draven", "Lucian", "Samira", "KogMaw"),
            GameRole.SUP, List.of(
                    "Lux", "Leona", "Sona", "Swain", "Rell", "Braum", "Soraka", "Bard", "Zyra", "Blitzcrank",
                    "Velkoz", "Zilean", "Lulu", "Nautilus", "Janna", "Brand", "Alistar", "Amumu", "Galio", "Karma",
                    "Milio", "Shen", "Thresh", "TahmKench", "Yuumi", "Seraphine", "Renata", "Poppy", "Nami",
                    "Morgana", "Neeko", "Maokai", "Pyke", "Rakan", "Taric"));

    private StartingChampionPool() {
    }
}
