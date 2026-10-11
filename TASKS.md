## **Build-a-Recruiter-Bot**

_Backend Developer Assignment - RecruiterFlow Engineering_

### **Welcome**

Hi! Thanks for taking the time to work through this. We built this assignment to feel like an actual day of backend work at RecruiterFlow, not a whiteboard trivia contest - so expect some silliness in the data and real design decisions in the work.

There's no single "correct" solution. We care much more about **how you think and why you built it the way you did** than about matching some hidden reference implementation. You'll get a chance to walk us through your choices afterward, so keep notes on the trade-offs you made as you go.

### **The Scenario**

RecruiterFlow is prototyping **Recruiter Bot** - a small backend service that ingests candidate profiles and job openings, then ranks the best matches for each. To keep grading (and building) fun, our test data isn't real people - it's a cast of well-known fictional characters, each with invented skills and quirks. Somewhere out there, Sherlock Holmes is trying to get a job as a backend detective, and Michael Scott is applying to be a sales engineer. Your job: build the backend that makes those matches happen.

### **Part 1 - The Recruiter Bot (core project)**

###### **Seed data: Candidates**

| Name        | Skills                                                           | Experience (yrs) | Availability | Traits                         | Quirk                                                                                 |
| ----------- | ---------------------------------------------------------------- | ---------------- | ------------ | ------------------------------ | ------------------------------------------------------------------------------------- |
| Sherlock H. | deduction, pattern-recog nition, forensics                       | 8                | Immediate    | analytical, blunt              | Solves problems by eliminating the impossible; occasionally insufferable in standups. |
| Hermione G. | research, time-manage ment, public-speaking                      | 4                | 2 weeks      | detail-oriente d, overachiever | Reads the entire documentatio n before writing a single line of code.                 |
| Tony S.     | systems-desi gn, rapid-prototyp ing, leadership                  | 12               | Not looking  | confident, innovative          | Ships an MVP overnight, refuses to write tests.                                       |
| Leslie K.   | project-mana gement, stakeholder- management, public-speaki ng   | 6                | Immediate    | tenacious, organized           | Has a binder for everything, including the binder.                                    |
| Ron S.      | woodworking, minimalism, negotiation                             | 15               | Not looking  | stubborn, principled           | Refuses to use more than one monitor.                                                 |
| Rick S.     | systems-desi gn, rapid-prototyp ing, chemistry                   | 20               | Immediate    | genius, reckless               | Solution works, mechanism deeply concerning.                                          |
| Elle W.     | persuasion, research, public-speaki ng                           | 3                | Immediate    | optimistic, sharp              | Underestimat ed in every standup, correct in every retro.                             |
| MacGyver    | rapid-prototyp ing, resourcefulne ss, chemistry, systems-desi gn | 10               | 2 weeks      | calm, improviser               | Fixes production outages with duct tape and a paperclip metaphor.                     |
| Sheldon C.  | theoretical-an alysis, pattern-recognition, research             | 9                | Immediate    | rigid, brilliant               | Correct 95% of the time, insufferable 100% of the time.                               |
| Katniss E.  | precision, strategy, crisis-manage ment                          | 5                | Immediate    | resilient, decisive            | Excellent under pressure, terrible with public speaking.                              |
| Michael S.  | sales, public-speaki ng, team-building                           | 11               | Not looking  | enthusiastic, chaotic          | World's best boss, according to a mug he bought himself.                              |
| Olivia P.   | crisis-manage ment, negotiation, strategy, leadership            | 13               | 2 weeks      | decisive, intense              | Handles it. Whatever it is.                                                           |
| Ted L.      | team-building , optimism, mentorship, public-speaki ng           | 7                | Immediate    | empathetic, persistent         | Turns every technical setback into a folksy metaphor.                                 |
| Miranda P.  | leadership, negotiation, stakeholder- management, precision      | 18               | Not looking  | demanding, decisive            | Reviews every PR personally. Says nothing. Everyone panics.                           |
| Dwight S.   | sales, negotiation, security, loyalty                            | 9                | Immediate    | intense, loyal                 | Assistant to the regional backend engineer.                                           |

###### **Seed data: Jobs**

| Title                           | Required Skills                                    | Min Experience | Culture Keywords               | Tagline                                                 |
| ------------------------------- | -------------------------------------------------- | -------------- | ------------------------------ | ------------------------------------------------------- |
| Backend Detective               | deduction, pattern-recogniti on, forensics         | 3              | analytical, autonomous         | We have a bug. We have no leads. We have you.           |
| Rapid Prototyping Engineer      | rapid-prototyping , systems-design                 | 2              | innovative, fast-paced         | Ship first, document never (kidding - please document). |
| Developer Relations Lead        | public-speaking, research                          | 2              | energetic, curious             | Explain complex things to confused humans, cheerfully.  |
| Engineering Manager, Chaos Team | team-building, stakeholder-man agement, leadership | 5              | empathetic, organized          | Herd cats. The cats are senior engineers.               |
| Incident Commander              | crisis-manageme nt, strategy, negotiation          | 4              | decisive, calm-under-pres sure | 3am page. You're the one who picks up.                  |
| Sales Engineer                  | sales, public-speaking, negotiation                | 3              | enthusiastic, persistent       | Sell the vision, then go build it.                      |

###### **What to build**

1. **Ingestion** : a way to load the candidates and jobs above into your own data store (an API endpoint, a seed script - your call).

2. **Your own schema** : design the database schema for candidates, jobs, and however you choose to represent a "match." **Raw SQL only** - no ORM, query builder, or anything that generates SQL for you. Hand-written DDL and hand-written queries, full stop.

3. **Matching API** , at minimum:

   - GET /jobs/{id}/matches - ranked candidates for a job, with a score and a short reason.

   - GET /candidates/{id}/matches - ranked jobs for a candidate (the reverse direction).

4. **The matching algorithm** - this is the heart of the assignment. It _must_ account for:

   - Skill overlap between the candidate and the job's required skills.

   - Experience fit against the job's minimum experience.

Beyond those two, the design is entirely yours: how you weight them, whether/how you use traits, culture keywords, availability, tie-breaking - all up to you. Be ready to explain and defend whatever you chose.

###### **Constraints**

- **Any programming language** . Use whatever you're strongest in.

- **Any relational database** (SQLite, MySQL, Postgres, etc.), but interact with it via **raw, hand-written SQL** - no ORMs or query-builder abstractions.

- No frontend needed. A README with curl examples, or a Postman collection, is enough to demonstrate the API works.

- Tests and API docs are **not required** , but are a nice bonus if you have time left over.

### **Part 2 - The SQL Detective Challenge**

Separately from your project's own schema, here's a small, slightly messy "hiring ops" dataset. Load it as-is (don't redesign it) and write the SQL to answer the questions below. This part is about your raw querying skill, independent of your Part 1 code.

###### **Schema**

```sql
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
   status VARCHAR(20) /*'open', 'closed', 'on_hold'*/
);

CREATE TABLE applicants (
   id INT PRIMARY KEY,
   full_name VARCHAR(100),
   email VARCHAR(100),
   source VARCHAR(50) /*nullable*/,
   applied_date DATE
);

CREATE TABLE interviews (
   id INT PRIMARY KEY,
   applicant_id INT,
   job_posting_id INT,
   stage VARCHAR(20) /*'Screen', 'Technical', 'Final'*/,
   scheduled_date DATE,
   outcome VARCHAR(20) /*'passed', 'failed', 'no_show', NULL (pending)*/
);
```

###### **Seed data**

```sql
INSERT INTO
   recruiters
VALUES
   (1, 'Priya Shah', 'APAC'),
   (2, 'Daniel Ortiz', 'NA'),
   (3, 'Fatima Al-Sayed', 'EMEA'),
   (4, 'Wei Zhang', 'APAC'),
   (5, 'Grace Okafor', 'EMEA');

INSERT INTO
   job_postings
VALUES
   (
      1,
      'Backend Engineer I',
      1,
      'Engineering',
      '2026-01-05',
      'open'
   ),
   (
      2,
      'Backend Engineer I',
      2,
      'Engineering',
      '2026-01-10',
      'open'
   ),
   (
      3,
      'Data Analyst',
      3,
      'Data',
      '2026-01-08',
      'closed'
   ),
   (
      4,
      'Backend Engineer II',
      1,
      'Engineering',
      '2026-02-01',
      'open'
   ),
   (
      5,
      'DevOps Engineer',
      4,
      'Infrastructure',
      '2026-01-20',
      'open'
   ),
   (
      6,
      'Data Analyst',
      5,
      'Data',
      '2026-02-05',
      'on_hold'
   ),
   (
      7,
      'Backend Engineer I',
      2,
      'Engineering',
      '2026-02-15',
      'closed'
   );

INSERT INTO
   applicants
VALUES
   (
      1,
      'Ananya Rao',
      'ananya.rao@mail.com',
      'LinkedIn',
      '2026-01-06'
   ),
   (
      2,
      'Ananya Rao',
      'Ananya.Rao@mail.com',
      'Referral',
      '2026-02-04'
   ),
   (
      3,
      'Ben Turner',
      'ben.turner@mail.com',
      'Job Board',
      '2026-01-07'
   ),
   (
      4,
      'Chloe Martin',
      'chloe.martin@mail.com',
      NULL,
      '2026-01-09'
   ),
   (
      5,
      'Devansh Patel',
      'devansh.patel@mail.com',
      'LinkedIn',
      '2026-01-11'
   ),
   (
      6,
      'Elena Popescu',
      'elena.popescu@mail.com',
      'Referral',
      '2026-01-12'
   ),
   (
      7,
      'Farid Haidari',
      'farid.haidari@mail.com',
      'Job Board',
      '2026-01-13'
   ),
   (
      8,
      'Grace Lin',
      'grace.lin@mail.com',
      'LinkedIn',
      '2026-01-15'
   ),
   (
      9,
      'Hassan Malik',
      'hassan.malik@mail.com',
      NULL,
      '2026-01-16'
   ),
   (
      10,
      'Isla Fraser',
      'isla.fraser@mail.com',
      'Referral',
      '2026-01-18'
   ),
   (
      11,
      'Jonas Weber',
      'jonas.weber@mail.com',
      'Job Board',
      '2026-02-10'
   );

INSERT INTO
   interviews
VALUES
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
```

###### **Questions**

1. List all currently **open** job postings, along with the name of the recruiter who owns each one.

2. For each job posting, how many applicants reached the **Final** interview stage?

3. Find every person who effectively applied to **more than one** job posting. Careful - the data isn't as clean as it looks.

4. For each recruiter, compute their **Final-stage conversion rate** (passed ÷ total Final-stage interviews), but only include recruiters with at least 3 Final-stage interviews.

5. _(Bonus)_ Using a window function, find the recruiter with the most successful placements (passed at Final stage) **per department** .

Submit your answers as a single .sql file (e.g. sql_detective.sql), one query per question, each preceded by a comment noting the question number.

### **What to Submit**

- A **README** in that repo covering:

  - How to install and run it locally.

  - A short (few-paragraph) write-up of the decisions and trade-offs you made - especially in the matching algorithm.

  - A note on where, if anywhere, you used AI tools (ChatGPT, Copilot, etc.) while working on this. This isn't a penalty - we're just curious how you use them, and it helps guide our conversation afterward.

- Your sql_detective.sql file from Part 2, included in the same repo.

### **Questions?**

If anything here is ambiguous, that's partly on purpose - recruiting requirements in the real world are rarely fully specified either, and we're curious how you handle that. But if you're genuinely stuck on something outside the spirit of the exercise, reach out and ask. _Good luck, and have fun with it - Sherlock is counting on you._
