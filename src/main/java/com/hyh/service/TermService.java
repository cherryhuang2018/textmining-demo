package com.hyh.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("TermService")
public class TermService {

	private static final Logger logger = LoggerFactory.getLogger(TermService.class);

	public List<Path> readPaths(String folderPath, String format) {
		List<Path> pathList = null;
		try (Stream<Path> pathStream = Files.find(Paths.get(folderPath), 5,
				(path, attr) -> path.toString().indexOf(format) > -1)) {
			pathList = pathStream.collect(Collectors.toList());
		} catch (IOException e) {
			logger.error("Read Folder Failed, msg={}", e.getMessage(), e);
		}
		return pathList;
	}

	public String readFiles(Path path) {
		StringBuilder sb = new StringBuilder();
		try {
			Files.readAllLines(path).forEach(line -> {
				sb.append(line).append("\r\n");
			});
		} catch (IOException e) {
			logger.error("Read File Failed, msg={}", e.getMessage(), e);
		}
		return sb.toString();
	}

	/**
	 * 斷詞並移除停用字
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public String[] cutWord(Path path, List<String> stopWords) {
		String article = this.readFiles(path);

		List<String> cutwords = new ArrayList<>();
		for (String word : article.split(" ")) {
			if (stopWords.indexOf(word) > -1) {
				continue;
			}
			cutwords.add(word);
		}
		return cutwords.stream().toArray(String[]::new);
	}

	/**
	 * 正規化
	 * 
	 * @param cutWords
	 * @return
	 */
	public Map<String, Float> tf(String[] cutWords) {

		Map<String, Integer> tfNormalMap = new HashMap<String, Integer>();
		for (int i = 0; i < cutWords.length; i++) {
			String word = cutWords[i];
			Integer count = tfNormalMap.getOrDefault(word, 0) + 1;
			tfNormalMap.put(word, count);
		}

		Map<String, Float> tfMap = new HashMap<String, Float>();
		for (Entry<String, Integer> entry : tfNormalMap.entrySet()) {
			tfMap.put(entry.getKey(), (new Float(entry.getValue())) / cutWords.length);
		}
		return tfMap;
	}

	/**
	 * 沒有正規化
	 * 
	 * @param cutWordResult
	 * @return
	 */
	public Map<String, Integer> normalTF(String[] cutWords) {
		Map<String, Integer> tfNormalMap = new HashMap<String, Integer>();

		for (int i = 0; i < cutWords.length; i++) {
			String word = cutWords[i];
			Integer count = tfNormalMap.getOrDefault(word, 0) + 1;
			tfNormalMap.put(word, count);
		}
		return tfNormalMap;
	}

	public Map<String, Map<String, Float>> tfOfAll(String dir, List<String> stopWords) {
		Map<String, Map<String, Float>> allTheTf = new HashMap<>();
		readPaths(dir, ".txt").forEach(path -> {
			Map<String, Float> dict = this.tf(cutWord(path, stopWords));
			allTheTf.put(path.toString(), dict);
		});
		return allTheTf;
	}

	public Map<String, Map<String, Integer>> normalTFOfAll(String dir, List<String> stopWords) {
		Map<String, Map<String, Integer>> allTheNormalTF = new HashMap<>();
		readPaths(dir, ".txt").forEach(path -> {
			Map<String, Integer> dict = this.normalTF(cutWord(path, stopWords));
			allTheNormalTF.put(path.toString(), dict);
		});
		return allTheNormalTF;
	}

	public Map<String, Float> idf(String dir, Map<String, Map<String, Integer>> tfMap) {
		// 公式IDF＝log((1+|D|)/|Dt|)
		// D為文章總數
		// Dt為含關鍵詞t的文章數
		Map<String, Float> idf = new HashMap<String, Float>();
		List<String> located = new ArrayList<String>();

		float Dt = 1;
		float D = tfMap.size();
		List<Path> key = readPaths(dir, ".txt");

		for (int i = 0; i < D; i++) {
			Map<String, Integer> temp = tfMap.get(key.get(i).toString());
			for (String word : temp.keySet()) {
				Dt = 1;
				if (located.contains(word)) {
					continue;
				}

				for (int k = 0; k < D; k++) {
					if (k != i) {
						Map<String, Integer> temp2 = tfMap.get(key.get(k).toString());
						if (temp2.containsKey(word)) {
							located.add(word);
							Dt = Dt + 1;
							continue;
						}
					}
				}
				idf.put(word, this.log((1 + D) / Dt, 10));
			}
		}
		return idf;
	}

	public Map<String, Map<String, Float>> tfidf(String dir, List<String> stopwords,
			Map<String, Map<String, Integer>> tfMap) {
		Map<String, Float> idf = this.idf(dir, tfMap);
		Map<String, Map<String, Float>> tf = this.tfOfAll(dir, stopwords);

		for (String file : tf.keySet()) {
			Map<String, Float> singelFile = tf.get(file);
			for (String word : singelFile.keySet()) {
				singelFile.put(word, (idf.get(word)) * singelFile.get(word));
			}
		}
		return tf;
	}

	public float log(float value, float base) {
		return (float) (Math.log(value) / Math.log(base));
	}
}
