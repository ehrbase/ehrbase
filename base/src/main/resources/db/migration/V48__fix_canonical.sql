-- missing type...
CREATE OR REPLACE FUNCTION ehr.js_party_ref(TEXT, TEXT, TEXT, TEXT)
    RETURNS JSON AS
$$
DECLARE
    id_value ALIAS FOR $1;
    id_scheme ALIAS FOR $2;
    namespace ALIAS FOR $3;
    party_type ALIAS FOR $4;
BEGIN

    IF (id_value IS NULL AND id_scheme IS NULL AND namespace IS NULL AND party_type IS NULL) THEN
        RETURN NULL;
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_REF',
                    'id',
                    json_build_object(
                            '_type', 'GENERIC_ID',
                            'value', id_value,
                            'scheme', id_scheme
                        ),
                    'namespace', namespace,
                    'type', party_type
                );
    END IF;
END
$$
    LANGUAGE plpgsql;


-- fix wrong encoding of code_phrase
CREATE OR REPLACE FUNCTION ehr.js_dv_coded_text(dvcodedtext ehr.dv_coded_text)
    RETURNS JSON AS
$$
BEGIN
    RETURN
        jsonb_strip_nulls(
                jsonb_build_object(
                        '_type', (SELECT (
                                             CASE
                                                 WHEN ((dvcodedtext).defining_code IS NOT NULL)
                                                     THEN
                                                     'DV_CODED_TEXT'
                                                 ELSE
                                                     'DV_TEXT'
                                                 END
                                             )
                ),
                        'value', dvcodedtext.value,
                        'defining_code', ehr.js_code_phrase(dvcodedtext.defining_code),
                        'formatting', dvcodedtext.formatting,
                        'language', dvcodedtext.language,
                        'encoding', dvcodedtext.encoding,
                        'mappings', ehr.js_term_mappings(dvcodedtext.term_mapping)
                    )
            );
END
$$
    LANGUAGE plpgsql;