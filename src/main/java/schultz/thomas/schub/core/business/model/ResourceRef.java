package schultz.thomas.schub.core.business.model;

public record ResourceRef(ResourceType type, String id) {

    public static ResourceRef team(String id) {
        return id == null ? null : new ResourceRef(ResourceType.TEAM, id);
    }
}
