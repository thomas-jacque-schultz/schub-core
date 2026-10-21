package schultz.thomas.schub.core.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import schultz.thomas.schub.core.api.mapper.AccountDtoMapperImpl;
import schultz.thomas.schub.core.api.mapper.GameServerMapperImpl;
import schultz.thomas.schub.core.api.mapper.IngestDtoMapperImpl;
import schultz.thomas.schub.core.api.mapper.GameServerProjection;
import schultz.thomas.schub.core.api.mapper.PortForwardingDtoMapperImpl;
import schultz.thomas.schub.core.api.mapper.StaticPortRuleMapperImpl;
import schultz.thomas.schub.core.api.mapper.UserDtoMapper;
import schultz.thomas.schub.core.augur.api.mapper.AugurDtoMapperImpl;
import schultz.thomas.schub.core.augur.business.service.AugurService;
import schultz.thomas.schub.core.augur.business.service.PatternService;
import schultz.thomas.schub.core.business.service.ActivityService;
import schultz.thomas.schub.core.business.service.ContainerRequestService;
import schultz.thomas.schub.core.business.service.DeploymentService;
import schultz.thomas.schub.core.business.service.GameServerControlPolicy;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.PortForwardingService;
import schultz.thomas.schub.core.business.service.RedirectionRequestService;
import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RoleService;
import schultz.thomas.schub.core.business.service.StaticPortRuleService;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.config.ContractNullability;
import schultz.thomas.schub.core.config.OpenApiConfiguration;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.lol.draft.api.mapper.DraftDtoMapperImpl;
import schultz.thomas.schub.core.lol.draft.business.service.CompositionRecordService;
import schultz.thomas.schub.core.lol.draft.business.service.DraftAdviceService;
import schultz.thomas.schub.core.lol.draft.business.service.TierCombosService;
import schultz.thomas.schub.core.lol.riot.api.mapper.RiotDtoMapperImpl;
import schultz.thomas.schub.core.lol.riot.business.service.RiotDataInvalidationService;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.seo.api.client.ConnectorRiotSeoGateway;
import schultz.thomas.schub.core.lol.seo.api.mapper.SeoDtoMapperImpl;
import schultz.thomas.schub.core.lol.seo.business.service.PremadeLabSettingsService;
import schultz.thomas.schub.core.lol.seo.business.service.PublicTeamService;
import schultz.thomas.schub.core.lol.seo.business.service.SearchedPlayerService;
import schultz.thomas.schub.core.lol.session.api.mapper.SessionSummaryDtoMapperImpl;
import schultz.thomas.schub.core.lol.session.business.service.SessionSummaryService;
import schultz.thomas.schub.core.lol.session.business.service.SessionWatcher;
import schultz.thomas.schub.core.lol.session.data.repository.TeamSessionRepository;
import schultz.thomas.schub.core.lol.stats.api.mapper.StatsDtoMapperImpl;
import schultz.thomas.schub.core.lol.stats.business.service.CompanionsService;
import schultz.thomas.schub.core.lol.stats.business.service.MyGamesService;
import schultz.thomas.schub.core.lol.stats.business.service.MyStatsService;
import schultz.thomas.schub.core.lol.stats.business.service.StatsWindows;
import schultz.thomas.schub.core.lol.stats.business.service.TeamGamesStatsService;
import schultz.thomas.schub.core.lol.stats.business.service.TeamOppositionService;
import schultz.thomas.schub.core.lol.stats.business.service.TeamPlayerStatsService;
import schultz.thomas.schub.core.lol.stats.business.service.TeamStatsRefreshService;
import schultz.thomas.schub.core.lol.stats.business.service.TeamSynergyService;
import schultz.thomas.schub.core.lol.team.api.mapper.TeamDtoMapperImpl;
import schultz.thomas.schub.core.lol.team.api.mapper.TeamProjection;
import schultz.thomas.schub.core.lol.team.business.service.ChampionPoolService;
import schultz.thomas.schub.core.lol.team.business.service.CompositionService;
import schultz.thomas.schub.core.lol.team.business.service.TeamService;
import schultz.thomas.schub.core.quiz.api.mapper.QuizDtoMapperImpl;
import schultz.thomas.schub.core.quiz.business.service.QuizQuestionService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Le contrat que lit le front (Schub#124). Changé, il est réécrit ici et le test échoue : on le committe,
// puis le front le recopie (npm run contrat).
@WebMvcTest(properties = {"springdoc.writer-with-order-by-keys=true", "mongock.enabled=false"})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class})
@Import({OpenApiConfiguration.class, ContractNullability.class, AccountDtoMapperImpl.class, AugurDtoMapperImpl.class, DraftDtoMapperImpl.class, GameServerMapperImpl.class, IngestDtoMapperImpl.class, PortForwardingDtoMapperImpl.class, QuizDtoMapperImpl.class, RiotDtoMapperImpl.class, SeoDtoMapperImpl.class, SessionSummaryDtoMapperImpl.class, StaticPortRuleMapperImpl.class, StatsDtoMapperImpl.class, TeamDtoMapperImpl.class})
class OpenApiContractTest {

    static final Path CONTRAT = Path.of("openapi/core.json");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private AugurService augurService;

    @MockBean
    private ChampionPoolService championPoolService;

    @MockBean
    private CompanionsService companionsService;

    @MockBean
    private CompositionRecordService compositionRecordService;

    @MockBean
    private CompositionService compositionService;

    @MockBean
    private ConnectorRiotSeoGateway connectorRiotSeoGateway;

    @MockBean(name = "portainerRequestService")
    private ContainerRequestService containerRequestService;

    @MockBean
    private DeploymentService deploymentService;

    @MockBean
    private DraftAdviceService draftAdviceService;

    @MockBean
    private GameServerControlPolicy gameServerControlPolicy;

    @MockBean
    private GameServerProjection gameServerProjection;

    @MockBean
    private GameServerService gameServerService;

    @MockBean
    private MyGamesService myGamesService;

    @MockBean
    private MyStatsService myStatsService;

    @MockBean
    private PatternService patternService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private PortForwardingProperties portForwardingProperties;

    @MockBean
    private PortForwardingService portForwardingService;

    @MockBean
    private PremadeLabSettingsService premadeLabSettingsService;

    @MockBean
    private PublicTeamService publicTeamService;

    @MockBean
    private QuizQuestionService quizQuestionService;

    @MockBean
    private RedirectionRequestService redirectionRequestService;

    @MockBean
    private RiotAccountService riotAccountService;

    @MockBean
    private RiotConnectorService riotConnectorService;

    @MockBean
    private RiotDataInvalidationService riotDataInvalidationService;

    @MockBean
    private RiotStatsGateway riotStatsGateway;

    @MockBean
    private RoleService roleService;

    @MockBean
    private SearchedPlayerService searchedPlayerService;

    @MockBean
    private SessionSummaryService sessionSummaryService;

    @MockBean
    private SessionWatcher sessionWatcher;

    @MockBean
    private StaticPortRuleService staticPortRuleService;

    @MockBean
    private StatsWindows statsWindows;

    @MockBean
    private TeamGamesStatsService teamGamesStatsService;

    @MockBean
    private TeamOppositionService teamOppositionService;

    @MockBean
    private TeamPlayerStatsService teamPlayerStatsService;

    @MockBean
    private TeamProjection teamProjection;

    @MockBean
    private TeamService teamService;

    @MockBean
    private TeamSessionRepository teamSessionRepository;

    @MockBean
    private TeamStatsRefreshService teamStatsRefreshService;

    @MockBean
    private TeamSynergyService teamSynergyService;

    @MockBean
    private TierCombosService tierCombosService;

    @MockBean
    private UserDtoMapper userDtoMapper;

    @MockBean
    private UserService userService;
    @Test
    @DisplayName("le contrat versionné est celui que le code expose")
    void contratAJour() throws Exception {
        String brut = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        String contrat = new ObjectMapper().readTree(brut).toPrettyString() + "\n";
        String versionne = Files.exists(CONTRAT) ? Files.readString(CONTRAT) : "";
        if (!contrat.equals(versionne)) {
            Files.createDirectories(CONTRAT.getParent());
            Files.writeString(CONTRAT, contrat);
        }
        assertThat(versionne).as("contrat réécrit dans %s : à committer", CONTRAT).isEqualTo(contrat);
    }
}
