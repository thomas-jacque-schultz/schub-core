package schultz.thomas.schub.core.service;

import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.model.DockerContainerState;

@Service
public interface ContainerRequestService {

    boolean startContainer(Integer stackId);
    boolean stopContainer(Integer stackId);
    DockerContainerState getContainerState(Integer stackId);

    /** Toutes les stacks connues de Portainer, pour en choisir une sans la saisir à la main. */
    java.util.List<schultz.thomas.schub.core.model.PortainerStack> listStacks();
}
