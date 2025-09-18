CREATE INDEX IF NOT EXISTS idx_rate_cc_date_version
    ON exchange_rate (currency_code, valid_date, version DESC);

CREATE INDEX IF NOT EXISTS idx_mv_tags_movement ON movement_tags (movement_id);
CREATE INDEX IF NOT EXISTS idx_mv_tags_tag ON movement_tags (tag_id);
