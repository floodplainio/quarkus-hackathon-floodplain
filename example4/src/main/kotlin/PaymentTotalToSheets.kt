import io.floodplain.elasticsearch.elasticSearchConfig
import io.floodplain.elasticsearch.elasticSearchSink
import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.empty
import io.floodplain.kotlindsl.postgresSource
import io.floodplain.kotlindsl.postgresSourceConfig
import io.floodplain.kotlindsl.stream
import io.floodplain.kotlindsl.scan
import io.floodplain.kotlindsl.set
import io.floodplain.mongodb.mongoConfig
import io.floodplain.mongodb.mongoSink
import io.floodplain.sink.sheet.googleSheetConfig
import io.floodplain.sink.sheet.googleSheetsSink
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import java.math.BigDecimal
import java.util.Collections.emptyList

@QuarkusMain
class PaymentTotalToSheets: QuarkusApplication {

    val spreadsheetId = "1MTAn1d13M8ptb2MkBHOSNK1gbJOOW1sFQoSfqa1JbXU"

    override fun run(vararg args: String?): Int {
        stream {
            val postgresConfig = postgresSourceConfig("mypostgres", "localhost", 5432, "postgres", "mysecretpassword", "dvdrental", "public")
            val sheetConfig = googleSheetConfig("sheets")
            postgresSource("payment", postgresConfig) {
                scan({ empty().set("total", BigDecimal(0)) },
                        {
                            set { _, msg, state ->
                                state["total"] = (state["total"] as BigDecimal).add(msg["amount"] as BigDecimal)
                                state
                            }
                        },
                        {
                            set { _, msg, state ->
                                state["total"] = (state["total"] as BigDecimal).subtract(msg["amount"] as BigDecimal)
                                state
                            }
                        }
                )
                set { _, total, _ ->
                    total["_row"] = 2L
                    total
                }
                googleSheetsSink("outputtopic", spreadsheetId, listOf("total"), "B",2,sheetConfig)
            }
        }.runWithArguments(args) {
            Quarkus.waitForExit()
        }
        return 0
    }

}