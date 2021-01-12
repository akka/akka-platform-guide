--DROP TABLE IF EXISTS public.item_popularity;

-- tag::create[]
CREATE TABLE IF NOT EXISTS public.item_popularity (
    itemid VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    count BIGINT NOT NULL,
    PRIMARY KEY (itemid));
-- end::create[]