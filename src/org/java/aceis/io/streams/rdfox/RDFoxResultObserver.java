package org.java.aceis.io.streams.rdfox;


import eu.larkc.csparql.engine.RDFStreamFormatter;
import org.java.aceis.observations.AarhusTrafficObservation;
import org.java.aceis.observations.SensorObservation;
import org.java.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.client.QueryAnswerMonitor;
import tech.oxfordsemantic.jrdfox.client.ResourceValue;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;
import tech.oxfordsemantic.jrdfox.logic.expression.IRI;
import tech.oxfordsemantic.jrdfox.logic.expression.Resource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RDFoxResultObserver extends RDFStreamFormatter implements QueryAnswerMonitor {
	private static final Logger logger = LoggerFactory.getLogger(RDFoxResultObserver.class);
	public static Set<String> capturedObIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public static Set<String> capturedResults = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public Set<Resource> localSet = new HashSet();
	private int cnt = 0;
	List<Integer> indexes = new ArrayList<Integer>();
	private boolean optimizedForHugeResults = false;

	public RDFoxResultObserver(String iri, String queryId) {
		super(iri);
		initializeIndexes(queryId);
	}

	@Override
	public void queryAnswersStarted(String[] strings) throws JRDFoxException {
		//logger.info("Hier sind wir angekommen1.");
		if(optimizedForHugeResults) {
			this.localSet.clear();
			this.cnt = 0;
		}
	}

	@Override
	//public void processQueryAnswer(List<Resource> list, long l) throws JRDFoxException {
	public void processQueryAnswer(List<ResourceValue> list, long l) throws JRDFoxException {
		if(optimizedForHugeResults) {
			++this.cnt;
			Iterator var4 = this.indexes.iterator();

			while (var4.hasNext()) {
				int i = (Integer) var4.next();
				this.localSet.add((Resource) list.get(i));
			}
		}
		else {
			Map<String, Long> latencies = new HashMap<String, Long>();
			int cnt = 0;
			//String result = Arrays.toString(list.toArray()).replaceAll("\t", " ").trim();
			//logger.info(result);
			//if (! capturedResults.contains(result)) {
			// logger.info(this.getIRI() + " Results: " + result);

			//capturedResults.add(result);

			cnt += 1;
			for (int i : indexes) {
				// String obid = t.get(i);
				String obid = "";
				try {
					//IRI iri = (IRI) list.get(i);
					//obid = iri.getIRI();
					obid = list.get(i).getLexicalForm();
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				//logger.info(obid);
				if (obid == null)
					logger.error("NULL ob Id detected.");
				if (capturedObIds.add(obid)) {
					// uncomment for testing the completeness, i.e., showing how many observations are captured
					//logger.info("RDFox result arrived " + capturedResults.size() + ", obs size: " + capturedObIds.size() + ", result: " + result);
					try {
						SensorObservation so = CityBench.obMap.get(obid);
						if (so == null) {
							logger.error("Cannot find observation for: " + obid);
						} else {
							long creationTime = so.getSysTimestamp().getTime();
							latencies.put(obid, (System.currentTimeMillis() - creationTime));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
			//}
			if (cnt > 0)
				CityBench.pm.addResults(getIRI(), latencies, cnt, capturedObIds.size());
		}
	}
	@Override
	public void queryAnswersFinished() throws JRDFoxException {
		if (optimizedForHugeResults) {
			Map<String, Long> latencies = new HashMap();

			String obid;
			for(Iterator var2 = this.localSet.iterator(); var2.hasNext(); capturedObIds.add(obid)) {
				Resource resource = (Resource)var2.next();
				IRI iri = (IRI)resource;
				obid = iri.getIRI();

				try {
					SensorObservation so = (SensorObservation)CityBench.obMap.get(obid);
					if (so == null) {
						logger.error("Cannot find observation for: " + obid);
					} else {
						long creationTime = so.getSysTimestamp().getTime();
						latencies.put(obid, System.currentTimeMillis() - creationTime);
					}
				} catch (Exception var9) {
					var9.printStackTrace();
				}
			}

			if (this.cnt > 0) {
				CityBench.pm.addResults(this.getIRI(), latencies, this.cnt, (long)capturedObIds.size());
			}
		}
		//logger.info("Hier sind wir angekommen3.");
	}

	public static void giveDifferences() {
		System.out.println("ObIds: " + capturedObIds.size() + ", ObMap: " + CityBench.obMap.size());
		for(String ob : CityBench.obMap.keySet()) {
			if((!capturedObIds.contains(ob)) && ob.contains("---")) {
				System.out.println("Not: " + ((SensorObservation)CityBench.obMap.get(ob)).toString());
			}
		}
		System.out.println("Current: " + System.currentTimeMillis());
	}

	private void initializeIndexes(String queryId) {
		if(queryId.equals("location_parking_1")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if(queryId.equals("pollution_weather_1")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if(queryId.equals("Q1") || queryId.equals("Q1_N3")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if(queryId.equals("Q1_20MB")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if(queryId.equals("Q1_30MB")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q2")) {
			indexes.add(0);
			indexes.add(1);
			indexes.add(2);
			indexes.add(3);
		}
		else if (queryId.equals("Q3")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q4")) {
			indexes.add(3);
		}
		else if (queryId.equals("Q5")) {
			indexes.add(4);
		}else if (queryId.equals("Q6")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q7")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q8")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q9")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q10")) {
			indexes.add(0);
			indexes.add(1);
		}
		else if (queryId.equals("Q10_5")) {
			indexes.add(0);
			indexes.add(1);
			indexes.add(2);
			indexes.add(3);
			indexes.add(4);
		}
		else if (queryId.equals("Q10_8")) {
			indexes.add(0);
			indexes.add(1);
			indexes.add(2);
			indexes.add(3);
			indexes.add(4);
			indexes.add(5);
			indexes.add(6);
			indexes.add(7);
		}
		else if (queryId.equals("Q11")) {

		}
		else if (queryId.equals("Q12")) {

		}
		else if (queryId.equals("Q_RAND")) {
			indexes.add(0);
		}
	}
}
