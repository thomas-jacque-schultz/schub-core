package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.business.model.RiotAccountState;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La liaison de compte Riot, et surtout ses trois façons de mal tourner.
 *
 * <p>Ce qui est testé ici est ce qui échoue <em>en silence</em> : un {@code puuid} pris par
 * quelqu'un d'autre, un connecteur éteint, une saisie qui n'est pas un Riot ID. Aucun des trois
 * ne lève tout seul, et les trois produisent une donnée fausse ou une fonctionnalité morte si on
 * ne les traite pas.</p>
 */
class RiotAccountServiceTest {

    private static final String PUUID = "puuid-de-thomas";

    private UserRepository userRepository;
    private RiotIdResolver riotIdResolver;
    private RiotAccountService service;

    private User acteur;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        riotIdResolver = mock(RiotIdResolver.class);
        service = new RiotAccountService(userRepository, riotIdResolver);

        acteur = compte("user-1", "discord-1");

        when(userRepository.findByRiotPuuid(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Un Riot ID résolu donne un compte lié, et c'est le puuid qui est stocké")
    void lieUnRiotIdResolu() {
        when(riotIdResolver.resolvePuuid("J1HUIV", "000")).thenReturn(Optional.of(PUUID));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000");

        assertThat(dto.state()).isEqualTo(RiotAccountState.RESOLU);
        assertThat(dto.riotId()).isEqualTo("J1HUIV#000");
        assertThat(dto.gameName()).isEqualTo("J1HUIV");
        assertThat(dto.tagLine()).isEqualTo("000");
        assertThat(dto.linkedAt()).isNotNull();
        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
    }

    @Test
    @DisplayName("Le puuid ne figure jamais dans la réponse — aucun écran n'en a l'usage")
    void neSertJamaisLePuuid() {
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.of(PUUID));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000");

        assertThat(dto.toString()).doesNotContain(PUUID);
    }

    // --- un puuid déjà pris ---

    @Test
    @DisplayName("Un puuid déjà lié à un autre compte est refusé — deux comptes ne sont pas le même joueur")
    void refuseUnPuuidDejaLie() {
        User autre = compte("user-2", "discord-2");
        autre.setRiotPuuid(PUUID);
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.of(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service.link(acteur, "J1HUIV#000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà lié");

        verify(userRepository, never()).save(any(User.class));
        assertThat(acteur.getRiotPuuid()).isNull();
    }

    @Test
    @DisplayName("Re-déclarer son propre Riot ID n'est pas un doublon de soi-même")
    void accepteSonPropreCompte() {
        acteur.setRiotPuuid(PUUID);
        acteur.setRiotGameName("J1HUIV");
        acteur.setRiotTagLine("000");
        acteur.setRiotLinkedAt(Instant.parse("2026-09-01T10:00:00Z"));
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.of(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(acteur));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000");

        assertThat(dto.state()).isEqualTo(RiotAccountState.RESOLU);
        assertThat(dto.linkedAt())
                .as("la date de déclaration ne bouge pas quand on relance la résolution du même Riot ID")
                .isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Un Riot ID déjà déclaré par un compte non résolu est refusé quand on ne résout pas non plus")
    void refuseUnRiotIdDejaDeclareQuandRienNEstResolu() {
        User autre = compte("user-2", "discord-2");
        autre.setRiotGameName("J1HUIV");
        autre.setRiotTagLine("000");
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.empty());
        // La casse est celle de la requête Mongo (`IgnoreCase`), pas du service : le bouchon
        // répond donc quelle que soit la casse saisie, comme le dépôt réel le ferait.
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(autre));

        assertThatThrownBy(() -> service.link(acteur, "j1huiv#000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà déclaré");
    }

    @Test
    @DisplayName("Un pseudo périmé sur un compte déjà résolu ne bloque pas le vrai propriétaire")
    void unPseudoPerimeNeBloquePas() {
        User ancienProprietaire = compte("user-2", "discord-2");
        ancienProprietaire.setRiotGameName("J1HUIV");
        ancienProprietaire.setRiotTagLine("000");
        ancienProprietaire.setRiotPuuid("puuid-de-quelqu-un-d-autre");
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.of(PUUID));
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(ancienProprietaire));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000");

        assertThat(dto.state())
                .as("le puuid est la clé stable : un Riot ID recopié ailleurs ne décide de rien")
                .isEqualTo(RiotAccountState.RESOLU);
    }

    // --- le connecteur injoignable ---

    @Test
    @DisplayName("Connecteur injoignable : la déclaration est conservée, en attente de résolution")
    void accepteSansPuuidQuandLeConnecteurNeRepondPas() {
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.empty());

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000");

        assertThat(dto.state()).isEqualTo(RiotAccountState.EN_ATTENTE_DE_RESOLUTION);
        assertThat(dto.riotId()).isEqualTo("J1HUIV#000");
        assertThat(acteur.getRiotGameName()).isEqualTo("J1HUIV");
        assertThat(acteur.getRiotPuuid()).isNull();
        verify(userRepository).save(acteur);
    }

    @Test
    @DisplayName("Rejouer la déclaration relance la résolution — c'est le « réessayer » de l'écran")
    void relanceLaResolutionAuSecondAppel() {
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.empty());
        assertThat(service.link(acteur, "J1HUIV#000").state())
                .isEqualTo(RiotAccountState.EN_ATTENTE_DE_RESOLUTION);

        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.of(PUUID));

        assertThat(service.link(acteur, "J1HUIV#000").state()).isEqualTo(RiotAccountState.RESOLU);
        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
    }

    // --- saisies refusées ---

    @Test
    @DisplayName("Ce qui n'est pas un Riot ID est refusé en 400, pas enregistré tel quel")
    void refuseLesSaisiesQuiNeSontPasDesRiotId() {
        List<String> saisies = List.of("J1HUIV", "#000", "J1HUIV#", "   ", "A#B#C", "");

        for (String saisie : saisies) {
            assertThatThrownBy(() -> service.link(acteur, saisie))
                    .as("saisie refusée : « %s »", saisie)
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.link(acteur, null)).isInstanceOf(IllegalArgumentException.class);
        verify(riotIdResolver, never()).resolvePuuid(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Les espaces autour de la saisie sont mangés, ceux du pseudo respectés")
    void nettoieLaSaisie() {
        when(riotIdResolver.resolvePuuid("Le Joueur", "EUW")).thenReturn(Optional.of(PUUID));

        RiotAccountDto dto = service.link(acteur, "  Le Joueur # EUW  ");

        assertThat(dto.riotId()).isEqualTo("Le Joueur#EUW");
    }

    // --- lecture et retrait ---

    @Test
    @DisplayName("Un compte sans Riot ID est ABSENT, et ne prétend rien d'autre")
    void litUnCompteSansRiotId() {
        RiotAccountDto dto = service.of(acteur);

        assertThat(dto.state()).isEqualTo(RiotAccountState.ABSENT);
        assertThat(dto.riotId()).isNull();
        assertThat(dto.linkedAt()).isNull();
    }

    @Test
    @DisplayName("Délier efface les trois champs ensemble")
    void delie() {
        acteur.setRiotPuuid(PUUID);
        acteur.setRiotGameName("J1HUIV");
        acteur.setRiotTagLine("000");
        acteur.setRiotLinkedAt(Instant.now());

        RiotAccountDto dto = service.unlink(acteur);

        assertThat(dto.state()).isEqualTo(RiotAccountState.ABSENT);
        assertThat(acteur.getRiotPuuid()).isNull();
        assertThat(acteur.getRiotGameName()).isNull();
        assertThat(acteur.getRiotTagLine()).isNull();
        assertThat(acteur.getRiotLinkedAt()).isNull();
    }

    @Test
    @DisplayName("Délier un compte qui ne l'était pas n'écrit rien")
    void delierSansLienNEcritRien() {
        assertThat(service.unlink(acteur).state()).isEqualTo(RiotAccountState.ABSENT);
        verify(userRepository, never()).save(any(User.class));
    }

    private static User compte(String id, String discordId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId(discordId);
        return user;
    }
}
