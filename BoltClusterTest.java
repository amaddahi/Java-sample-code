import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class BoltClusterTest {

    public static void main(String[] args) {

        java.util.Date startdate = new java.util.Date();

        // Provide URL to a Core Server

        //Driver driver = GraphDatabase.driver("bolt+routing://54.221.166.76:26000", Config.build().withMaxTransactionRetryTime(15, TimeUnit.SECONDS).toConfig());
        // Driver driver = GraphDatabase.driver("bolt+routing://localhost:7627", Config.build().withMaxTransactionRetryTime(15, TimeUnit.SECONDS).toConfig());
        Driver driver = GraphDatabase.driver("bolt+routing://localhost:7627", AuthTokens.basic("neo4j","password"), Config.build().withMaxTransactionRetryTime(15, TimeUnit.SECONDS).toConfig());

	// Driver driver = GraphDatabase.driver("bolt://localhost",AuthTokens.basic("neo4j","test"),noSSL);
	// Driver driver = GraphDatabase.driver("bolt://localhost:7687",AuthTokens.basic("neo4j","password"),noSSL);
	//Driver driver = GraphDatabase.driver("bolt+routing://localhost:7627",AuthTokens.basic("neo4j","password"));

        Session session;
        // Test 10 Write Transactions - new session each time:

        System.out.println("Using session.writeTransaction");

        for (int i = 0; i < 0; i++)
        {
            session = driver.session();

            session.writeTransaction(new TransactionWork<Transaction>() {
            @Override
            public Transaction execute(Transaction tx) {

                    StatementResult result =
                    tx.run("call dbms.listConfig() Yield name,value\n" +
                            "where name = 'dbms.connectors.default_listen_address'\n" +
                            "with value\n" +
                            "call dbms.cluster.role() Yield role\n" +
                            "return value as ServerIP, role as Role");

                    Record rec = result.single();

                    System.out.println("Role: " + rec.get(1) + " , Server IP: " + rec.get(0));

                    tx.success();
                    return null;
                }

            });

            session.close();

            }



        System.out.println("Using session.readTransaction");
        for (int i = 0; i < 0; i++)
        {
            session = driver.session();

            session.readTransaction(new TransactionWork<Transaction>() {
            @Override
            public Transaction execute(Transaction tx) {

                    StatementResult result =
                            tx.run("call dbms.listConfig() Yield name,value\n" +
                                    "where name = 'dbms.connectors.default_listen_address'\n" +
                                    "with value\n" +
                                    "call dbms.cluster.role() Yield role\n" +
                                    "return value as ServerIP, role as Role");

                    Record rec = result.single();


                    System.out.println("Role: " + rec.get(1) + " , Server IP: " + rec.get(0));

                tx.success();
                return null;
            }

            });

            session.close();

        }
        

        driver.close();

    }

}
