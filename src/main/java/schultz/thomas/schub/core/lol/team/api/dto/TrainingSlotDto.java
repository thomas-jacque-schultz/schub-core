package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.time.DayOfWeek;

// Récurrent (day) ou ponctuel (date, AAAA-MM-JJ) ; heures en HH:mm, à l'heure de Paris. Sans id : un créneau à créer.
public record TrainingSlotDto(@Nullable String id, @Nullable DayOfWeek day, @Nullable String date, String start,
                              String end) {
}
