package schultz.thomas.schub.core.data.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Game {

    MINECRAFT("Minecraft", "https://thumbnails.pcgamingwiki.com/4/47/Minecraft_Java_Edition_cover.jpg/450px-Minecraft_Java_Edition_cover.jpg"),
    VALHEIM("Valheim", "https://thumbnails.pcgamingwiki.com/5/57/Valheim_cover.jpg/450px-Valheim_cover.jpg"),
    ARK("Ark", "https://thumbnails.pcgamingwiki.com/6/6f/ARK_Survival_Evolved_cover.jpg/450px-ARK_Survival_Evolved_cover.jpg"),
    RUST("Rust", "https://thumbnails.pcgamingwiki.com/8/88/Rust_header.jpg/450px-Rust_header.jpg"),
    GARRYSMOD("Garry's Mod", "https://images.pcgamingwiki.com/e/e7/Garry%27s_Mod_Logo.jpg"),
    RETURN_MORIA("Return to Moria", "https://thumbnails.pcgamingwiki.com/1/15/LORD_OF_THE_RINGS_RETURN_TO_MORIA_Cover.jpg/300px-LORD_OF_THE_RINGS_RETURN_TO_MORIA_Cover.jpg"),
    PALWORLD("PalWorld", "https://thumbnails.pcgamingwiki.com/6/6a/Palworld_cover.jpg/450px-Palworld_cover.jpg"),
    ASSETTO_CORSA("Assetto Corsa", "https://images.pcgamingwiki.com/7/7b/Assetto_Corsa_-_cover.png"),
    SATISFACTORY("Satisfactory", "https://images.pcgamingwiki.com/d/d1/Satisfactory_cover.png");

    private final String label;
    private final String iconUrl;
}
