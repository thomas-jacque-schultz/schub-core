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

    /** Toutes les stacks connues de Portainer, pour en choisir une sans la saisir à la main. */
    List<PortainerStack> listStacks();
}
