package schultz.thomas.schub.core.augur.data.model;

// observed : ce que la condition compare (le centile pour une condition en centile) ; value : la mesure brute.
public record ConditionTrace(Role role, String signal, Condition.Unit unit, double from, double to,
                             Double observed, Double value, Double degree) {

    public enum Role { REQUIRED, OPTIONAL, EXCEPTION }
}
