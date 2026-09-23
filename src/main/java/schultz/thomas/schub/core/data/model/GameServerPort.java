package schultz.thomas.schub.core.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameServerPort {

    private String proto;

    private Integer wanPort;

    private Integer lanPort;

    private String lanIp;
}
