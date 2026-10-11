CREATE TABLE api_usage_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    completed_at_ms INTEGER NOT NULL,
    request_id TEXT,
    http_method TEXT NOT NULL,
    route_template TEXT NOT NULL,
    status_code INTEGER NOT NULL CHECK (status_code BETWEEN 100 AND 599),
    duration_ms INTEGER NOT NULL CHECK (duration_ms >= 0)
);
CREATE INDEX idx_api_usage_events_completed_at ON api_usage_events(completed_at_ms);
CREATE INDEX idx_api_usage_events_route_completed_at ON api_usage_events(route_template, completed_at_ms);
