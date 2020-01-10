// requires guava-21.0.jar  
// javac -cp jar2/*:. Neo4jTestCase.java

import static org.neo4j.driver.v1.Values.parameters;

//import com.appdynamics.pi.services.graphservice.config.GraphConstants;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
//import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

/**
 * @author Piyush Mathur
 */
//@Slf4j
public class Neo4jTestCase {

    public final Driver driver;

    public Neo4jTestCase(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }


    /**
     * Add vertex to neo4j.
     */
    public void addVertex(String id, String label) {
        try (Session session = driver.session()) {
            Boolean value = session.writeTransaction(new TransactionWork<Boolean>() {
                @Override
                public Boolean execute(Transaction tx) {
                    StatementResult rs = tx.run("CREATE (n:" + label + " { qName: '" + id + "' })");
                    return null;
                }
            });
        } catch (Exception ex) {
            //log.error("Exception while adding a vertex.", ex);
        }
    }

    /**
     * Add a edge between to vertices.
     */
    public void addEdgeBetweenVertices(String from, String to) {
        try (Session session = driver.session()) {
            Boolean value = session.writeTransaction(new TransactionWork<Boolean>() {
                @Override
                public Boolean execute(Transaction tx) {
                    StatementResult rs = tx.run("MATCH (a:MobVertex),(b:MetricVertex)" +
                        "WHERE a.qName = '" + from + "' AND b.qName = '" + to + "'" +
                        "CREATE (a)-[r:MetricEdge { edgeId: '" + from + "_" + to + "'}]->(b)");
                    return null;
                }
            });
        } catch (Exception ex) {
            //log.error("Exception while adding a vertex.", ex);
        }
    }


    /**
     * Fetches all the edge attached to a mob.
     */
    public void getEdges(final String qName) {
        try (Session session = driver.session()) {
            Set<String> greeting = session.readTransaction(new TransactionWork<Set<String>>() {
                @Override
                public Set<String> execute(Transaction tx) {
                    Set<String> edges = Sets.newHashSet();
                    StatementResult result = tx
                        .run("MATCH (n:MobVertex {qName: $qName" + "})-[edges]-() return edges;",
                            parameters("qName", qName));
                    while (result.hasNext()) {
                        Map<String, Object> edge = result.next().get("edges").asMap();
                        edges.add((String) edge.get("edgeId"));

                    }
                    System.out.println("edges for qName: " + qName + "= " + edges.size());
                    return edges;
                }
            });
        }
    }


    /**
     * Detach and delete all the vertices in the set.
     */
    public void deleteVertices(Set<String> qNames, String label) {

        try (Session session = driver.session()) {

            session.writeTransaction(tx -> {
                tx.run("MATCH (n:" + label + ") where n.qName IN $set DETACH DELETE n",
                    parameters("set", qNames));
                return null;
            });

        } catch (Exception ex) {

        }

    }


    /**
     * Test case scenario: 1. First it creates a hierarchy where there are two type of vertices
     * MobVertex and MetricVertex. It creates about 20 MobVertex and attaches 200 MetricVertex under
     * each MobVertex.
     *
     * 2. Then we perform two operations in parallel a. A continuous operation to read edges of each
     * of the MobVertex. b. Detaching and deleting attached metric vertices.
     */
    public static void main(String... args) throws Exception {
        Neo4jTestCase testCase = new Neo4jTestCase("bolt://localhost:7687",
            "neo4j", "password");
        try {

            testCase.initializeNco4jSchema(testCase.driver);
            int startMobId = 1;
            int startMetricId = 1000;

            for (int i = startMobId; i < startMobId + 20; i++) {
                String mobId = "mob" + i;
                testCase.addVertex(mobId, "MobVertex");
                for (int j = startMetricId; j < startMetricId + 200; j++) {
                    String metricId = "metric" + j + i;
                    testCase.addVertex(metricId, "MetricVertex");
                    testCase.addEdgeBetweenVertices(mobId, metricId);
                }
            }

            new Thread(() -> {
                while (true) {
                    for (int i = startMobId; i < startMobId + 20; i++) {
                        String qName = "mob" + i;
                        testCase.getEdges(qName);
                    }
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    for (int i = startMobId; i < startMobId + 20; i++) {
                        Set<String> qNames = Sets.newHashSet();
                        for (int j = startMetricId; j < startMetricId + 200; j++) {
                            qNames.add("metric" + j + i);
                        }
                        testCase.deleteVertices(qNames, "MetricVertex");
                    }

                }
            }).start();

        } catch (Exception ex) {
            //log.error(ex.getMessage(), ex);
        }
    }


    /**
     * Initializing the schema
     */
    private void initializeNco4jSchema(Driver neo4JConnDriver) {
        createUniqueConstraint(neo4JConnDriver, "MOB_VERTEX_LABEL",
            "DOC_FIELD_QNAME");
        createUniqueConstraint(neo4JConnDriver, "METRIC_VERTEX_LABEL",
            "DOC_FIELD_QNAME");
        createUniqueConstraint(neo4JConnDriver, "METRIC_EDGE_LABEL",
            "DOC_FIELD_EDGE_ID");
    }


    /**
     * Adding unique constraint properties
     */
    public void createUniqueConstraint(Driver neo4JConnDriver, String label, String param) {
        try (Session session = neo4JConnDriver.session()) {
            final String payload =
                "CREATE CONSTRAINT ON (m:" + label + ") ASSERT m." + param + " IS UNIQUE";
            session.writeTransaction((TransactionWork<String>) tx -> {
                tx.run(payload);
                return null;
            });

        } catch (Exception ex) {
            //log.error("Exception while adding unique constraint to graph schema.");
        }
    }


}
