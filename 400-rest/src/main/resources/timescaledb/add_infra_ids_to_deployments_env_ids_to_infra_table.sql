-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
ALTER TABLE CG_INFRA_DEFINITION ADD COLUMN IF NOT EXISTS ENV_ID TEXT;

ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS INFRA_DEFINITIONS TEXT[];
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS INFRA_MAPPINGS TEXT[];

ALTER TABLE DEPLOYMENT_STAGE ADD COLUMN IF NOT EXISTS INFRA_DEFINITIONS TEXT[];
ALTER TABLE DEPLOYMENT_STAGE ADD COLUMN IF NOT EXISTS INFRA_MAPPINGS TEXT[];

ALTER TABLE DEPLOYMENT_PARENT ADD COLUMN IF NOT EXISTS INFRA_DEFINITIONS TEXT[];
ALTER TABLE DEPLOYMENT_PARENT ADD COLUMN IF NOT EXISTS INFRA_MAPPINGS TEXT[];

CREATE INDEX IF NOT EXISTS DEPLOYMENTS_INFRA_DEFINITIONS_GIN_INDEX ON DEPLOYMENT(infra_definitions);
CREATE INDEX IF NOT EXISTS DEPLOYMENTS_INFRA_MAPPINGS_GIN_INDEX ON DEPLOYMENT(infra_mappings);

CREATE INDEX IF NOT EXISTS DEPLOYMENT_PARENT_INFRA_DEFINITIONS_GIN_INDEX ON DEPLOYMENT_PARENT(infra_definitions);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_PARENT_INFRA_MAPPINGS_GIN_INDEX ON DEPLOYMENT_PARENT(infra_mappings);

CREATE INDEX IF NOT EXISTS DEPLOYMENT_STAGE_INFRA_DEFINITIONS_GIN_INDEX ON DEPLOYMENT_STAGE(infra_definitions);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_STAGE_INFRA_MAPPINGS_GIN_INDEX ON DEPLOYMENT_STAGE(infra_mappings);
COMMIT ;