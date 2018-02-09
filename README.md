# Meltwater practical skills assessment – Simulate SMSC (Short Message Service Center)

Author: Zoltán Domahidi  
Date: November 11, 2017  
Location: Budapest, Hungary  
  

## Task

#### Story (just for the fun of it)

Monday morning – I’m commuting to work and going through my personal TODO list. I suddenly realize that I
should have send a lot of SMS to different people; answer friends on their “go to the pub”-requests, work
colleagues to answer their questions, ... . As I’m thinking on how I can accomplish this task in an efficient way,
there is also the message coming in from my wife asking me to pick up the dry cleaning.  

This would all be so simple with a single mobile phone, but, probably out of historic reasons, I have several
and now I’m stuck with several.  

Sending all those messages will take me ages... but If I could “hack” the SMSC (Short Message Service
Center, for details see: http://en.wikipedia.org/wiki/Short_message_service_center) this would be so much
simpler... I can do the “hack” part, but I would need a simple simulation of an SMSC.  

It would be great if you could help me with that task...

#### Task

Your task is to create a simple SMSC, which can be controlled through an input file (see Input). The SMSC
should provide the following functionality:  
 * Allow a Number (which can be added to the system at any point in time) to subscribe/unsubscribe
(this happens when you switch on your phone)
 * Send a message from one subscriber to another subscriber
 * Send a message to a group of subscribers (by using some wildcard pattern)
 * Send a message to all subscribers  

It should be noted that a an SMSC is a dynamic and concurrent system, so you should think carefully on how
to handle corner cases:
 * Subscriber appears/disapper at random times
 * A message might be sent to a subscriber that doesn’t exist (yet/anymore)  

Your implementation should make sure (if time allows) to handle those conditions gracefully, i.e. clearly
defined manner (It is up to you to decide what the exact behavior is, i.e. you don’t need to imitate the behavior
of a real SMSC).  
To acknowledge the reception of a message should be done by simply printing the appropriate information to
the console (see Output).

#### Input

You receive as input to your program a path to a file which will contain instructions about subscribers, and
messages and from who to whom a message should be sent. The file can contains a sequence of statements
with the following syntax (elements in greater/less than signs (i.e.: <>) are variables, parts in square brackets
(i.e.: []) are optional elements):
 * ```number<int> <phone number>```: This is to make the system aware of a new subscriber (phone
number). This does not mean that the number is connected to the SMSC (the phone needs to be
switched on), see below
 * ```subscribe <number>```: This is to register a number (see above) with the SMSC
 * ```unsubscribe <number>```: This is the inverse of “subscribe” (see above) to unregister a number
from an SMSC. The number is still known in general though
 * ```group <phone number pattern>[,<phone number pattern>]```: This is allows to define a
group of phone numbers (see above). A ```<phone number pattern>``` is a phone number with an
optional asterisk (i.e.: *) as a wildcard.
 * ```message <sender number> <receiver number>[,<receiver number>] <message>```:
This will send a message (specified between double quotes) from the sender to the receiver(s). The
receiver number can also be a group number (see above). There is a special receiver number which
is called broadcast, which will send the message to all numbers.  

Here an example input file:  

```
  number1 +36991212321
  subscribe number1

  number2 +36991234321
  subscribe number2

  number3 +36991234567
  subscribe number3

  number4 +36991212121

  group1 +3699123*

  unsubscribe number1
  message number1, number2,number3 "guys, are you going for lunch?"

  message number1 number2 "Hi Ann, pub this evening will be fine" 
  message number1 number4 "No, dry cleaning will have to wait until tomorrow"
  message number1 number2,number4 "guys, I'm back at work now..."
  message number3 group1 "The world is great, plan your vacation today"

  unsubscribe number3
  unsubscribe number1

  message group1 "The world is great, still haven't planned your vacation?"

  number5 1234

  subscribe number5
  
  message number5 broadcast "New phone service... check it out now!"
```

#### Output

You should output to the console when someone receives a message in a simple format:

```<from> <to> "<message>"```

For example:

```+36991212321 +36991234321 "Hi Ann, pub this evening will be fine"```


## Preparation

First, I was thinking about an hour how should I solve this problem.  
After reading the Wikipedia page about the SMS-Center I was thinking this messaging stuff should work like microservices,
so I will need two RESTful microservices. One for the SMS-Center (receiving subscriptions and sending messages) and one for 
simulating the devices (sending and receiving messages).  
The SMS-C microservice should store somewhere all the data, including the messages what could not deliver.  
At the beginning I wanted to use Redis for storage, because it's persistent cache with expiring items capability.
So this way the messages would survive a service restart, and I should not take care about checking and removing messages
what have been in the system undelivered for long time. I was thinking it would be nice to use Docker containers, so 
whoever wants to check the working application should not install Redis to his machine. I wanted to develop as most as 
I can using TDD but I'm not that familiar with Redis libraries so I was thinking that's going to be way too much for 2,5 
half hour of work.  
Instead I should use some Docker based database, like Postgre. Ok, but using the tests I should be independent from the 
underlying infrastructure, so I should use some in-memory database, like H2. To check that the data were persisted I will 
need some database management tool, using Docker of course. I tried for 10 minutes to reach the H2 of the application 
from the Docker container, but that took much time, so I gave up. That's still not 2,5 hours of work.  
So I thought ok, let's do this incrementally:
 * Design the service and repository layer. The repository layer should 
 use Java objects (lists, maps) but should be Spring Data compatible, so later I could easily replace them. 
 * The package structure should follow the structure of separate domains (separate application for the server, and 
 separate one for the client) so if I will have time I can easily refactor it into a separate application.
 * When the service layer works I should replace the repository layer with Spring Data using Postgre database.
 * If the time is still enough I could add the REST layer, using only hardcoded URLs. That would be much effort to use 
 some service discovery here.

 
## Concept

 * Separate responsibilities (services):
   * Registering accounts (number with name) and groups (number patters with name) 
   * Subscribing and unsubscribing of devices
   * Transportation and redelivery of messages
 * At subscription time the number of accounts should be stored so the original message will be delivered even if the 
 number of the same account changes in time. 
 * The sender must be subscribed when sends message.
 * If the receiver is registered to the system but not subscribed, the numbers and the message should be stored
 * A scheduler tries to redeliver the undelivered messages (hoping that the receiver will subscribe later)
 * The application will delete the undelivered messages older than a given time.

 
## Technologies used and running the application

During the development I was using:
 * Java 8 (1.8.0_131 need to be installed)
 * Spring Boot 1.5.8
 * Gradle 3.5.1 (wrapper)
 * Junit 4.12
 * Mockito 2.12
 * Lombok 1.16.18
 * Git 2.7.4
 * IntelliJ
 
You can run the application two ways:
 * Using Gradle by running the following command in terminal when standing in the folder of the project:  
 ```./gradlew bootRun```
 * Running the main method of SmscApplication class in the IDE.
 (Don't forget to install [Lombok](https://projectlombok.org/))
 > When using IntelliJ don't forget to turn on Annotation Processing:   
 File -> Settings... -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Enable annotation processing)
 
 
## Development

I tried to use TDD as much as I can but due to time limitations I could not use step by step implementation.  
I was writing tests for a separate module then the implementation. You can track this evolution from the Git history.
When I realised that this takes more time than I was expecting after a while I started to hurry up, so some parts are 
ugly, I admit.  
Here are some pain-points:
 * the SubscriptionService, it should be refactored because using too much dependencies (all the repositories).
 * the main application (this was the last part of the application after 6 hours of testing and implementation, 
 so I was tired and just wanted to see everything working with the input file)
 * some operations should use Java8 features much wisely (stream processing, try-catch handling in main application)
 * scheduling should be configurable from property file (now it is hardcoded for 3 seconds) and should be in a separate 
 configuration file
 * meanwhile the tests are passing and the input file scenario was running successfully, I'm not convinced that the 
 application is bug-free
 
Some notes about the implementation details:  
 * registering accounts with existing name are overwritten (to avoid sending error message to the client)
 * registering groups with existing numbers are extended (to avoid sending error message to the client)
 * validation's are simple:
   * account name must start with **number** followed by a number
   * account number must start with **+** followed by 11 digits (for this reason I changed the input file, so number5 can 
   not have account number 1234, otherwise it will fail to register)
   * group name must start with **group** followed by a number
   * a list of account numbers can be registered for groups
   * group number can have a wildcard <b>*</b> character at the end of the number
 * message format must be  
 ```message <source_account_number> <destination_account_numbers> "message"'```    
   (no comma after source account number; destination account numbers separated with commas without spaces between them;
    message in double quotes; space between the tokens)
 * tokens are separated with a single space
 * message must have a source account number (at this moment) otherwise an exception is thrown
 * the input file is processed instantly, meanwhile you can add blocking waits by using   
 ```sleep <delay_in_seconds>```