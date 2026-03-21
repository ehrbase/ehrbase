package org.ehrbase.schemagen;

import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.schemagen.enums.TemplateFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserServiceTest {

    private final TemplateParserService parser = new TemplateParserService();

    @Test
    void parseMinimalObservation_returnsWebTemplate() throws IOException {
        String xml = loadTemplate("templates/minimal_observation.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.XML);

        assertThat(wt).isNotNull();
        assertThat(wt.getTemplateId()).isNotBlank();

        WebTemplateNode root = wt.getTree();
        assertThat(root).isNotNull();
        assertThat(root.getRmType()).isEqualTo("COMPOSITION");
        assertThat(root.getChildren()).isNotEmpty();
    }

    @Test
    void parseBloodPressure_returnsWebTemplateWithObservation() throws IOException {
        String xml = loadTemplate("templates/ehrbase_blood_pressure_simple.de.v0.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.XML);

        assertThat(wt).isNotNull();
        assertThat(wt.getTemplateId()).contains("blood_pressure");
    }

    @Test
    void parseInvalidXml_throwsIllegalArgument() {
        assertThatThrownBy(() -> parser.parse("<invalid>xml", TemplateFormat.XML))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OPT XML");
    }

    @Test
    void parseAdlText_throwsUnsupported() {
        assertThatThrownBy(() -> parser.parse("archetype text", TemplateFormat.ADL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void parseJson_throwsUnsupported() {
        assertThatThrownBy(() -> parser.parse("{}", TemplateFormat.JSON))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Phase 11");
    }

    @Test
    void parseOptFormat_sameAsXml() throws IOException {
        String xml = loadTemplate("templates/minimal_observation.opt");
        WebTemplate wt = parser.parse(xml, TemplateFormat.OPT);
        assertThat(wt).isNotNull();
        assertThat(wt.getTree().getRmType()).isEqualTo("COMPOSITION");
    }

    private String loadTemplate(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Template not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
