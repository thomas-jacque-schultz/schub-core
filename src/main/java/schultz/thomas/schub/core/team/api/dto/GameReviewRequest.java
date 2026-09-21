package schultz.thomas.schub.core.team.api.dto;

/** Ce qu'on écrit : sur qui, et quoi. Le sujet est ignoré en modification — il ne se déplace pas. */
public record GameReviewRequest(String subjectMemberId, String content) {
}
