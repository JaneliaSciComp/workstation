
Configurations laid out here, will work, according to the author's latest tests (as of 8/9/2006).

The log4j props create a log file called 'mapping_generation.log' in the starting directory.

The hibernate properties (.properties may be redundant with .cfg.xml), but both probably point to the same location.  And
that config will create a simple HSQLDB database that can be populated with data.  Alternatively, there is a collection
of files for PostGres, that points at the author's database.  So please do not blow away my database ;-)

+Les Foster+