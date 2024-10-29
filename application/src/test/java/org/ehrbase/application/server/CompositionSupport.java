package org.ehrbase.application.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;

import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestCompositionEndpoint;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.junit.jupiter.api.Assertions;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;

public class CompositionSupport {

	private final CanonicalJson json = new CanonicalJson();
	private final Function<UUID,DefaultRestCompositionEndpoint> endpoint;
	
	private static String compo;
	
	static {
		URL url = CompositionSupport.class.getResource("composition.json");
		Path path = Paths.get(URI.create(url.toString()));
		try {
			compo = Files.readString(path);
		} catch (IOException e) {
			Assertions.fail();
		}
	}
	
	public CompositionSupport(OpenEhrClientConfig cfg) {
		endpoint = uuid -> new DefaultRestCompositionEndpoint(new DefaultRestClient(cfg), uuid);
	}
	
	public ObjectVersionId create(UUID ehrId) {
		Composition composition = json.unmarshal(compo, Composition.class);
		ObjectVersionId versionId = endpoint.apply(ehrId).mergeRaw(composition);
		return versionId;
	}
}
