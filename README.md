# Tunnit

Read and count hours from tracking files.

File format `[date] [project code] [hours] [comment]`

Example rows for supported formats
```
2018-02-01  p1234   10:10-12:00		Working on project
2018-02-01  p1234   7.5h 			Working on project
2018-02-01  p1234   7h30m 			Working on project
```

## Run
```
-f FILENAME     Hour files to read
-d NUMBER       Initial diff in minutes
```

`lein run -f 2018-01.txt -d 0`

### Run standalone version
If you have Java JRE installed you can run app without installing Clojure or Leiningen.

Get latest jar from [releases](https://github.com/jleh/tunnit/releases).

Run:
`java -jar tunnit.jar -f 2018-01.txt -d 0`

## How to run the tests

`lein midje` will run all tests.

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.
