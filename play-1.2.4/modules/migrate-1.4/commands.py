import sys
import inspect
import os
import glob
import re
import subprocess

try:
    from play.utils import *
except:
    pass

MODULE = 'migrate'

COMMANDS = ['migrate','migrate:help','migrate:init','migrate:up','migrate:version','migrate:drop-rebuild','migrate:create','migrate:drop']

app = None

# Migrate - database migration and creation

def getFormatString():
    return app.readConf('migrate.module.file.format').replace("[[eq]]","=")

def getDbArg():
    grab_next = False
    for arg in sys.argv:
        if arg.strip() == "--db":
            grab_next = True
        elif arg.startswith("--db=") or grab_next == True:
            spec_db = arg.replace("--db","")
            print "~ Processing specified database: " + spec_db
            return spec_db

    return None
    
# ~~~~~~~~~~~~~~~~~~~~~~ getUpToVersion() is to look up the desired migration version number, to which we'd like to migrate
def getUpToVersion():
    grab_next = False
    for arg in sys.argv:
        if arg.strip() == "--to":
            grab_next = True
        elif arg.startswith("--to=") or grab_next == True:
            try:
                to_version = int(arg.replace("--to=",""))
                print "~ Migrating to version: %(tv)s" % {'tv': to_version}
                return to_version
            except TypeError:
                print "~   ERROR: unable to parse --to argument: '%(ta)s'" % { 'ta': arg }
                return None

    return None

# ~~~~~~~~~~~~~~~~~~~~~~ getVersion(dbname) is to look up the version number of the database
def getVersion(dbname):
    [tmp_path,f] = createTempFile('migrate.module/check_version.sql')
    f.write("select %(version)s, %(status)s from patchlevel" %{ 'version':"version", 'status': "status" })
    f.close()
    
    # The format string for running commands through a file
    db_format_string = getFormatString()
    command_strings = getCommandStrings()
    command_strings['filename'] = tmp_path
    command_strings['dbname'] = dbname
    db_cmd = db_format_string % command_strings
    
    [code, response] = runDBCommand(db_cmd)
    if code <> 0:
        print "Failure " + response
        sys.exit(-1)

    parts = response.split()
    return [parts[0]," ".join(parts[1:])]
    
# ~~~~~~~~~~~~~~~~~~~~~~ updateVersionTo(dbname,version) updates the version number in the passed database
def updateVersionTo(dbname,version):
    [tmp_path,f] = createTempFile('migrate.module/update_version.sql')
    f.write("update patchlevel set version = %(version)s, status = '%(status)s'" %{ 'version':version, 'status': "Successful" })
    f.close()
    
    # The format string for running commands through a file
    db_format_string = getFormatString()
    command_strings = getCommandStrings()
    command_strings['filename'] = tmp_path
    command_strings['dbname'] = dbname
    db_cmd = db_format_string % command_strings
    
    [code, response] = runDBCommand(db_cmd)
    if code <> 0:
        print "~ ERROR updating version number: "
        print "    " + response
        sys.exit(-1)
        
# ~~~~~~~~~~~~~~~~~~~~~~ updateStatusTo(dbname,status) updates the status in the passed database
def updateStatusTo(dbname,status):
    [tmp_path,f] = createTempFile('migrate.module/update_status.sql')
    f.write("update patchlevel set status = '%(status)s'" %{'status': status })
    f.close()
    
    # The format string for running commands through a file
    db_format_string = getFormatString()
    command_strings = getCommandStrings()
    command_strings['filename'] = tmp_path
    command_strings['dbname'] = dbname
    db_cmd = db_format_string % command_strings
    
    [code, response] = runDBCommand(db_cmd)
    if code <> 0:
        print "~ ERROR updating status: "
        print "~    " + response
        sys.exit(-1)
        
    
# Constructs a temporary file for use in running SQL commands 
def createTempFile(relative_path):
    tmp_path = os.path.normpath(os.path.join(app.path, 'tmp/' + relative_path))
    pathdir = os.path.dirname(tmp_path)
    if not os.path.exists(pathdir):
        os.makedirs(pathdir)
    return [tmp_path, open(tmp_path,'w')]

    
# ~~~~~~~~~~~~~~~~~~~~~~ getCommandStrings() retrieves the command parameters for running a DB command from the command line
def getCommandStrings():
    db_create_user = app.readConf('migrate.module.username')
    db_create_pwd = app.readConf('migrate.module.password')
    db_port = app.readConf('migrate.module.port')
    db_host = app.readConf('migrate.module.host')
    return {'username': db_create_user, 'password': db_create_pwd, \
            'host': db_host, 'port': db_port, 'filename': "", 'dbname': "" } 

# ~~~~~~~~~~~~~~~~~~~~~  Runs the specified command, returning the returncode and the text (if any)            
def runDBCommand(command):
    returncode = None
    line = ""
    try:
        create_process = subprocess.Popen(command, env=os.environ, shell=True, stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
        while True:
            returncode = create_process.poll()
            line += create_process.stdout.readline()     
            if returncode != None:
                break;
                
    except OSError:
        print "Could not execute the database create script: " 
        print "    " + command
        returncode = -1
    
    return [returncode,line]
    
# ~~~~~~~~~~~~~~~~~~~~~~ Retrieves the list of migration files for the specified database.  The files live in the
# ~~~~~~~~~~~~~~~~~~~~~~ {playapp}/db/migrate/{dbname} folder and follow a naming convention: {number}.{up|down}.{whatever}.sql
def getMigrateFiles(dbname, exclude_before):
    search_path = os.path.join(app.path, 'db/migrate/',dbname + '/*.up*.sql')

    initial_list = glob.glob(search_path)
    return_obj = {}
    collisions = []
    # Filter the list to only the specified pattern
    pat = re.compile('(\d+)\.up.*\.sql\Z')
    maxindex = 0
    for file in initial_list:
        match = re.search(pat,file)
        index = int(match.group(1))
        if index in return_obj:
            collisions.append("" + return_obj[index] + "  <==>  " + file)
        if match != None and index > exclude_before:
            return_obj[index] = file
        if match != None and index > maxindex:
            maxindex = index
            
    # Check for collisions
    if len(collisions) > 0:
        print "~"
        print "~ ======================================================================================================"
        print "~ "
        print "~  ERROR:  Migrate collisions detected.  Please resolve these, then try again"
        print "~"
        print "~  Collision list:"
        for item in collisions:
            print "~         " + item
        print "~"
        print "~"
        sys.exit(-1)
        
    # Check for gaps
    missed = []
    for idx in range((exclude_before + 1),maxindex):
        if idx not in return_obj:
            missed.append(idx)
    
    if len(missed) > 0:
        print "~"
        print "~ ======================================================================================================"
        print "~ "
        print "~  ERROR:  Migrate file gaps detected.  Please resolve these, then try again"
        print "~"
        print "~  Files at the following levels are missing:"
        for idx in missed:
            print "~      %s" % idx
        print "~"
        print "~"
        sys.exit(-1)
            
    return [maxindex, return_obj]

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ extracts the database and its alias from the passed database name.
def extractDatabaseAndAlias(db):
    db = db.strip()
        
    # See if there's an alias.
    match = re.search('(\w+)\[(\w+)]',db);
    if match == None:
        db_alias = db;
        db_alias_name = 'None';
    else:
        db_alias = match.group(2);
        db_alias_name = db_alias;
        db = match.group(1);
        
    return [db,db_alias,db_alias_name]
    
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ interpolates the creation file with the passed database name.
def interpolateDBFile(db, createpath):
    [tmp_path,f] = createTempFile('migrate.module/temp_create_%(db)s.sql' % {'db': db})
    print "~ Creating temp file: %(tf)s" % {'tf':tmp_path}
    for line in open(createpath).readlines():
        f.write(line.replace("${db}",db))

    f.close()
    
    return tmp_path
    
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Runs the creation script
def runCreateScript(createpath, createname):
        db_format_string = getFormatString()
        db_commands = getCommandStrings()
        db_commands['filename'] = createpath
        db_commands['dbname'] = ""

        db_create_cmd = db_format_string %db_commands
            
        print "~ Running script %(cs)s..." % {'cs': createname}
                
        [code,response] = runDBCommand(db_create_cmd)
        if code <> 0:
            print "~ " + str(code)
            print "~ ERROR: could not execute the database create script: "
            print "~     " + db_create_cmd
            print "~ "
            print "~ Process response: " + response
            print "~ "
            print "~ Check your credentials and your script syntax and try again"
            print "~ "
            sys.exit(-1)


# ~~~~~~~~~~~~~~~~~~~~~~ Runs the database creation script
def create():
    try:
        is_generic = False
        
        if app.path:
            createpath = os.path.join(app.path, 'db/migrate/generic_create.sql')
            if not os.path.exists(createpath):
                createpath = os.path.join(app.path, 'db/migrate/create.sql')
                if not os.path.exists(createpath):
                    print "~ "
                    print "~ Unable to find create script"
                    print "~    Please place your database creation script in db/migrate/create.sql or db/migrate/generic_create.sql"
                    print "~ "
                    sys.exit(-1)
            else:
                is_generic = True
        else:
            print "~ Unable to find create script"
            sys.exit(-1)
            
        if is_generic:
            print "~ Using generic create script, replacing parameter ${db} with each database name..."
            print "~ "
            db_list = app.readConf('migrate.module.dbs').split(',')
            db_arg = getDbArg()
            
            for db in db_list:
                db = db.strip()
                # Extract the database name, trimming any whitespace.
                [db, db_alias, db_alias_name] = extractDatabaseAndAlias(db)
                
                # Skip the database if a specified was given
                if db_arg != None and db != db_arg:
                    continue
            
                print "~ Database: %(db)s" % {'db': db}
                
                # interpolate the generic file to contain the database name.
                interpolated = interpolateDBFile(db, createpath)
                # run the interpolated creation script
                runCreateScript(interpolated,'generic_created.sql (%(db)s)' % {'db': db})
        else:
            # Run the create script
            runCreateScript(createpath, 'create.sql')
            
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)
    
    print "~ "
    print "~ Database creation script(s) completed."
    print "~ "
    
    
# ~~~~~~~~~~~~~~~~~~~~~~ Performs the up migration task
def up():
    # The format string we'll use to run DB commands
    db_format_string = getFormatString()
    
    # Find the databases to iterat
    db_list = app.readConf('migrate.module.dbs').split(',')
    
    db_arg = getDbArg()
    
    to_version = getUpToVersion()
    
    print "~ Database migration:"
    print "~ "
    for db in db_list:
        
        # Extract the database name, trimming any whitespace.
        [db, db_alias, db_alias_name] = extractDatabaseAndAlias(db)
        
        # Skip the database if a specified was given
        if db_arg != None and db != db_arg:
            continue
    
        print "~    Database: %(db)s (Alias:%(alias)s)" % {'db':db, 'alias': db_alias_name }
        [version,status] = getVersion(db)
        print "~    Version: %(version)s" % {'version': version}
        print "~    Status: %(status)s" % {'status': status}
        [maxindex, files_obj] = getMigrateFiles(db_alias,int(version))
        print "~    Max patch version: " + str(maxindex)
        print "~ "
        if maxindex <= int(version):
            print "~    " + db + " is up to date."
            print "~ "
            print "~ "    
            continue
        print "~    Migrating..."
        command_strings = getCommandStrings()
        if to_version == None or to_version > maxindex:
            to_version = maxindex
        for i in range(int(version) + 1, to_version + 1):
            # Skip missed files
            if files_obj[i] == None:
                print "~      Patch " + str(i) + " is missing...skipped"
                continue
                
            command_strings['filename'] = files_obj[i]
            command_strings['dbname'] = db
            db_cmd = db_format_string % command_strings
            
            [code, response] = runDBCommand(db_cmd)
            if code <> 0:
                print "~  Migration failed on patch " + str(i) + "!"
                print "~    ERRROR message: " + response
                updateStatusTo(db,response)
                
                sys.exit(-1)
            print "~      " + str(i) + "..."
        
            updateVersionTo(db,i)
        print "~ "
        print "~    Migration completed successfully"
        print "~ "
        print "~ ------------------------------------"
        print "~ "
        
# ~~~~~~~~~~~~~~~~~~~~~~ Drops all databases
def dropAll():
    db_list = app.readConf('migrate.module.dbs').split(',')
    db_arg = getDbArg()
    
    print "~ "
    print "~ Dropping databases..."
    for db in db_list:
    
        [db, db_alias, db_alias_name] = extractDatabaseAndAlias(db)
        
        # Skip the database if a specified was given
        if db_arg != None and db != db_arg:
            continue
    
        print "~    drop %(db)s" % {'db':db}
        [tmp_path,f] = createTempFile('migrate.module/drop_db.sql')
        f.write("drop database if exists %(db)s;" %{ 'db':db })
        f.close()
        
        # The format string for running commands through a file
        db_format_string = getFormatString()
        command_strings = getCommandStrings()
        command_strings['filename'] = tmp_path
        command_strings['dbname'] = ""
        db_cmd = db_format_string % command_strings
        
        [code, response] = runDBCommand(db_cmd)
        if code <> 0:
            print "Failure " + response
            sys.exit(-1)
    print "~ "
    print "~ Database drop completed"
    print "~ "

def execute(**kargs):
    global app
    play_command = kargs.get("command")
    app = kargs.get("app")
    
    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:create] Create the initial database
    if play_command == 'migrate:create':
        create()
        sys.exit(0)
        
    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:up] Migrate the database from it's current version to another version
    if play_command == 'migrate:up':
        up()
        sys.exit(0)

    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:version] Output the current version(s) of the datbase(s)
    if play_command == 'migrate:version':
        db_list = app.readConf('migrate.module.dbs').split(',')
        db_arg = getDbArg()

        print "~ Database version check:"
        print "~ "
        for db in db_list:
            [db, db_alias, db_alias_name] = extractDatabaseAndAlias(db)
            
            # Skip the database if a specified was given
            if db_arg != None and db != db_arg:
                continue
        
            [version, status] = getVersion(db)
            format = "%(dbname)-20s version %(version)s, status: %(status)s" % {'dbname':db, 'version':version, 'status': status}
            print "~ " + format 
        
        print "~ " 
        sys.exit(0)

    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:init] Build the initial / example files for the migrate module
    if play_command == 'migrate:init':
        app.override('db/migrate/generic_create.sql', 'db/migrate/generic_create.sql')
        app.override('db/migrate/db1/1.up.create_user.sql', 'db/migrate/db1/1.up.create_user.sql')
        print "~ "
        sys.exit(0)

    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:drop-rebuild] drop then rebuild databases
    if play_command == 'migrate:drop-rebuild':
        dropAll()
        create()
        up()
        sys.exit(0)
        
    # ~~~~~~~~~~~~~~~~~~~~~~ [migrate:drop] drop databases
    if play_command == 'migrate:drop':
        dropAll()
        sys.exit(0)
        
    if play_command.startswith('migrate:') or play_command == 'migrate':
        print "~ Database migration module "
        print "~  "
        print "~ Use: migrate:create to create your initial database" 
        print "~      migrate:up to migrate your database up" 
        print "~      migrate:version to read the current version of the database" 
        print "~      migrate:init to set up some initial database migration files" 
        print "~      migrate:drop-rebuild to drop and then rebuild all databases (use with caution!)"
        print "~      migrate:help to show this message"
        print "~  "
        print "~ Add --db={database name} to any command to only affect that database"
        print "~ Add --to={version number} to any command to only migrate to the specified version number"
        
        print "~ "
        
        sys.exit(0)
        
class MockPlayApp:
    override = None
    readConf = None
    path = None
    
# 1.0 version code
try:
    cmd_10 = play_command
    app = MockPlayApp()
    app.override = override
    app.readConf = readConf
    app.path = application_path
    execute(command=play_command, app=app)
except NameError:
    pass
