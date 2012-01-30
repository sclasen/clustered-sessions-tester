#Clustered Sessions Tester

Finagle Based Stress tester for clustered sessions on heroku.

    git clone https://github.com/sclasen/clustered-sessions-tester.git
    heroku create -s cedar

##Config vars (heroku config:add)

    APP_URL (required): URL of an app that will return a json payload like {"count": 15} that contains a clustered session persisted count of the times a client hits the url.
    CONCURRENCY: Number of concurrent sessions to create and use to test the app at APP_URL
    REQUESTS: Number of serialized requests a single client/session should make
    THINK: Number of miliseconds to pause between requests in a given session
    DEBUG: true/false if true, print debug output to logs


##Running 

    git push heroku master

You can run a single test by running

    heroku run tester

To generate more load, you can scale this across a number of dynos. 

    heroku scale tester=8

The the tester process will run to completion, and exit which heroku interprets as a crash.
Heroku will then restart the process, and it will again run to completion. At this point Heroku marks the process as crashed.
Once all your runs finish, run this:
    
    heroku scale web=0
    

