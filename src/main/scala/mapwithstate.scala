import org.apache.spark.sql.SparkSession
import java.sql.Timestamp
import org.apache.spark.sql.functions.{
  timestamp_seconds,
  round,
  get_json_object
}
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.Row

// Case class for basket events
case class Basket(user: String, item: String, quantity: Int, eventtime: Timestamp)

//Case class for basket state
case class BasketState(user: String, item: String, quantity: Int, accu: String)

object mapWithState {

//  This function is used on "mapGroupsWithState" to accumulate and update state of the 
// basket based on latest data  

  def updateBasketQuantity(
      key: (String, String),
      transactions: Iterator[Basket],
      state: GroupState[BasketState]
  ): BasketState = {

    // This function recursively updates the state of basked based on current transactions
    // in mini-batch
    def updateState(
        currentState: BasketState,
        transactionsToProcess: Iterator[Basket]
    ): BasketState = {
      if (transactionsToProcess.hasNext) {
        val currentTransaction = transactionsToProcess.next()
        val newQuantity = currentTransaction.quantity
        val updatedQuantity =
          if (currentState.quantity + newQuantity < 0) 0
          else currentState.quantity + newQuantity
        val newState = new BasketState(
          key._1,
          key._2,
          updatedQuantity,
          currentState.accu + " " + newQuantity
        )
        updateState(newState, transactionsToProcess)
      } else currentState
    }

    if (state.exists)
      state.update(updateState(state.get, transactions))
    else
      state.update(
        updateState(new BasketState(key._1, key._2, 0, ""), transactions)
      )

    return state.get
  }

  def main(args: Array[String]) {

    val spark = SparkSession.builder.appName("mapWithState").getOrCreate()
    import spark.implicits._

    val datastream = (spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 4000)
      .load())

    // Although we get eventime in the data, it is not used anywhere. It is just how the data is 
    // generated by the generator.
    val data_table: Dataset[Basket] = datastream
      .select(
        get_json_object($"value", "$.user").alias("user"),
        get_json_object($"value", "$.item").alias("item"),
        get_json_object($"value", "$.quantity").cast("int").alias("quantity"),
        timestamp_seconds(
          round(get_json_object($"value", "$.eventtime") / 1000)
        ).alias("eventtime")
      )
      .as[Basket]

    // This cis where we calls our function to update the state
    val grouped_data_ds = (data_table
      .groupByKey(row => (row.user, row.item))
      .mapGroupsWithState(updateBasketQuantity _))

    // Output the results.  "Complete" output mode is not supported in this scenario  
    val streamingQuery = (grouped_data_ds.writeStream
      .format("console")
      .option("truncate", "false")
      .outputMode("update")
      .trigger(Trigger.ProcessingTime("10 second"))
      .start())

    streamingQuery.awaitTermination()

  }
}