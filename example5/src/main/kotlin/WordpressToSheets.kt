import io.floodplain.elasticsearch.elasticSearchConfig
import io.floodplain.elasticsearch.elasticSearchSink
import io.floodplain.kotlindsl.*
import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.empty
import io.floodplain.mongodb.mongoConfig
import io.floodplain.mongodb.mongoSink
import io.floodplain.sink.sheet.googleSheetConfig
import io.floodplain.sink.sheet.googleSheetsSink
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import java.math.BigDecimal
import java.util.Collections.emptyList

private val logger = mu.KotlinLogging.logger {}

@QuarkusMain
class WordpressToSheets: QuarkusApplication {


    override fun run(vararg args: String?): Int {
        stream {
            var spreadsheetId = "1COkG3-Y0phnHKvwNiFpYewKhT3weEC5CmzmKkXUpPA4"
            val sheetConfig = googleSheetConfig("sheets")

            val mysqlConfig = mysqlSourceConfig("mysqlsource", "localhost", 3306, "root", "mysecretpassword", "wpdb")
            mysqlSource("wpdb.wp_posts", mysqlConfig) {
                filter { _,msg ->
                    msg["post_status"] == "publish"
                }
                set { _,msg,_ ->
                    msg["_row"] = msg["ID"]
                    msg
                }
                googleSheetsSink("outputtopic", spreadsheetId, listOf("post_title", "post_name","post_status","comment_status","comment_count"), "A",1,sheetConfig)
            }
        }.runWithArguments(args) {
            Quarkus.waitForExit()
        }
        return 0
    }

}