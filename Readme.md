# Demonstrates Custom Aggregation with State.

This is described on this [Blog](https://www.kamalsblog.com/2021/09/stateful-aggregation-with-mapgroupswithstate.html) along with demo video

For data stream use  data generator [from here](https://github.com/skamalj/datagenerator)

## Generator Config used
```
datatype|number|user|{"min":1,"max":3}
helpers|randomize|item|["itemA","itemB","itemC"]
datatype|number|quantity|{"min":-10,"max":10}
```

### To  build and package 
`sbt clean package`

### To  run
`$SPARK_HOME/bin/spark-submit --class mapWithState ./target/scala-2.12/mapwithstate_2.12-1.0.jar`
