package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.service.RiotConnectorService.AccountsByRank;

import java.util.List;

public record AccountsByRankDto(boolean available, List<Row> rows) {

    public record Row(String tier, long tracked, long seeds) {
    }

    public static AccountsByRankDto from(AccountsByRank accounts) {
        return new AccountsByRankDto(true, accounts.rows().stream()
                .map(row -> new Row(row.tier(), row.tracked(), row.seeds()))
                .toList());
    }

    public static AccountsByRankDto unavailable() {
        return new AccountsByRankDto(false, List.of());
    }
}
