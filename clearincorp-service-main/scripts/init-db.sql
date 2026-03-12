-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS llc;

-- Create the user and grant privileges (for manual MySQL setup)
-- CREATE USER 'llc_user'@'localhost' IDENTIFIED BY 'password';
-- GRANT ALL PRIVILEGES ON llc.* TO 'llc_user'@'localhost';
-- FLUSH PRIVILEGES;

-- Tables will be automatically created by Hibernate on first startup
-- due to spring.jpa.hibernate.ddl-auto: update in application-local.yml
