CREATE TABLE IF NOT EXISTS teams (
    id INTEGER PRIMARY KEY,
    full_name TEXT,
    abbreviation TEXT,
    city TEXT,
    conference TEXT,
    division TEXT,
    name TEXT,
    logo_path TEXT,
    last_metadata_update TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY,
    team_id INTEGER REFERENCES teams(id),
    first_name TEXT,
    last_name TEXT,
    position TEXT,
    height TEXT,
    weight TEXT,
    last_metadata_update TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS standings (
    season INTEGER NOT NULL,
    team_id INTEGER NOT NULL REFERENCES teams(id),
    team_name TEXT,
    team_abbr TEXT,
    conference TEXT,
    division TEXT,
    wins INTEGER,
    losses INTEGER,
    win_percentage DOUBLE PRECISION,
    conference_rank INTEGER,
    last_ten_wins INTEGER,
    last_ten_losses INTEGER,
    win_streak INTEGER,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (season, team_id)
);

CREATE TABLE IF NOT EXISTS player_season_stats (
    player_id INTEGER PRIMARY KEY REFERENCES players(id),
    pts DOUBLE PRECISION,
    reb DOUBLE PRECISION,
    ast DOUBLE PRECISION,
    stl DOUBLE PRECISION,
    blk DOUBLE PRECISION,
    fg_pct DOUBLE PRECISION,
    three_p_pct DOUBLE PRECISION,
    last_updated TIMESTAMPTZ
);
