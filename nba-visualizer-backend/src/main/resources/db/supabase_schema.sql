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
    three_point_pct DOUBLE PRECISION,
    last_updated TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS team_season_stats (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    season INTEGER NOT NULL,
    pts DOUBLE PRECISION,
    reb DOUBLE PRECISION,
    ast DOUBLE PRECISION,
    stl DOUBLE PRECISION,
    blk DOUBLE PRECISION,
    plus_minus DOUBLE PRECISION,
    pts_rank INTEGER,
    reb_rank INTEGER,
    ast_rank INTEGER,
    stl_rank INTEGER,
    blk_rank INTEGER,
    plus_minus_rank INTEGER,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, season)
);

CREATE TABLE IF NOT EXISTS team_roster_players (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    season INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    full_name TEXT,
    first_name TEXT,
    last_name TEXT,
    position TEXT,
    jersey TEXT,
    height TEXT,
    weight TEXT,
    salary TEXT,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, season, player_id)
);

ALTER TABLE team_roster_players ADD COLUMN IF NOT EXISTS salary TEXT;

CREATE TABLE IF NOT EXISTS team_player_stats (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    season INTEGER NOT NULL,
    player_id INTEGER NOT NULL,
    player_name TEXT,
    pts DOUBLE PRECISION,
    reb DOUBLE PRECISION,
    ast DOUBLE PRECISION,
    stl DOUBLE PRECISION,
    blk DOUBLE PRECISION,
    plus_minus DOUBLE PRECISION,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, season, player_id)
);

CREATE TABLE IF NOT EXISTS team_games (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    season INTEGER NOT NULL,
    game_id TEXT NOT NULL,
    game_date DATE,
    matchup TEXT,
    opponent_abbreviation TEXT,
    opponent_name TEXT,
    location TEXT,
    result_type TEXT,
    team_score INTEGER,
    opponent_score INTEGER,
    record TEXT,
    completed BOOLEAN,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, season, game_id)
);

CREATE TABLE IF NOT EXISTS team_depth_charts (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    season INTEGER NOT NULL,
    position TEXT NOT NULL,
    depth_order INTEGER NOT NULL,
    player_id INTEGER,
    player_name TEXT,
    status TEXT,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, season, position, depth_order)
);

ALTER TABLE team_depth_charts ADD COLUMN IF NOT EXISTS status TEXT;

CREATE TABLE IF NOT EXISTS team_injuries (
    team_id INTEGER NOT NULL REFERENCES teams(id),
    player_name TEXT NOT NULL,
    position TEXT,
    injury TEXT,
    expected_return TEXT,
    status TEXT,
    last_updated TIMESTAMPTZ,
    PRIMARY KEY (team_id, player_name)
);

ALTER TABLE team_injuries ADD COLUMN IF NOT EXISTS position TEXT;
