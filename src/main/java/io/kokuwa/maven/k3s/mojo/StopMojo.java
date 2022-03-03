package io.kokuwa.maven.k3s.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.kokuwa.maven.k3s.K3sMojo;
import lombok.Setter;

/**
 * Mojo for stopping k3s.
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresProject = false)
public class StopMojo extends K3sMojo {

	/** Skip stoppping of k3s container. */
	@Setter @Parameter(property = "k3s.skipStop", defaultValue = "false")
	private boolean skipStop = false;

	@Override
	public void execute() throws MojoExecutionException {

		if (isSkip(skipStop)) {
			return;
		}

		// get container id

		var optionalContainerId = dockerUtil().getContainerId();
		if (optionalContainerId.isEmpty()) {
			log.info("Container not found, skip stop");
			return;
		}
		var containerId = optionalContainerId.get();

		// stop container

		if (dockerUtil().isRunning(containerId)) {
			dockerClient().stopContainerCmd(containerId).exec();
			log.info("Container with id '{}' stopped", containerId);
		}
	}
}
