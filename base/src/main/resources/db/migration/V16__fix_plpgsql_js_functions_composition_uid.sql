create or replace function composition_uid(composition_uid uuid, server_id text) returns text
    language plpgsql
as
$$
BEGIN
    RETURN (select "composition"."id" || '::' || server_id || '::' || 1
        + COALESCE(
                                                                              (select count(*)
                                                                               from "ehr"."composition_history"
                                                                               where "composition_history"."id" = composition_uid)
                                                                          , 0)
            from ehr.composition
            where composition.id = composition_uid);
END
$$;

-- alter function composition_uid(uuid, text) owner to ehrbase;