package schultz.thomas.schub.core.data.model;

// Réponse du 05/10 sur #99 : activés pour les séances planifiées, désactivés pour les sessions libres.
public record SessionSummaryPreferences(boolean planned, boolean free) {

    public static final SessionSummaryPreferences DEFAUT = new SessionSummaryPreferences(true, false);

    public static SessionSummaryPreferences of(User user) {
        return user.getSessionSummaries() == null ? DEFAUT : user.getSessionSummaries();
    }
}
