
//   javac -cp neo4j-java-driver-1.7.5.jar ex1.java
//   java -cp neo4j-java-driver-1.7.5.jar:. ex1
//
//  Simple program to create a node and query the results
//
//

import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class createAndQueryNode {

    	public static void main(String...args) {
        
	Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "password" ) );
	Session session = driver.session();

	session.run( "CREATE (a:Person {name: {name}, title: {title}})",
        				parameters( "name", "Arthur", "title", "King" ) );

	StatementResult result = session.run( "MATCH (a:Person) WHERE a.name = {name} " +
                                      "RETURN a.name AS name, a.title AS title", parameters( "name", "Arthur" ) );
	while ( result.hasNext() )
	{
    		Record record = result.next();
    		System.out.println( record.get( "title" ).asString() + " " + record.get( "name" ).asString() );
	}

	session.close();
	driver.close();

        }
    }
