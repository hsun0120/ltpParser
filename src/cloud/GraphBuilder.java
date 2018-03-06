package cloud;

import java.util.ArrayList;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

public class GraphBuilder implements AutoCloseable{
	static final int MAX_LENGTH = 5000;
	
	private final Driver driver;
	private CloudParser parser;
	
	public GraphBuilder(String uri, String username, String password, String
			path) {
		this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username,
				password));
		this.parser = new CloudParser();
		this.parser.importKey(path);
	}
	
	public ArrayList<String> preprocess(String text) {
		ArrayList<String> parsed = new ArrayList<>();
		if(text.length() > MAX_LENGTH) {
			String[] sentences = text.split("(?<=¡£)|(?<=£»)|(?<=£¿)");
			StringBuilder buf = new StringBuilder();
			int count = 0;
			for(int i = 0; i < sentences.length; i++) {
				if(count + sentences[i].length() <= MAX_LENGTH) { //Check length
					count += sentences.length;
					buf.append(sentences[i]);
				} else {
					/* Add parsing output */
					parsed.add(this.parser.postRequest(buf.toString()));
					count = sentences[i].length();
					buf = new StringBuilder(); //Clear buffer
					buf.append(sentences[i]);
				}
			}
		} else
			parsed.add(this.parser.postRequest(text));
		return parsed;
	}
	
	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}