/*
	Build the containment table from ehr.entry
	C. Chevalley 18/04/16
*/
CREATE OR REPLACE FUNCTION _aql_labelize(raw_archetype text) RETURNS text AS $$

BEGIN
	raw_archetype := split_part(raw_archetype,'and name/value=', 1);
	raw_archetype := replace(raw_archetype, '-', '_');
	raw_archetype := replace(raw_archetype, '.', '_');
	raw_archetype := trim(trailing ' ' from raw_archetype);

	return raw_archetype;  
END

$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION build_containments() RETURNS integer AS $$
DECLARE
	entry_rec record;
	path_segment text;
	entry_cnt int;
	compositionId varchar;
	path_expression varchar;
	archetypeId text;
	segment text;
	segment_position int;
	segment_length int;
	ltree_expression text;
	labels ltree;
	path text;
	vartext text;
	counter int;
	root_path text;
	
BEGIN
	SELECT count(*) INTO entry_cnt FROM ehr.entry;
	if (entry_cnt > 0) then
	else
		RAISE NOTICE 'No valid entries found';
		return 0;
	end if;

	CREATE TEMPORARY TABLE IF NOT EXISTS contain_temp(comp_id UUID, label ltree, path text);

	FOR entry_rec IN SELECT composition_id, archetype_id, entry::text AS content FROM ehr.entry  LOOP
		-- insert the root for completness
		-- root_path := '/composition['||entry_rec.archetype_id||']';
		-- INSERT INTO ehr.containment (comp_id, "label", path) VALUES (entry_rec.composition_id, text2ltree(_aql_labelize(entry_rec.archetype_id)), root_path);
	
		segment_position := strpos(entry_rec.content, '/composition[')+13; 
		compositionId := substr(entry_rec.content, segment_position);
		segment_position := strpos(compositionId, ']');
		compositionId := substr(compositionId, 0, segment_position);
		root_path := '/composition['||compositionId||']';
		INSERT INTO ehr.containment (comp_id, "label", path) VALUES (entry_rec.composition_id, text2ltree(_aql_labelize(entry_rec.archetype_id)), root_path);
		 
		compositionId := _aql_labelize(compositionId);

		RAISE NOTICE 'Composition: %', compositionId;

		-- get the path

		FOR path_segment IN SELECT regexp_split_to_table(entry_rec.content, E'/\\$PATH\\$') LOOP
			/* ignore the first item in in list */
			CONTINUE when strpos(path_segment, '/composition') > 0; -- ignore the first element in table
			
			path_expression := substr(path_segment, 5);
			segment_position := strpos(path_expression, ',') - 1;
			path_expression := substr(path_expression, 0, segment_position);
			--RAISE NOTICE '---- PATH: %', path_expression;

			-- split the path, keep only the archetype Ids
			ltree_expression := compositionId;
			
			counter := 0;
		
			FOR segment IN SELECT regexp_split_to_table(path_expression, '\]/')
			LOOP
				if (strpos(segment, 'openEHR') > 0) then
					archetypeId := substr(segment, strpos(segment, '[')+1);
					segment_position := strpos(path_expression, archetypeId)+length(archetypeId)+1;
					
					archetypeId := _aql_labelize(archetypeId);
					
					path := substr(path_expression, 0, segment_position);
					path := trim(trailing ' ' from path);
					-- path := path || ']';
					
					if (length(ltree_expression) > 0) then	
						ltree_expression := ltree_expression || '.' || archetypeId;
					else
						ltree_expression := archetypeId;
					end if;

					RAISE NOTICE 'id: % ltree: % path:%', entry_rec.composition_id, ltree_expression, path;
					labels := text2ltree(ltree_expression);
					INSERT INTO contain_temp (comp_id, "label", path) VALUES (entry_rec.composition_id, labels, root_path||path);
				end if;
			END LOOP;
		END LOOP;
		-- wrap up this iteration
		INSERT INTO ehr.containment SELECT DISTINCT comp_id, contain_temp.label, contain_temp.path FROM contain_temp;
		DELETE FROM contain_temp;
		
	END LOOP;

	RETURN 1;
END
$$ LANGUAGE plpgsql;

SELECT build_containments();

DROP FUNCTION _aql_resolve_path(uuid, text);

CREATE OR REPLACE FUNCTION _aql_resolve_path(id UUID, archetype_id text) RETURNS text AS $$
DECLARE
	label_query lquery;
BEGIN
	
	RETURN (SELECT path FROM ehr.containment WHERE containment.comp_id = id AND label ~ ('COMPOSITION%.*.'||archetype_id::text)::lquery LIMIT 1);
END
$$ LANGUAGE plpgsql;

SELECT _aql_resolve_path('064f266c-8f5a-453a-a17d-3acfe4fd89ab','openEHR_EHR_EVALUATION_problem_diagnosis_v1'); 

DELETE FROM ehr.containment;

DROP TABLE ehr.containment;

create TABLE ethercis.ehr.containment (
 comp_id UUID,
 label ltree,
 path text
);