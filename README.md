# SimBa

### About

SimBa stands for Bayesian Simulation.

Right now this project is an engine based on actors for discrete simulations.

### How to run engine in test mode on local machine.

1. Server node.  
`./gradlew run --args="-server" -PisProduction -Dconfig.resource=local1_test.conf` (or configuration `simba [run server test]`).
This command runs server (that is accessible at http://0.0.0.0:9090) and first engine node.
2. Additional nodes  
To use second and third nodes run corresponding commands: 
   * `./gradlew run -PisProduction -Dconfig.resource=local2_test.conf`
   (or configuration `simba [run local2 test]`)
   * `./gradlew run -PisProduction -Dconfig.resource=local3_test.conf`
   (or configuration `simba [run local3 test]`)  
These nodes will connect to the first one automatically.
3. Open [simulation in browser](http://0.0.0.0:9090). To choose which simulation to run navigate to 
`src/jvmMain/kotlin/space/kscience/simba/Server.kt` and uncomment necessary one.

### Production mode

You can run engine on multiple machines with postgresql database, but it requires additional configuration.
1. Use the same commands, but drop `_test` suffix from `.conf` file.
2. You need running postgres database on the same machine where server is running. Requirements for DB are described in 
`src/jvmMain/resources/application.conf`.
3. To specify remote machines change `127.0.0.1` in `src/jvmMain/resources/local-shared.conf` to something meaningful.

### Notes

1. By default, logs are disabled because they occupy a lot of space. To enable logging uncomment line 
`<appender-ref ref="ACTORS_FILE"/>` in `src/jvmMain/resources/logback.xml` file. Logs can be found in `build/actors.log` file.

### How to add new simulation

1. Implement interface `space.kscience.simba.state.ObjectState`. This is where all actor's state will be stored.
2. (Optional) Implement interface `space.kscience.simba.state.EnvironmentState`. Here we can store some information 
that is common for all agents.
3. Implement interface `space.kscience.simba.simulation.Simulation`, see examples in `src/jvmMain/kotlin/space/kscience/simba/simulation`.
This is the connection between server and engine.
4. Instantiate newly created simulation in `space.kscience.simba.ServerKt.main`
5. To be able to see results in browser we must implement corresponding JS handler `space.kscience.simba.simulation.GameSystem`, 
see examples in `src/jsMain/kotlin/space.kscience.simba/simulation`.

### Useful links

1. https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html
2. https://doc.akka.io/docs/akka/current/typed/persistence.html
3. https://doc.akka.io/docs/akka/current/persistence-plugins.html#persistence-plugin-proxy

## TODO
1. Implement cluster engine using something besides `Akka`. It is quite hard to use this framework. Documentation is a 
mess, very little information in internet and a lot of features just miss.
2. Try to get rid of java serialization and use something like `kotlinx.serialization`. Right now it is not 
implemented because we need to write custom serializer for Akka using `kotlinx.serialization` and this is quite hard.
3. Add possibility to created agents by other agents. Right now the number of agents is fixed and is not changing 
during simulation.
4. Add `kafka` support for aggregators. There can be a ton of messages and just send them back to main server is a bad 
idea, we need some sort pipeline.
5. Add possibility to run engine from given "snapshot" that is taken, for example, from DB. In case of long 
simulations we want to be able to stop it and continue later. Probably we must use something like `Redis` or `ZeroMQ`.
6. Add "reactive" way of simulation. Right now all agents will wait for their neighbours. We want to support "reactive"
way where agent will be iterating as soon as it is ready, in case when some agents disappeared unexpectedly.
7. Add a way to configure cluster in code and not in config file. Right now we can run only 3 different nodes because
we only have 3 config files. This can be easily fixed by adding new files, but better idea will be to use code configuration.
8. Run all given simulations at once. Right now we need to manually change simulation in `Server` file. We can't 
run them simultaneously because they will occupy the same port.
9. Make a proper DSL to be able to create a new simulation instead of manual implementation of all these interfaces.
10. Add the ability for agents to exchange intermediate messages. Right now agents can only send predetermined messages 
about their state to predetermined neighbours. Probably it will be useful to extend this.
