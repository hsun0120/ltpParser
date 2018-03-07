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
	static final String ROOT = "CREATE (n:Root {uid: %d, idx: -1, section: '%s', "
			+ "sentenceNum: %d})";
	/* Labels: postag, ner  */
	static final String WORDNODE = "MERGE (n:wordNode:%s:`%s` {uid: %d, idx: %d, "
			+ "text: '%s', section: '%s', sentenceNum: %d, docID: '%s', type: '%s', "
			+ "year: %d})";
	static final String NEXT_RELN = "MATCH (a {uid: %d}), (b {uid: %d}) MERGE "
			+ "(a)-[:NEXT]->(b)";
	static final String SRELN = "MATCH (a {uid: %d}), (b {uid: %d}) MERGE "
			+ "(a)-[:%s]->(b)"; //Syntactic or semantic relation
	
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
	 * @param docId - document ID
	 * @param type - court case type
	 * @param year - year of document
	 */
	public void ingest(String text, String section, String docId, String type, 
			int year) {
		if(text == null || text.length() == 0) return;
		
		ArrayList<String> parsed = this.parser.postRequest(text);
		int sentenceNum = 0;
		for(String json: parsed) { //Ingest each block of parsed text
			JSONArray depGraphs = new JSONArray(json);
			for(int i = 0; i < depGraphs.length(); i++) { //Iterate over paragraphs
				JSONArray paragraph = depGraphs.getJSONArray(i);
				for(int j = 0; j < paragraph.length(); j++) { //Iterate over a sentence
					JSONArray sentence = paragraph.getJSONArray(j);
					/* Storing node uid and its index in the sentence */
					HashMap<Integer, Integer> map = new HashMap<>();
					ArrayList<JSONObject> srl = new ArrayList<>();
					/* Iterate over a sentence */
					for(int k = 0; k < sentence.length(); k++) {
						JSONObject node = sentence.getJSONObject(k);
						this.createRelns(node, section, sentence, sentenceNum, docId, type, 
								year, map);
						if(node.getJSONArray("arg").length() > 0)
							srl.add(node);
					}
					this.createSRL(sentence, srl, section, sentenceNum);
					sentenceNum++;
				}
			}
		}
	}
	
	private void createRelns(JSONObject node, String section, JSONArray sentence,
			int sentenceNum, String docId, String type, int year, HashMap<Integer, 
			Integer> map) {
		try (Session session = driver.session()) {
			/* Create current node if not exists */
			if(!map.containsKey(new Integer(node.getInt("id")))) {
				session.run(String.format(WORDNODE, node.getString("pos"), 
						node.getString("ne"), uid, node.getInt("id"), 
						node.getString("cont"), section, sentenceNum, docId, type, year));
				map.put(node.getInt("id"), uid++);
			}
			
			if(node.getInt("id") == 0) { //Create dummy root node
				session.run(String.format(ROOT, uid, section, sentenceNum));
				session.run(String.format(NEXT_RELN, uid, map.get(new Integer(0))));
				map.put(-1, uid++);
			}
			
			if(node.getInt("id") < sentence.length() - 1) { //If not last node
				/* Create next node if not exists */
				JSONObject next = sentence.getJSONObject(node.getInt("id") + 1);
				if(!map.containsKey(new Integer(next.getInt("id")))) {
					session.run(String.format(WORDNODE, next.getString("pos"), 
							next.getString("ne"), uid, next.getInt("id"), 
							next.getString("cont"), section, sentenceNum, docId, type, 
							year));
					map.put(next.getInt("id"), uid++);
				}
				
				session.run(String.format(NEXT_RELN, map.get(new 
						Integer(node.getInt("id"))), map.get(new 
								Integer(next.getInt("id"))))); //Connect to next node
			}
			
			/* Syntactic relation */
			this.createSRlen(sentence, node, "parent", "relate", section, 
					sentenceNum, map, session, docId, type, year);
			
			/* Semantic relation */
			this.createSRlen(sentence, node, "semparent", "semrelate", section, 
					sentenceNum, map, session, docId, type, year);
		}
	}
	
	//TODO
	private void createSRL(JSONArray sentence, ArrayList<JSONObject> srl, 
			String section, int sentenceNum) {
		
	}
	
	private void createSRlen(JSONArray sentence, JSONObject node, String 
			parentField, String relateField, String section, int sentenceNum, 
			HashMap<Integer, Integer> map, Session session, String docId, String 
			type, int year) {
		if(node.getInt(parentField) == -1) {
			session.run(String.format(SRELN, map.get(new Integer(-1)), map.get(new 
					Integer(node.getInt("id"))), node.getString(relateField)));
			return;
		}
		
		JSONObject parent = sentence.getJSONObject(node.getInt(parentField));
		/* Create parent node if not exists */
		if(!map.containsKey(new Integer(parent.getInt("id")))) {
			session.run(String.format(WORDNODE, parent.getString("pos"), 
					parent.getString("ne"), uid, parent.getInt("id"), 
					parent.getString("cont"), section, sentenceNum, docId, type, year));
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
	
	public static void main(String args[]) {
		try (GraphBuilder builder = new GraphBuilder("bolt://localhost:7687",
				"neo4j", "sdsc123", "api.key")){
			builder.ingest("巴希尔强调，政府坚决主张通过和平和政治途径结束目前的武装冲突，在全国实现和平。"
				+ "他强烈呼吁以约翰·加朗为首的反政府武装力量回到国家的怀抱。在谈到周边关系时，巴希尔说，苏丹政府将采取行动改善与周边国家的关系。", 
				"news", "001", "poli", 2010);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}