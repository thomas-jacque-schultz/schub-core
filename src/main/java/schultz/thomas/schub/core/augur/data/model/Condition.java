package schultz.thomas.schub.core.augur.data.model;

// Une rampe : le degré vaut 0 en « from », 1 en « to », et varie linéairement entre les deux.
// from < to : le degré monte avec le signal ; from > to : il monte quand le signal baisse.
// PERCENTILE : le signal est situé dans le référentiel de son palier et de son poste (0 à 100).
public record Condition(String signal, Unit unit, double from, double to, double weight) {

    public enum Unit { VALUE, PERCENTILE }

    public double weightOrOne() {
        return weight <= 0 ? 1 : weight;
    }
}
