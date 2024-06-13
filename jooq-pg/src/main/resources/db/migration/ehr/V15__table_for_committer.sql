/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific LANGUAGE governing permissions and
 * limitations under the License.
 */

ALTER TABLE users ADD COLUMN committer_id uuid NOT NULL DEFAULT uuid_generate_v4();
ALTER TABLE users ALTER COLUMN committer_id DROP DEFAULT;
ALTER TABLE audit_details ADD COLUMN committer_id uuid;

CREATE TABLE committer
(
    id          uuid        NOT NULL,
    data        jsonb       NOT NULL,
    audit_ids   uuid[],

    PRIMARY KEY (id)
);
CREATE INDEX committer_data_idx ON committer USING hash ((data::text));


INSERT INTO committer (id,data)
    SELECT committer_id ,
           jsonb_insert(
           jsonb_insert(
           jsonb_insert(
                   '{
                        "T" : "PI",
                        "er" : {
                          "T" : "PF",
                          "ns" : "User",
                          "tp" : "PARTY",
                          "X" : {
                            "T" : "GX",
                            "sc" : "DEMOGRAPHIC"
                          }
                        },
                        "Xs" : [{
                          "T" : "id",
                          "is" : "EHRbase",
                          "as" : "EHRbase",
                          "tp" : "EHRbase Security Authentication User"
                        }]
                   }'::jsonb,
            '{N}', to_jsonb('EHRbase Internal '|| username)),
            '{er,X,V}', to_jsonb(id)),
            '{Xs,0,X}', to_jsonb(username))
    FROM users;


INSERT INTO committer (id, data, audit_ids)
    SELECT uuid_generate_v4(), jsonb_with_aliased_keys_and_types(a.committer), array_agg(a.id)
    FROM audit_details a
    WHERE a.committer IS NOT NULL
    GROUP BY a.committer;


UPDATE audit_details a SET committer_id = (SELECT u.committer_id FROM users u WHERE u.id=a.user_id) WHERE committer IS NULL;
UPDATE audit_details a  SET committer_id = c.id FROM (SELECT id, UNNEST(audit_ids) as audit_id FROM committer) as c WHERE a.id = c.audit_id;
ALTER TABLE audit_details ALTER COLUMN committer_id SET NOT NULL;

ALTER TABLE committer DROP COLUMN audit_ids;
ALTER TABLE users ADD FOREIGN KEY (committer_id) REFERENCES committer(id);
ALTER TABLE audit_details ADD FOREIGN KEY (committer_id) REFERENCES committer(id);
ALTER TABLE audit_details DROP COLUMN committer;
