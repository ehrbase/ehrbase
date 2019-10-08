-- Create table ehr.stored_query

CREATE TABLE ehr.stored_query
(
  -- check for a syntactically valid reverse domain name (https://en.wikipedia.org/wiki/Reverse_domain_name_notation)
  reverse_domain_name VARCHAR NOT NULL
    CHECK (reverse_domain_name ~* '^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$'),
  -- match a string consisting of alphanumeric or '-' or '_'
  semantic_id VARCHAR NOT NULL
    CHECK (semantic_id ~* '[\w|\-|_|]+'),
  -- match a valid SEMVER (from https://semver.org/)
  semver VARCHAR DEFAULT '0.0.0'
    CHECK (semver ~*
           '^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$' ),
  query_text VARCHAR NOT NULL,
  creation_date TIMESTAMP default CURRENT_TIMESTAMP,
  type VARCHAR DEFAULT 'AQL',
  CONSTRAINT pk_qualified_name PRIMARY KEY (reverse_domain_name, semantic_id, semver)
)
