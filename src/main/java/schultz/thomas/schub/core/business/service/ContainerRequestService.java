package schultz.thomas.schub.core.business.service;

import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.DockerContainerState;
import schultz.thomas.schub.core.business.model.PortainerStack;

import java.util.List;

@Service
public interface ContainerRequestService {

    boolean startContainer(Integer stackId);
    boolean stopContainer(Integer stackId);
    DockerContainerState getContainerState(Integer stackId);

    List<PortainerStack> listStacks();
}
