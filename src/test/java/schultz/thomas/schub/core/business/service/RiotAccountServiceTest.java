package schultz.thomas.schub.core.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.api.dto.RiotAccountChangeDto;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.api.dto.RiotAccountSuggestionDto;
import schultz.thomas.schub.core.api.dto.RiotIngestDto;
import schultz.thomas.schub.core.business.model.RiotAccountState;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La liaison de compte Riot, son remplacement, et surtout leurs façons de mal tourner.
 *
 * <p>Ce qui est testé ici est ce qui échoue <em>en silence</em> : un {@code puuid} pris par
 * quelqu'un d'autre, un connecteur éteint, un compte remplacé sans que personne ne le dise,
 * une saisie qui n'est pas un Riot ID. Aucun ne lève tout seul, et tous produisent une donnée
 * fausse ou une fonctionnalité morte si on ne les traite pas.</p>
 */
class RiotAccountServiceTest {

    private static final String PUUID = "puuid-de-thomas";

    private UserRepository userRepository;
    private RiotIdResolver riotIdResolver;
    private RiotConnectorService riotConnectorService;
    private RiotAccountService service;

    private User acteur;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        riotIdResolver = mock(RiotIdResolver.class);
        riotConnectorService = mock(RiotConnectorService.class);
        service = new RiotAccountService(userRepository, riotIdResolver, riotConnectorService,
                mock(org.springframework.context.ApplicationEventPublisher.class));

        acteur = compte("user-1", "discord-1");

        when(userRepository.findByRiotPuuid(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Un Riot ID résolu donne un compte lié, et c'est le puuid qui est stocké")
    void lieUnRiotIdResolu() {
        when(riotIdResolver.resolve("J1HUIV", "000")).thenReturn(RiotIdResolution.resolved(PUUID));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

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
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

        assertThat(dto.toString()).doesNotContain(PUUID);
    }

    // --- un puuid déjà pris ---

    @Test
    @DisplayName("Un puuid déjà lié à un autre compte est refusé — deux comptes ne sont pas le même joueur")
    void refuseUnPuuidDejaLie() {
        User autre = compte("user-2", "discord-2");
        autre.setRiotPuuid(PUUID);
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service.link(acteur, "J1HUIV#000", false))
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
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(acteur));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

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
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.unavailable());
        // La casse est celle de la requête Mongo (`IgnoreCase`), pas du service : le bouchon
        // répond donc quelle que soit la casse saisie, comme le dépôt réel le ferait.
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(autre));

        assertThatThrownBy(() -> service.link(acteur, "j1huiv#000", false))
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
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));
        when(userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(ancienProprietaire));

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

        assertThat(dto.state())
                .as("le puuid est la clé stable : un Riot ID recopié ailleurs ne décide de rien")
                .isEqualTo(RiotAccountState.RESOLU);
    }

    // --- le connecteur injoignable ---

    @Test
    @DisplayName("Connecteur injoignable : la déclaration est conservée, en attente de résolution")
    void accepteSansPuuidQuandLeConnecteurNeRepondPas() {
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.unavailable());

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

        assertThat(dto.state()).isEqualTo(RiotAccountState.EN_ATTENTE_DE_RESOLUTION);
        assertThat(dto.riotId()).isEqualTo("J1HUIV#000");
        assertThat(acteur.getRiotGameName()).isEqualTo("J1HUIV");
        assertThat(acteur.getRiotPuuid()).isNull();
        verify(userRepository).save(acteur);
    }

    @Test
    @DisplayName("Rejouer la déclaration relance la résolution — c'est le « réessayer » de l'écran")
    void relanceLaResolutionAuSecondAppel() {
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.unavailable());
        assertThat(service.link(acteur, "J1HUIV#000", false).state())
                .isEqualTo(RiotAccountState.EN_ATTENTE_DE_RESOLUTION);

        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));

        assertThat(service.link(acteur, "J1HUIV#000", false).state()).isEqualTo(RiotAccountState.RESOLU);
        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
    }

    // --- saisies refusées ---

    @Test
    @DisplayName("Ce qui n'est pas un Riot ID est refusé en 400, pas enregistré tel quel")
    void refuseLesSaisiesQuiNeSontPasDesRiotId() {
        List<String> saisies = List.of("J1HUIV", "#000", "J1HUIV#", "   ", "A#B#C", "");

        for (String saisie : saisies) {
            assertThatThrownBy(() -> service.link(acteur, saisie, false))
                    .as("saisie refusée : « %s »", saisie)
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.link(acteur, null, false)).isInstanceOf(IllegalArgumentException.class);
        verify(riotIdResolver, never()).resolve(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Les espaces autour de la saisie sont mangés, ceux du pseudo respectés")
    void nettoieLaSaisie() {
        when(riotIdResolver.resolve("Le Joueur", "EUW")).thenReturn(RiotIdResolution.resolved(PUUID));

        RiotAccountDto dto = service.link(acteur, "  Le Joueur # EUW  ", false);

        assertThat(dto.riotId()).isEqualTo("Le Joueur#EUW");
    }

    // --- lecture ---

    @Test
    @DisplayName("Un compte sans Riot ID est ABSENT, et ne prétend rien d'autre")
    void litUnCompteSansRiotId() {
        RiotAccountDto dto = service.of(acteur);

        assertThat(dto.state()).isEqualTo(RiotAccountState.ABSENT);
        assertThat(dto.riotId()).isNull();
        assertThat(dto.linkedAt()).isNull();
    }

    // --- le changement de compte ---

    @Test
    @DisplayName("Remplacer un compte résolu par un autre est refusé tant que ce n'est pas confirmé")
    void refuseUnChangementNonConfirme() {
        acteurDejaLie();
        when(riotIdResolver.resolve("Nouveau", "EUW")).thenReturn(RiotIdResolution.resolved("puuid-nouveau"));

        assertThatThrownBy(() -> service.link(acteur, "Nouveau#EUW", false))
                .isInstanceOf(RiotAccountChangeNotConfirmedException.class);

        verify(userRepository, never()).save(any(User.class));
        assertThat(acteur.getRiotPuuid())
                .as("un refus ne change rien : le compte reste celui d'avant")
                .isEqualTo(PUUID);
    }

    @Test
    @DisplayName("Le refus porte les conséquences, sinon l'écran devrait les réécrire en dur")
    void leRefusPorteLesConsequences() {
        acteurDejaLie();
        when(riotIdResolver.resolve("Nouveau", "EUW")).thenReturn(RiotIdResolution.resolved("puuid-nouveau"));

        RiotAccountChangeDto change = catchThrowableOfType(
                () -> service.link(acteur, "Nouveau#EUW", false),
                RiotAccountChangeNotConfirmedException.class).getChange();

        assertThat(change.previousRiotId()).isEqualTo("J1HUIV#000");
        assertThat(change.riotId()).isEqualTo("Nouveau#EUW");
        assertThat(change.statsReset()).isTrue();
        assertThat(change.rosterSlotsToClaim()).isTrue();
        assertThat(change.estimatedMatches()).isEqualTo(1_000);
        assertThat(change.estimatedDuration()).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    @DisplayName("Confirmé, le changement s'applique et demande la collecte du nouveau compte")
    void appliqueUnChangementConfirme() {
        acteurDejaLie();
        when(riotIdResolver.resolve("Nouveau", "EUW")).thenReturn(RiotIdResolution.resolved("puuid-nouveau"));
        when(riotConnectorService.requestIngest("puuid-nouveau")).thenReturn(true);

        RiotAccountDto dto = service.link(acteur, "Nouveau#EUW", true);

        assertThat(acteur.getRiotPuuid()).isEqualTo("puuid-nouveau");
        assertThat(dto.change()).isNotNull();
        assertThat(dto.change().ingestRestarted()).isTrue();
        assertThat(dto.linkedAt()).isNotEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
        verify(riotConnectorService).requestIngest("puuid-nouveau");
    }

    @Test
    @DisplayName("Changer de Riot ID sans changer de compte n'est pas un changement : même puuid")
    void unRenommageChezRiotNEstPasUnChangementDeCompte() {
        acteurDejaLie();
        when(riotIdResolver.resolve("NouveauPseudo", "EUW")).thenReturn(RiotIdResolution.resolved(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(acteur));

        RiotAccountDto dto = service.link(acteur, "NouveauPseudo#EUW", false);

        assertThat(dto.change())
                .as("aucun avertissement sur une simple mise à jour de pseudo")
                .isNull();
        assertThat(dto.riotId()).isEqualTo("NouveauPseudo#EUW");
        verify(riotConnectorService, never()).requestIngest(anyString());
    }

    @Test
    @DisplayName("Relancer le même Riot ID avec un connecteur muet ne perd pas le puuid connu")
    void nePerdPasLePuuidQuandLeConnecteurSeTait() {
        acteurDejaLie();
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.unavailable());

        RiotAccountDto dto = service.link(acteur, "J1HUIV#000", false);

        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
        assertThat(dto.state()).isEqualTo(RiotAccountState.RESOLU);
    }

    @Test
    @DisplayName("Une première liaison demande la collecte : sans elle, aucune partie n'arriverait jamais")
    void demandeLaCollecteALaPremiereLiaison() {
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));

        service.link(acteur, "J1HUIV#000", false);

        verify(riotConnectorService).requestIngest(PUUID);
    }

    // --- le connecteur d'ingestion éteint ---

    @Test
    @DisplayName("Connecteur muet : l'avancement est nul et le compte se lit quand même")
    void litLeCompteSansLAvancement() {
        acteurDejaLie();
        when(riotConnectorService.ingestOf(PUUID)).thenReturn(Optional.empty());

        RiotAccountDto dto = service.of(acteur);

        assertThat(dto.state()).isEqualTo(RiotAccountState.RESOLU);
        assertThat(dto.ingest()).isNull();
    }

    @Test
    @DisplayName("L'avancement de la collecte est reporté tel quel quand le connecteur répond")
    void reporteLAvancementDeLaCollecte() {
        acteurDejaLie();
        Instant pret = Instant.parse("2026-09-21T12:00:00Z");
        when(riotConnectorService.ingestOf(PUUID))
                .thenReturn(Optional.of(new RiotConnectorService.PlayerIngest(980, 1, pret)));

        RiotIngestDto ingest = service.of(acteur).ingest();

        assertThat(ingest.pending()).isEqualTo(980);
        assertThat(ingest.running()).isEqualTo(1);
        assertThat(ingest.estimatedReadyAt()).isEqualTo(pret);
    }

    // --- les suggestions ---

    @Test
    @DisplayName("Une recherche sans résultat rend une liste vide et n'interroge pas les comptes")
    void suggereRienQuandRienNeRessemble() {
        when(riotConnectorService.search("zzz", 10)).thenReturn(List.of());

        assertThat(service.suggestions(acteur, "zzz", 10)).isEmpty();
        verify(userRepository, never()).findByRiotPuuidIn(any());
    }

    @Test
    @DisplayName("Un compte déjà revendiqué ailleurs est marqué, pas retiré de la liste")
    void marqueLesComptesDejaRevendiques() {
        User autre = compte("user-2", "discord-2");
        autre.setRiotPuuid("puuid-pris");
        when(riotConnectorService.search("thom", 10)).thenReturn(List.of(
                joueur("puuid-pris", "Thomas", "EUW"),
                joueur("puuid-libre", "ThomasBis", "EUW")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of(autre));

        List<RiotAccountSuggestionDto> propositions = service.suggestions(acteur, "thom", 10);

        assertThat(propositions).hasSize(2);
        assertThat(propositions.get(0).alreadyLinked()).isTrue();
        assertThat(propositions.get(0).riotId()).isEqualTo("Thomas#EUW");
        assertThat(propositions.get(1).alreadyLinked()).isFalse();
    }

    @Test
    @DisplayName("Son propre compte est reconnu comme sien, pas comme pris par un autre")
    void reconnaitSonProprePompte() {
        acteurDejaLie();
        when(riotConnectorService.search("j1h", 10)).thenReturn(List.of(joueur(PUUID, "J1HUIV", "000")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of(acteur));

        RiotAccountSuggestionDto proposition = service.suggestions(acteur, "j1h", 10).getFirst();

        assertThat(proposition.mine()).isTrue();
        assertThat(proposition.alreadyLinked()).isFalse();
    }

    @Test
    @DisplayName("Le puuid ne sort pas du cœur, même dans une proposition")
    void neFuitPasLePuuidDansLesPropositions() {
        when(riotConnectorService.search("thom", 10)).thenReturn(List.of(joueur("puuid-secret", "Thomas", "EUW")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of());

        assertThat(service.suggestions(acteur, "thom", 10).toString()).doesNotContain("puuid-secret");
    }

    private void acteurDejaLie() {
        acteur.setRiotPuuid(PUUID);
        acteur.setRiotGameName("J1HUIV");
        acteur.setRiotTagLine("000");
        acteur.setRiotLinkedAt(Instant.parse("2026-09-01T10:00:00Z"));
    }

    private static RiotConnectorService.KnownPlayer joueur(String puuid, String gameName, String tagLine) {
        return new RiotConnectorService.KnownPlayer(puuid, gameName, tagLine, gameName + "#" + tagLine,
                42, List.of(new RiotConnectorService.PositionPlayed("MIDDLE", 30)), Instant.now(),
                Instant.now(), "PARTICIPATION");
    }

    private static User compte(String id, String discordId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId(discordId);
        return user;
    }

    @Test
    @DisplayName("un Riot ID inconnu de Riot est refusé, il ne part pas en attente de résolution")
    void riotIdInconnuRefuse() {
        when(riotIdResolver.resolve("NexistePas", "ZZZZ")).thenReturn(RiotIdResolution.notFound());

        assertThatThrownBy(() -> service.link(acteur, "NexistePas#ZZZZ", true))
                .isInstanceOf(UnknownRiotAccountException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("un compte déjà lié survit à une saisie inexistante")
    void compteExistantPreserveFaceAUnRiotIdInconnu() {
        acteur.setRiotPuuid(PUUID);
        acteur.setRiotGameName("J1HUIV");
        acteur.setRiotTagLine("000");
        when(riotIdResolver.resolve("NexistePas", "ZZZZ")).thenReturn(RiotIdResolution.notFound());

        assertThatThrownBy(() -> service.link(acteur, "NexistePas#ZZZZ", true))
                .isInstanceOf(UnknownRiotAccountException.class);

        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
        assertThat(acteur.getRiotGameName()).isEqualTo("J1HUIV");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("connecteur muet : la saisie est conservée en attente, elle n'est pas refusée")
    void connecteurMuetNestPasUnRefus() {
        when(riotIdResolver.resolve(anyString(), anyString()))
                .thenReturn(RiotIdResolution.unavailable());

        RiotAccountDto dto = service.link(acteur, "Quelquun#EUW", true);

        assertThat(dto.state()).isEqualTo(RiotAccountState.EN_ATTENTE_DE_RESOLUTION);
    }

    // --- la vérification : une saisie partielle cherche, un Riot ID complet demande à Riot ---

    @Test
    @DisplayName("Une saisie partielle ne dérange jamais Riot")
    void neDerangePasRiotSurUneSaisiePartielle() {
        when(riotConnectorService.search("thom", 10)).thenReturn(List.of());

        assertThat(service.suggestions(acteur, "thom", 10)).isEmpty();
        verify(riotIdResolver, never()).resolve(anyString(), anyString());
    }

    @Test
    @DisplayName("Un Riot ID complet est vérifié chez Riot, puis apparaît dans la liste")
    void verifieUnRiotIdCompletEtLeRendTrouvable() {
        when(riotIdResolver.resolve("Thomas", "EUW")).thenReturn(RiotIdResolution.resolved("puuid-neuf"));
        when(riotConnectorService.search("Thomas#EUW", 10))
                .thenReturn(List.of(joueur("puuid-neuf", "Thomas", "EUW")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of());

        List<RiotAccountSuggestionDto> propositions = service.suggestions(acteur, "Thomas#EUW", 10);

        assertThat(propositions).extracting(RiotAccountSuggestionDto::riotId).containsExactly("Thomas#EUW");
        verify(riotIdResolver).resolve("Thomas", "EUW");
    }

    @Test
    @DisplayName("Vérifier ne lie rien : le compte de l'appelant n'est pas touché")
    void laVerificationNeLiePersonne() {
        when(riotIdResolver.resolve("Thomas", "EUW")).thenReturn(RiotIdResolution.resolved("puuid-neuf"));
        when(riotConnectorService.search("Thomas#EUW", 10))
                .thenReturn(List.of(joueur("puuid-neuf", "Thomas", "EUW")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of());

        service.suggestions(acteur, "Thomas#EUW", 10);

        verify(userRepository, never()).save(any(User.class));
        assertThat(acteur.getRiotPuuid()).isNull();
        assertThat(acteur.getRiotGameName()).isNull();
    }

    @Test
    @DisplayName("Un Riot ID que Riot ne connaît pas est refusé, il n'entre pas dans la liste")
    void refuseUnRiotIdInexistantALaVerification() {
        when(riotIdResolver.resolve("NexistePas", "ZZZZ")).thenReturn(RiotIdResolution.notFound());

        assertThatThrownBy(() -> service.suggestions(acteur, "NexistePas#ZZZZ", 10))
                .isInstanceOf(UnknownRiotAccountException.class);
        verify(riotConnectorService, never()).search(anyString(), anyInt());
    }

    @Test
    @DisplayName("Connecteur muet : on le dit, on ne répond pas « ce compte n'existe pas »")
    void distingueLIndisponibiliteDeLInexistence() {
        when(riotIdResolver.resolve("Thomas", "EUW")).thenReturn(RiotIdResolution.unavailable());

        assertThatThrownBy(() -> service.suggestions(acteur, "Thomas#EUW", 10))
                .isInstanceOf(RiotConnectorUnavailableException.class);
        verify(riotConnectorService, never()).search(anyString(), anyInt());
    }

    @Test
    @DisplayName("La fraîcheur de l'observation est servie, c'est elle qui date le Riot ID affiché")
    void sertLaFraicheurDeLObservation() {
        when(riotConnectorService.search("thom", 10)).thenReturn(List.of(
                new RiotConnectorService.KnownPlayer("puuid-vu", "Thomas", "EUW", "Thomas#EUW", 0,
                        List.of(), null, Instant.parse("2024-09-20T18:00:00Z"), "RESOLUTION")));
        when(userRepository.findByRiotPuuidIn(any())).thenReturn(List.of());

        RiotAccountSuggestionDto proposition = service.suggestions(acteur, "thom", 10).getFirst();

        assertThat(proposition.observedAt()).isEqualTo(Instant.parse("2024-09-20T18:00:00Z"));
        assertThat(proposition.source()).isEqualTo("RESOLUTION");
        assertThat(proposition.matchCount()).isZero();
        assertThat(proposition.lastPlayedAt()).isNull();
    }

    @Test
    @DisplayName("un lien resté en attente se résout à la connexion, et lance la collecte")
    void lienEnAttenteResoluALaConnexion() {
        acteur.setRiotGameName("LaFolleDuBus");
        acteur.setRiotTagLine("214");
        acteur.setRiotPuuid(null);
        when(riotIdResolver.resolve("LaFolleDuBus", "214")).thenReturn(RiotIdResolution.resolved(PUUID));

        assertThat(service.resolvePendingLink(acteur)).isTrue();

        assertThat(acteur.getRiotPuuid()).isEqualTo(PUUID);
        verify(riotConnectorService).requestIngest(PUUID);
    }

    @Test
    @DisplayName("un lien déjà résolu n'interroge pas le connecteur à chaque connexion")
    void lienDejaResoluNeRelancePas() {
        acteur.setRiotGameName("J1HUIV");
        acteur.setRiotTagLine("000");
        acteur.setRiotPuuid(PUUID);

        assertThat(service.resolvePendingLink(acteur)).isFalse();

        verify(riotIdResolver, never()).resolve(anyString(), anyString());
    }

    @Test
    @DisplayName("connecteur toujours muet : on reste en attente, sans rien écrire")
    void connecteurToujoursMuetNecritRien() {
        acteur.setRiotGameName("LaFolleDuBus");
        acteur.setRiotTagLine("214");
        acteur.setRiotPuuid(null);
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.unavailable());

        assertThat(service.resolvePendingLink(acteur)).isFalse();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("un puuid déjà revendiqué ailleurs ne se vole pas à la connexion")
    void puuidDejaRevendiqueNeSeVolePas() {
        acteur.setRiotGameName("LaFolleDuBus");
        acteur.setRiotTagLine("214");
        acteur.setRiotPuuid(null);
        User autre = new User();
        autre.setId("un-autre");
        when(riotIdResolver.resolve(anyString(), anyString())).thenReturn(RiotIdResolution.resolved(PUUID));
        when(userRepository.findByRiotPuuid(PUUID)).thenReturn(Optional.of(autre));

        assertThat(service.resolvePendingLink(acteur)).isFalse();

        assertThat(acteur.getRiotPuuid()).isNull();
        verify(userRepository, never()).save(any());
    }
}
