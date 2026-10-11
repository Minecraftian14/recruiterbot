INSERT INTO candidates (id, name, years_experience, availability, quirk) VALUES
(1, 'Sherlock H.', 8, 'IMMEDIATE', 'Solves problems by eliminating the impossible; occasionally insufferable in standups.'),
(2, 'Hermione G.', 4, 'TWO_WEEKS', 'Reads the entire documentation before writing a single line of code.'),
(3, 'Tony S.', 12, 'NOT_LOOKING', 'Ships an MVP overnight, refuses to write tests.'),
(4, 'Leslie K.', 6, 'IMMEDIATE', 'Has a binder for everything, including the binder.'),
(5, 'Ron S.', 15, 'NOT_LOOKING', 'Refuses to use more than one monitor.'),
(6, 'Rick S.', 20, 'IMMEDIATE', 'Solution works, mechanism deeply concerning.'),
(7, 'Elle W.', 3, 'IMMEDIATE', 'Underestimated in every standup, correct in every retro.'),
(8, 'MacGyver', 10, 'TWO_WEEKS', 'Fixes production outages with duct tape and a paperclip metaphor.'),
(9, 'Sheldon C.', 9, 'IMMEDIATE', 'Correct 95% of the time, insufferable 100% of the time.'),
(10, 'Katniss E.', 5, 'IMMEDIATE', 'Excellent under pressure, terrible with public speaking.'),
(11, 'Michael S.', 11, 'NOT_LOOKING', 'World''s best boss, according to a mug he bought himself.'),
(12, 'Olivia P.', 13, 'TWO_WEEKS', 'Handles it. Whatever it is.'),
(13, 'Ted L.', 7, 'IMMEDIATE', 'Turns every technical setback into a folksy metaphor.'),
(14, 'Miranda P.', 18, 'NOT_LOOKING', 'Reviews every PR personally. Says nothing. Everyone panics.'),
(15, 'Dwight S.', 9, 'IMMEDIATE', 'Assistant to the regional backend engineer.');

INSERT INTO candidate_skills (candidate_id, skill) VALUES
(1,'deduction'),(1,'pattern-recognition'),(1,'forensics'),
(2,'research'),(2,'time-management'),(2,'public-speaking'),
(3,'systems-design'),(3,'rapid-prototyping'),(3,'leadership'),
(4,'project-management'),(4,'stakeholder-management'),(4,'public-speaking'),
(5,'woodworking'),(5,'minimalism'),(5,'negotiation'),
(6,'systems-design'),(6,'rapid-prototyping'),(6,'chemistry'),
(7,'persuasion'),(7,'research'),(7,'public-speaking'),
(8,'rapid-prototyping'),(8,'resourcefulness'),(8,'chemistry'),(8,'systems-design'),
(9,'theoretical-analysis'),(9,'pattern-recognition'),(9,'research'),
(10,'precision'),(10,'strategy'),(10,'crisis-management'),
(11,'sales'),(11,'public-speaking'),(11,'team-building'),
(12,'crisis-management'),(12,'negotiation'),(12,'strategy'),(12,'leadership'),
(13,'team-building'),(13,'optimism'),(13,'mentorship'),(13,'public-speaking'),
(14,'leadership'),(14,'negotiation'),(14,'stakeholder-management'),(14,'precision'),
(15,'sales'),(15,'negotiation'),(15,'security'),(15,'loyalty');

INSERT INTO candidate_traits (candidate_id, trait) VALUES
(1,'analytical'),(1,'blunt'),
(2,'detail-oriented'),(2,'overachiever'),
(3,'confident'),(3,'innovative'),
(4,'tenacious'),(4,'organized'),
(5,'stubborn'),(5,'principled'),
(6,'genius'),(6,'reckless'),
(7,'optimistic'),(7,'sharp'),
(8,'calm'),(8,'improviser'),
(9,'rigid'),(9,'brilliant'),
(10,'resilient'),(10,'decisive'),
(11,'enthusiastic'),(11,'chaotic'),
(12,'decisive'),(12,'intense'),
(13,'empathetic'),(13,'persistent'),
(14,'demanding'),(14,'decisive'),
(15,'intense'),(15,'loyal');

INSERT INTO jobs (id, title, minimum_experience, tagline) VALUES
(1, 'Backend Detective', 3, 'We have a bug. We have no leads. We have you.'),
(2, 'Rapid Prototyping Engineer', 2, 'Ship first, document never (kidding - please document).'),
(3, 'Developer Relations Lead', 2, 'Explain complex things to confused humans, cheerfully.'),
(4, 'Engineering Manager, Chaos Team', 5, 'Herd cats. The cats are senior engineers.'),
(5, 'Incident Commander', 4, '3am page. You''re the one who picks up.'),
(6, 'Sales Engineer', 3, 'Sell the vision, then go build it.');

INSERT INTO job_required_skills (job_id, skill) VALUES
(1,'deduction'),(1,'pattern-recognition'),(1,'forensics'),
(2,'rapid-prototyping'),(2,'systems-design'),
(3,'public-speaking'),(3,'research'),
(4,'team-building'),(4,'stakeholder-management'),(4,'leadership'),
(5,'crisis-management'),(5,'strategy'),(5,'negotiation'),
(6,'sales'),(6,'public-speaking'),(6,'negotiation');

INSERT INTO job_culture_keywords (job_id, keyword) VALUES
(1,'analytical'),(1,'autonomous'),
(2,'innovative'),(2,'fast-paced'),
(3,'energetic'),(3,'curious'),
(4,'empathetic'),(4,'organized'),
(5,'decisive'),(5,'calm-under-pressure'),
(6,'enthusiastic'),(6,'persistent');
