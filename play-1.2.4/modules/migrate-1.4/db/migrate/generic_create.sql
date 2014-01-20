CREATE DATABASE ${db};

-- A migratable database has to have a 'patchlevel' table, which stores the version and the status of the last update.
CREATE TABLE  ${db}.patchlevel (version int(10) unsigned NOT NULL, status varchar(255) default NULL, PRIMARY KEY  (`version`));
insert into ${db}.patchlevel (version, status) values (0,'Successful');