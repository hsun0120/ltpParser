package ltp4j;
import java.util.ArrayList;
import java.util.List;

import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Pair;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.SRL;
import edu.hit.ir.ltp4j.Segmentor;

/**
 * A demo for ltp4j.
 * @author Haoran Sun
 * @deprecated
 */
public class LTPParser {
	public static void main(String[] args) {
		Segmentor segmentor = new Segmentor();
		if(segmentor.create("ltp_data_v3.4.0/cws.model")<0){
			System.err.println("load failed");
			return;
		}

		String sent = "我是中国人";
		List<String> words = new ArrayList<String>();
		int size = segmentor.segment(sent,words);
		segmentor.release();

		Postagger postag = new Postagger();
		postag.create("ltp_data_v3.4.0/pos.model");
		List<String> tags = new ArrayList<String>();
		postag.postag(words, tags);
		postag.release();
		for(int i = 0; i < size; i++) {
			System.out.print(words.get(i)+ "_" + tags.get(i));
			if(i==size-1) {
				System.out.println();
			} else {
				System.out.print("|");
			}
		}

		NER ner = new NER();
		ner.create("ltp_data_v3.4.0/ner.model");
		List<String> ners = new ArrayList<String>();
		ner.recognize(words, tags, ners);

		Parser parser = new Parser();
		parser.create("ltp_data_v3.4.0/parser.model");
		List<Integer> heads = new ArrayList<>();
		List<String> deprels = new ArrayList<>();
		parser.parse(words, tags, heads, deprels);

		for(int i=0; i<heads.size(); i++){
			System.out.println(heads.get(i));
			System.out.println(deprels.get(i));
			System.out.println(ners.get(i));
		}
		parser.release();

		SRL srl = new SRL();
		srl.create("ltp_data_v3.4.0/srl");
		List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = 
				new ArrayList<>();
		srl.srl(words, tags, ners, heads, deprels, srls);
		srl.release();
		for(int i = 0; i < srls.size(); i++) {
			System.out.println(srls.get(i));
		}
	}
}