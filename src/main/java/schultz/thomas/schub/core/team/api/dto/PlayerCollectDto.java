package schultz.thomas.schub.core.team.api.dto;

// KNOWN : rien à collecter. FAST : voie interactive. SLOW : au-delà du budget, au compte-gouttes.
public record PlayerCollectDto(Lane lane) {

    public enum Lane { KNOWN, FAST, SLOW }
}
