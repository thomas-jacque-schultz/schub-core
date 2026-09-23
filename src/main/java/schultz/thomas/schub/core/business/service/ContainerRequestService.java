package schultz.thomas.schub.core.business.service;

import java.util.List;
import schultz.thomas.schub.core.api.dto.PortainerStack;
import schultz.thomas.schub.core.business.model.DockerContainerState;

import org.springframework.stereotype.Service;
@Service
public interface ContainerRequestService {

    boolean startContainer(Integer stackId);
    boolean stopContainer(Integer stackId);
    DockerContainerState getContainerState(Integer stackId);

    List<PortainerStack> listStacks();
}
