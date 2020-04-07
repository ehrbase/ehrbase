-- concept as json
CREATE OR REPLACE FUNCTION ehr.js_concept(UUID)
  RETURNS JSON AS
$$
DECLARE
  concept_id ALIAS FOR $1;
BEGIN

  IF (concept_id IS NULL) THEN
    RETURN NULL;
  END IF;

  RETURN (
    SELECT ehr.js_dv_coded_text(description, ehr.js_code_phrase(conceptid :: TEXT, 'openehr'))
    FROM ehr.concept
    WHERE id = concept_id AND language = 'en'
  );
END
$$
  LANGUAGE plpgsql;