package cloud;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A driver class that reads json data and runs graph builder.
 * @author Haoran Sun
 * @since 03-06-2018
 */
public class GraphBuilderRunner {
	public static void main(String[] args) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new 
					FileInputStream(args[0]), StandardCharsets.UTF_8))){
			String line = null;
			GraphBuilder gb = new GraphBuilder("bolt://localhost:7687",
				"neo4j", "sdsc123", "api.key");
			while((line = reader.readLine()) != null) {
				JSONObject json = new JSONObject(line);
				String docId = json.getString("id");
				String type = json.getString("case_type");
				int year = json.getInt("year");
				
				JSONArray facts = json.getJSONArray("facts");
				String text = "";
				for(int i = 0; i < facts.length(); i++) {
					text += facts.getString(i);
				}
				//TODO: call graph builder with those params
				
				JSONArray holding = json.getJSONArray("holding");
				text = "";
				for(int i = 0; i < holding.length(); i++) {
					text += holding.getString(i);
				}
				//TODO: call graph builder with those params
				gb.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}