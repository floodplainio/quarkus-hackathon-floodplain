import io.floodplain.elasticsearch.elasticSearchConfig
import io.floodplain.elasticsearch.elasticSearchSink
import io.floodplain.kotlindsl.*
import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.empty
import io.floodplain.mongodb.mongoConfig
import io.floodplain.mongodb.mongoSink
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import java.util.Collections.emptyList

@QuarkusMain
class FilmsToElasticSearch: QuarkusApplication {
    override fun run(vararg args: String?): Int {
        stream {
            val postgresConfig = postgresSourceConfig("mypostgres", "localhost", 5432, "postgres", "mysecretpassword", "dvdrental", "public")
            val elasticConfig = elasticSearchConfig("elastic", "http://localhost:9200")
            postgresSource("film", postgresConfig) {
                joinGrouped {
                    postgresSource("film_category", postgresConfig) {
                        joinRemote({ msg -> "${msg["category_id"]}" }, true) {
                            postgresSource("category", postgresConfig) {}
                        }
                        set { _, msg, state ->
                            msg["category"] = state["name"] ?: "unknown"
                            msg["last_update"] = null
                            msg
                        }
                        group { msg -> "${msg["film_id"]}" }
                    }
                }
                set { _, msg, state ->
                    msg["categories"] = state["list"] ?: empty()
                    msg["last_update"] = null
                    msg
                }
                joinGrouped(optional = true) {
                    postgresSource("film_actor", postgresConfig) {
                        joinRemote({ msg -> "${msg["actor_id"]}" }, false) {
                            postgresSource("actor", postgresConfig) {
                            }
                        }
                        // copy the first_name, last_name and actor_id to the film_actor message, drop the last update
                        set { _, actor_film, actor ->
                            actor_film["last_name"] = actor["last_name"]
                            actor_film["first_name"] = actor["first_name"]
                            actor_film["actor_id"] = actor["actor_id"]
                            actor_film["last_update"] = null
                            actor_film
                        }
                        // group the film_actor stream by film_id
                        group { msg -> "${msg["film_id"]}" }
                    }
                }
                set { _, film, actorlist ->
                    film["actors"] = actorlist["list"] ?: emptyList<IMessage>()
                    film
                }
                elasticSearchSink("@filmwithcategories", "@filmwithcategories", elasticConfig)
            }
        }.runWithArguments(args) {
            Quarkus.waitForExit()
        }
        return 0
    }

}