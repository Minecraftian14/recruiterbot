-- Hiring operations challenge fixture. These four tables are intentionally independent
-- from the Recruiter Bot application's candidates/jobs schema.
CREATE TABLE recruiters (
   id INT PRIMARY KEY,
   name VARCHAR(100),
   region VARCHAR(50)
);

CREATE TABLE job_postings (
   id INT PRIMARY KEY,
   title VARCHAR(100),
   recruiter_id INT,
   department VARCHAR(50),
   opened_date DATE,
   status VARCHAR(20)
);

CREATE TABLE applicants (
   id INT PRIMARY KEY,
   full_name VARCHAR(100),
   email VARCHAR(100),
   source VARCHAR(50),
   applied_date DATE
);

CREATE TABLE interviews (
   id INT PRIMARY KEY,
   applicant_id INT,
   job_posting_id INT,
   stage VARCHAR(20),
   scheduled_date DATE,
   outcome VARCHAR(20)
);

INSERT INTO recruiters VALUES
   (1, 'Priya Shah', 'APAC'),
   (2, 'Daniel Ortiz', 'NA'),
   (3, 'Fatima Al-Sayed', 'EMEA'),
   (4, 'Wei Zhang', 'APAC'),
   (5, 'Grace Okafor', 'EMEA');

INSERT INTO job_postings VALUES
   (1, 'Backend Engineer I', 1, 'Engineering', '2026-01-05', 'open'),
   (2, 'Backend Engineer I', 2, 'Engineering', '2026-01-10', 'open'),
   (3, 'Data Analyst', 3, 'Data', '2026-01-08', 'closed'),
   (4, 'Backend Engineer II', 1, 'Engineering', '2026-02-01', 'open'),
   (5, 'DevOps Engineer', 4, 'Infrastructure', '2026-01-20', 'open'),
   (6, 'Data Analyst', 5, 'Data', '2026-02-05', 'on_hold'),
   (7, 'Backend Engineer I', 2, 'Engineering', '2026-02-15', 'closed');

INSERT INTO applicants VALUES
   (1, 'Ananya Rao', 'ananya.rao@mail.com', 'LinkedIn', '2026-01-06'),
   (2, 'Ananya Rao', 'Ananya.Rao@mail.com', 'Referral', '2026-02-04'),
   (3, 'Ben Turner', 'ben.turner@mail.com', 'Job Board', '2026-01-07'),
   (4, 'Chloe Martin', 'chloe.martin@mail.com', NULL, '2026-01-09'),
   (5, 'Devansh Patel', 'devansh.patel@mail.com', 'LinkedIn', '2026-01-11'),
   (6, 'Elena Popescu', 'elena.popescu@mail.com', 'Referral', '2026-01-12'),
   (7, 'Farid Haidari', 'farid.haidari@mail.com', 'Job Board', '2026-01-13'),
   (8, 'Grace Lin', 'grace.lin@mail.com', 'LinkedIn', '2026-01-15'),
   (9, 'Hassan Malik', 'hassan.malik@mail.com', NULL, '2026-01-16'),
   (10, 'Isla Fraser', 'isla.fraser@mail.com', 'Referral', '2026-01-18'),
   (11, 'Jonas Weber', 'jonas.weber@mail.com', 'Job Board', '2026-02-10');

INSERT INTO interviews VALUES
   (1, 1, 1, 'Screen', '2026-01-10', 'passed'),
   (2, 1, 1, 'Technical', '2026-01-15', 'passed'),
   (3, 1, 1, 'Final', '2026-01-22', 'passed'),
   (4, 2, 4, 'Screen', '2026-02-05', 'passed'),
   (5, 3, 1, 'Screen', '2026-01-11', 'passed'),
   (6, 3, 1, 'Technical', '2026-01-16', 'failed'),
   (7, 3, 3, 'Final', '2026-01-28', 'passed'),
   (8, 4, 1, 'Screen', '2026-01-12', 'passed'),
   (9, 4, 1, 'Final', '2026-01-24', 'failed'),
   (10, 5, 2, 'Screen', '2026-01-14', 'passed'),
   (11, 5, 2, 'Technical', '2026-01-19', 'passed'),
   (12, 5, 2, 'Final', '2026-01-25', 'passed'),
   (13, 6, 2, 'Screen', '2026-01-15', 'passed'),
   (14, 6, 2, 'Final', '2026-01-26', 'failed'),
   (15, 7, 2, 'Screen', '2026-01-16', 'no_show'),
   (16, 8, 4, 'Screen', '2026-02-03', 'passed'),
   (17, 8, 4, 'Technical', '2026-02-08', 'passed'),
   (18, 8, 4, 'Final', '2026-02-12', 'passed'),
   (19, 9, 4, 'Screen', '2026-02-04', 'passed'),
   (20, 9, 4, 'Final', '2026-02-14', 'passed'),
   (21, 10, 5, 'Screen', '2026-01-22', 'passed'),
   (22, 10, 5, 'Technical', '2026-01-28', NULL),
   (23, 11, 7, 'Final', '2026-02-20', 'passed'),
   (24, 6, 7, 'Final', '2026-02-21', 'failed');

-- Question 1: Open job postings and their recruiter.
SELECT jp.id, jp.title, r.name AS recruiter_name
FROM job_postings AS jp
JOIN recruiters AS r ON r.id = jp.recruiter_id
WHERE jp.status = 'open'
ORDER BY jp.id;

-- Question 2: Applicants who reached Final for each posting (including zero-count postings).
SELECT jp.id, jp.title, COUNT(DISTINCT i.applicant_id) AS final_applicant_count
FROM job_postings AS jp
LEFT JOIN interviews AS i
  ON i.job_posting_id = jp.id AND i.stage = 'Final'
GROUP BY jp.id, jp.title
ORDER BY jp.id;

-- Question 3: Effective people with applications to multiple distinct postings.
-- Email identity is trimmed and case-folded. Distinct posting IDs prevent multiple
-- interview stages for one applicant/job from inflating the count.
WITH effective_applications AS (
  SELECT lower(trim(a.email)) AS normalized_email,
         MIN(a.full_name) AS full_name,
         i.job_posting_id
  FROM applicants AS a
  JOIN interviews AS i ON i.applicant_id = a.id
  GROUP BY lower(trim(a.email)), i.job_posting_id
)
SELECT normalized_email AS email,
       MIN(full_name) AS full_name,
       COUNT(DISTINCT job_posting_id) AS distinct_job_postings
FROM effective_applications
GROUP BY normalized_email
HAVING COUNT(DISTINCT job_posting_id) > 1
ORDER BY normalized_email;

-- Question 4: Recruiter Final-stage conversion, restricted to at least three interviews.
SELECT r.id AS recruiter_id,
       r.name AS recruiter_name,
       COUNT(*) AS final_interviews,
       SUM(CASE WHEN i.outcome = 'passed' THEN 1 ELSE 0 END) AS final_passes,
       100.0 * SUM(CASE WHEN i.outcome = 'passed' THEN 1 ELSE 0 END) / COUNT(*) AS conversion_rate_percent
FROM recruiters AS r
JOIN job_postings AS jp ON jp.recruiter_id = r.id
JOIN interviews AS i ON i.job_posting_id = jp.id AND i.stage = 'Final'
GROUP BY r.id, r.name
HAVING COUNT(*) >= 3
ORDER BY r.id;

-- Question 5 (bonus): Recruiter(s) with the most Final-stage passes per department.
-- RANK preserves ties within each department.
WITH placement_counts AS (
  SELECT jp.department,
         r.id AS recruiter_id,
         r.name AS recruiter_name,
         COUNT(DISTINCT CASE WHEN i.id IS NOT NULL
           THEN CAST(i.applicant_id AS TEXT) || ':' || CAST(i.job_posting_id AS TEXT)
         END) AS successful_placements
  FROM job_postings AS jp
  JOIN recruiters AS r ON r.id = jp.recruiter_id
  LEFT JOIN interviews AS i
    ON i.job_posting_id = jp.id AND i.stage = 'Final' AND i.outcome = 'passed'
  GROUP BY jp.department, r.id, r.name
), ranked_recruiters AS (
  SELECT department,
         recruiter_id,
         recruiter_name,
         successful_placements,
         RANK() OVER (PARTITION BY department ORDER BY successful_placements DESC) AS department_rank
  FROM placement_counts
)
SELECT department, recruiter_id, recruiter_name, successful_placements
FROM ranked_recruiters
WHERE department_rank = 1
ORDER BY department, recruiter_id;
