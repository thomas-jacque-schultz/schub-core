package schultz.thomas.schub.core.business.model;

import lombok.EqualsAndHashCode;
import schultz.thomas.schub.core.data.model.GameServerCommandFailure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class DockerContainerState {

    private boolean running=false;

    private GameServerCommandFailure commandFailure;

}
