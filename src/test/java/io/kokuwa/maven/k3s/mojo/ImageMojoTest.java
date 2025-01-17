package io.kokuwa.maven.k3s.mojo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.maven.k3s.AbstractTest;
import lombok.SneakyThrows;

@DisplayName("mojo: import")
public class ImageMojoTest extends AbstractTest {

	private static final String HELLO_WORLD = "hello-world:linux";

	@DisplayName("without images")
	@Test
	void withoutImages(ImageMojo mojo) {
		assertDoesNotThrow(() -> mojo
				.setCtrImages(List.of())
				.setTarFiles(List.of())
				.setDockerImages(List.of())
				.execute());
	}

	@DisplayName("with skip")
	@Test
	void withSkip(ImageMojo mojo) {
		assertDoesNotThrow(() -> mojo
				.setSkipImage(true)
				.setCtrImages(List.of(HELLO_WORLD))
				.setTarFiles(List.of(HELLO_WORLD))
				.setDockerImages(List.of(HELLO_WORLD))
				.execute());
	}

	@DisplayName("with images")
	@Test
	void withImages(CreateMojo create, StartMojo start, ImageMojo mojo, RemoveMojo remove) {
		assertDoesNotThrow(create::execute);
		assertDoesNotThrow(start::execute);
		docker.removeImage(HELLO_WORLD);

		assertCtrPull(mojo);
		assertTagFiles(mojo);
		assertDockerWithoutImage(mojo);
		assertDockerWithCachedImage(mojo);

		assertDoesNotThrow(remove::execute);
	}

	// test

	private void assertCtrPull(ImageMojo mojo) {

		removeCtrImage(HELLO_WORLD);
		mojo.setCtrImages(List.of(HELLO_WORLD)).setTarFiles(List.of()).setDockerImages(List.of());

		assertFalse(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, false);
		assertDoesNotThrow(mojo::execute);
		assertFalse(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, true);
	}

	private void assertTagFiles(ImageMojo mojo) {

		removeCtrImage(HELLO_WORLD);
		mojo.setCtrImages(List.of())
				.setTarFiles(List.of("src/test/resources/hello-world.tar"))
				.setDockerImages(List.of());

		assertFalse(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, false);
		assertDoesNotThrow(mojo::execute);
		assertFalse(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, true);
	}

	private void assertDockerWithCachedImage(ImageMojo mojo) {

		removeCtrImage(HELLO_WORLD);
		mojo.setCtrImages(List.of()).setTarFiles(List.of()).setDockerImages(List.of(HELLO_WORLD));

		assertTrue(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, false);
		assertDoesNotThrow(mojo::execute);
		assertTrue(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, true);
	}

	private void assertDockerWithoutImage(ImageMojo mojo) {

		docker.removeImage(HELLO_WORLD);
		removeCtrImage(HELLO_WORLD);
		mojo.setCtrImages(List.of()).setTarFiles(List.of()).setDockerImages(List.of(HELLO_WORLD));

		assertFalse(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, false);
		assertDoesNotThrow(mojo::execute);
		assertTrue(hasDockerImage(HELLO_WORLD));
		assertCtrImage(HELLO_WORLD, true);
	}

	// internal

	@SneakyThrows
	private void assertCtrImage(String image, boolean exists) {
		var container = docker.getContainer().get();
		var result = docker.execThrows(container, "ctr image list --quiet", Duration.ofSeconds(30));
		var output = result.getMessages().stream().collect(Collectors.joining());
		var images = List.of(output.split("\n"));
		var normalizedImage = docker.normalizeDockerImage(image);
		assertEquals(exists, images.contains(docker.normalizeDockerImage(image)),
				"Image '" + normalizedImage + "' " + (exists ? "not " : "") + "found, available: \n" + output);
	}

	@SneakyThrows
	private boolean hasDockerImage(String image) {
		return docker.findImage(image).isPresent();
	}

	@SneakyThrows
	private void removeCtrImage(String image) {
		docker.execThrows(
				docker.getContainer().get(),
				"ctr image remove " + docker.normalizeDockerImage(image),
				Duration.ofMinutes(1));
	}
}
