package schultz.thomas.schub.core.team.data.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

// Un créneau d'entraînement (Schub#102), à l'heure de Paris. Récurrent (day) ou ponctuel (date) ;
// une fin avant le début passe minuit.
public record TrainingSlot(String id, DayOfWeek day, LocalDate date, LocalTime start, LocalTime end) {

    public boolean recurring() {
        return date == null;
    }
}
