-- Epic 3 Phase 2 devstack — seed the two Bang databases + the app DB user.
-- Schema/tables are created on first run by BangServer (bang DB) and the OOO user system
-- (ooouser DB); this only creates the empty databases + the `bang`/`yeehaw` account the baked
-- server.properties expects. mysql_native_password avoids the MySQL-8 caching_sha2 handshake that
-- the 2005-era connector config (no allowPublicKeyRetrieval/SSL) can't complete over plaintext.
CREATE DATABASE IF NOT EXISTS bang;
CREATE DATABASE IF NOT EXISTS ooouser;
CREATE USER IF NOT EXISTS 'bang'@'%' IDENTIFIED WITH mysql_native_password BY 'yeehaw';
GRANT ALL PRIVILEGES ON bang.* TO 'bang'@'%';
GRANT ALL PRIVILEGES ON ooouser.* TO 'bang'@'%';
FLUSH PRIVILEGES;
