package schultz.thomas.schub.core.lol.seo.business.model;

// KNOWN : rien à collecter. FAST : voie interactive. SLOW : au-delà du budget, au compte-gouttes.
public record PlayerCollect(Lane lane) {

    public enum Lane { KNOWN, FAST, SLOW }
}
