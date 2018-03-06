package cloud;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

/**
 * A class that ingests json output produced by ltp-cloud parser and constructs
 * dependency graph in neo4j.
 * @author Haoran Sun
 * @since 03-05-2018
 */
public class GraphBuilder implements AutoCloseable{
	static final int MAX_LENGTH = 5000;
	static final String WORDNODE = "MERGE (n:wordNode:%s:%s:%s {uid: %d, idx: %d"
			+ ", text: '%s', sentenceNum: %d})"; //Labels: section, postag, ner 
	static final String NEXT_RELN = "MATCH (a:wordNode {uid: %d}), (b:wordNode "
			+ "{uid: %d}) MERGE (a)-[:NEXT]->(b)";
	static final String NEW_LABEL = "MATCH (n:wordNode {uid %d}) set n: ROOT";
	static final String SRELN = "MATCH (a:wordNode {uid: %d}), (b:wordNode "
			+ "{uid: %d}) MERGE (a)-[:%s]->(b)"; //Syntactic or semantic relation
	
	private final Driver driver;
	private CloudParser parser;
	private int uid;
	
	/**
	 * Constructor.
	 * @param uri - uri of the neo4j server
	 * @param username - neo4j database username
	 * @param password - neo4j database password
	 * @param path - path of the ltp-cloud api key
	 */
	public GraphBuilder(String uri, String username, String password, String
			path) {
		this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username,
				password));
		this.parser = new CloudParser();
		this.parser.importKey(path);
		this.uid = 0;
	}
	
	/**
	 * Parse and injest text by section.
	 * @param text - text to parse
	 * @param section - section of the document
	 */
	public void ingest(String text, String section) {
		if(text == null || text.length() == 0) return;
		
		ArrayList<String> parsed = this.preprocess(text);
		int sentenceNum = 0;
		for(String json: parsed) { //Ingest each block of parsed text
			JSONArray depGraphs = new JSONArray(json);
			for(int i = 0; i < depGraphs.length(); i++) { //Assuming sentence level
				JSONArray sentence = depGraphs.getJSONArray(i);
				/* Storing node uid and its index in the sentence */
				HashMap<Integer, Integer> map = new HashMap<>();
				ArrayList<JSONObject> srl = new ArrayList<>();
				for(int j = 0; j < sentence.length(); j++) {
					JSONObject node = sentence.getJSONObject(j);
					this.createRelns(node, section, sentence, sentenceNum, map);
					if(node.getJSONArray("arg").length() > 0)
						srl.add(node);
				}
				this.createSRL(sentence, srl, section, sentenceNum);
				sentenceNum++;
			}
		}
	}
	
	private ArrayList<String> preprocess(String text) {
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
	
	private void createRelns(JSONObject node, String section, JSONArray sentence,
			int sentenceNum, HashMap<Integer, Integer> map) {
		try (Session session = driver.session()) {
			/* Create current node if not exists */
			if(!map.containsKey(new Integer(node.getInt("id")))) {
				session.run(String.format(WORDNODE, section, node.getString("pos"), 
						node.getString("ne"), uid, node.getInt("id"), 
						node.getString("cont"), sentenceNum));
				map.put(node.getInt("id"), uid++);
			}
			
			if(node.getInt("id") < sentence.length() - 1) { //If not last node
				/* Create next node if not exists */
				JSONObject next = sentence.getJSONObject(node.getInt("id") + 1);
				if(!map.containsKey(new Integer(next.getInt("id")))) {
					session.run(String.format(WORDNODE, section, next.getString("pos"), 
							node.getString("ne"), uid, next.getInt("id"), 
							node.getString("cont"), sentenceNum));
					map.put(next.getInt("id"), uid++);
				}
				
				session.run(String.format(NEXT_RELN, map.get(new 
						Integer(node.getInt("id"))), map.get(new 
								Integer(next.getInt("id"))))); //Connect to next node
			}
			
			/* Syntactic relation */
			if(node.getInt("parent") == -1) //If HED or ROOT, just add a label
				session.run(String.format(NEW_LABEL, map.get(new 
						Integer(node.getInt("id")).intValue())));
			else if(!map.containsKey(node.getInt("parent")))
				this.createSRlen(sentence, node, "parent", "relate", section, 
						sentenceNum, map, session);
			
			/* Semantic relation */
			if(node.getInt("semparent") != -1 && 
					!map.containsKey(node.getInt("semparent")))
				this.createSRlen(sentence, node, "semparent", "semrelate", section, 
						sentenceNum, map, session);
		}
	}
	
	//TODO
	private void createSRL(JSONArray sentence, ArrayList<JSONObject> srl, 
			String section, int sentenceNum) {
		
	}
	
	private void createSRlen(JSONArray sentence, JSONObject node, String 
			parentField, String relateField, String section, int sentenceNum, 
			HashMap<Integer, Integer> map, Session session) {
		JSONObject parent = sentence.getJSONObject(node.getInt(parentField));
		/* Create parent node if not exists */
		if(!map.containsKey(new Integer(parent.getInt("id")))) {
			session.run(String.format(WORDNODE, section, parent.getString("pos"), 
					node.getString("ne"), uid, parent.getInt("id"), 
					node.getString("cont"), sentenceNum));
			map.put(parent.getInt("id"), uid++);
		}
		
		session.run(String.format(SRELN, map.get(new 
				Integer(parent.getInt("id"))), map.get(new 
						Integer(node.getInt("id"))), node.getString(relateField)));
	}
	
	@Override
	public void close() throws Exception {
		this.driver.close();
	}
	
}