package com.hyh.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TermServiceTest {
	
	@Autowired
	private TermService termService;

	private static final String DESKTOP_DIR = System.getProperty("user.home") + "/Desktop/";
	private static final String DATA_DIR = DESKTOP_DIR + "/tfidf-demo/";

	@Test
	public void testTfIdf() throws IOException {
		List<String> stopwords = Files.readAllLines(Paths.get(DATA_DIR + "stopwords.txt"));
		System.out.println("stopwords size=" + stopwords.size());
		System.out.println("-----------------------------------------");

		Map<String, Map<String, Integer>> normal = termService.normalTFOfAll(DATA_DIR + "/news", stopwords);
		normal.forEach((k, v) -> {
			System.out.println("fileName " + k);
			System.out.println("TF " + v.toString());
		});

		System.out.println("-----------------------------------------");

		Map<String, Map<String, Float>> notNarmal = termService.tfOfAll(DATA_DIR + "/news", stopwords);
		notNarmal.forEach((k, v) -> {
			System.out.println("fileName " + k);
			System.out.println("TF " + v.toString());
		});

		System.out.println("-----------------------------------------");

		Map<String, Float> idf = termService.idf(DATA_DIR + "/news", normal);
		idf.forEach((k, v) -> {
			System.out.println("keyword :" + k + " idf: " + v);
		});

		System.out.println("-----------------------------------------");

		Map<String, Map<String, Float>> tfidf = termService.tfidf(DATA_DIR + "/news", stopwords, normal);
		tfidf.forEach((k,v) -> {
			System.out.println("fileName " + k);
			System.out.println("tfidf " + v.toString());
		});

	}
}
