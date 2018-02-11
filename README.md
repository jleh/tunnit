# Tunnit

Read and count hours from tracking files.

File format `[date] [project code] [hours] [comment]`

Example row
`2018-02-01  p1345   10:10-12:00 Working on project`

## Run
```
-f FILENAME     Hour files to read
-d NUMBER       Initial diff in minutes
```

`lein run -d 2018-01.txt -d 0`

## How to run the tests

`lein midje` will run all tests.

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.
