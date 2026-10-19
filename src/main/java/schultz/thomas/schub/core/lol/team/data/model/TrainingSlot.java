package schultz.thomas.schub.core.lol.team.data.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

// Un créneau d'entraînement (Schub#102), à l'heure de Paris. Récurrent (day) ou ponctuel (date) ; une fin avant
// le début passe minuit. Heures et date en texte ISO : Spring Data convertirait un LocalTime en date complète, au
// fuseau de la JVM.
public record TrainingSlot(String id, DayOfWeek day, String date, String start, String end) {

    public boolean recurring() {
        return date == null;
    }

    public LocalDate localDate() {
        return date == null ? null : LocalDate.parse(date);
    }

    public LocalTime startTime() {
        return LocalTime.parse(start);
    }

    public LocalTime endTime() {
        return LocalTime.parse(end);
    }
}
