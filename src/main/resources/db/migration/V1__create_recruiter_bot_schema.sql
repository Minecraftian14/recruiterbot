CREATE TABLE candidates (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    years_experience INTEGER NOT NULL CHECK (years_experience >= 0),
    availability TEXT NOT NULL CHECK (availability IN ('IMMEDIATE', 'TWO_WEEKS', 'NOT_LOOKING')),
    quirk TEXT NOT NULL
);

CREATE TABLE candidate_skills (
    candidate_id INTEGER NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    skill TEXT NOT NULL CHECK (skill = lower(trim(skill))),
    PRIMARY KEY (candidate_id, skill)
);
CREATE INDEX idx_candidate_skills_skill ON candidate_skills(skill, candidate_id);

CREATE TABLE candidate_traits (
    candidate_id INTEGER NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    trait TEXT NOT NULL CHECK (trait = lower(trim(trait))),
    PRIMARY KEY (candidate_id, trait)
);
CREATE INDEX idx_candidate_traits_trait ON candidate_traits(trait, candidate_id);

CREATE TABLE jobs (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    minimum_experience INTEGER NOT NULL CHECK (minimum_experience >= 0),
    tagline TEXT NOT NULL
);

CREATE TABLE job_required_skills (
    job_id INTEGER NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill TEXT NOT NULL CHECK (skill = lower(trim(skill))),
    PRIMARY KEY (job_id, skill)
);
CREATE INDEX idx_job_required_skills_skill ON job_required_skills(skill, job_id);

CREATE TABLE job_culture_keywords (
    job_id INTEGER NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    keyword TEXT NOT NULL CHECK (keyword = lower(trim(keyword))),
    PRIMARY KEY (job_id, keyword)
);
CREATE INDEX idx_job_culture_keywords_keyword ON job_culture_keywords(keyword, job_id);
