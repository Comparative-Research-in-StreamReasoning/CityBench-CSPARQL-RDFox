package org.java.aceis.utils.test;

import au.com.bytecode.opencsv.CSVReader;
import com.csvreader.CsvWriter;
import org.java.aceis.io.streams.cqels.CQELSSensorStream;
import org.java.aceis.io.streams.csparql.CSPARQLSensorStream;
import org.java.aceis.io.streams.rdfox.RDFoxResultObserver;
import org.java.aceis.io.streams.rdfox.RDFoxSensorStream;
import org.java.aceis.utils.RDFox.NamedQuery;
import org.java.aceis.utils.RDFox.RDFoxWrapper;
import org.java.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.oxfordsemantic.jrdfox.client.ComponentInfo;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PerformanceMonitor implements Runnable {
	private Map<String, String> qMap;
	private long duration;
	private int duplicates;
	private String resultName;
	private long start = 0;
	private ConcurrentHashMap<String, Set<Long>> latencyMap = new ConcurrentHashMap<>();
	private List<Double> memoryList = new ArrayList<Double>();;
	private ConcurrentHashMap<String, Long> resultCntMap = new ConcurrentHashMap<String, Long>();
	private CsvWriter cw;
	private long resultInitTime = 0, lastCheckPoint = 0, globalInit = 0;
	private boolean stop = false;
	private List<String> qList;
	private long currentObIds = 0;
	public long streamedStatementInLastSecond = 0;
	public Queue<Long> streamedStatementsPerSecond = new ConcurrentLinkedQueue<>();
	private boolean throughputMeasurerRunning = false;
	private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
	private File outputFile;
	private ServerConnection sCon;
	private boolean isRDFox;
	private boolean streamsStopped = false;

	public PerformanceMonitor(Map<String, String> queryMap, long duration, int duplicates, String resultName)
			throws Exception {
		qMap = queryMap;
		this.duration = duration;
		this.resultName = resultName;
		this.duplicates = duplicates;
		outputFile = new File("result_log" + File.separator + resultName + ".csv");
		if (outputFile.exists())
			throw new Exception("Result log file already exists.");
		cw = new CsvWriter(new FileWriter(outputFile, true), ',');
		cw.write("Benchmark");
		qList = new ArrayList(this.qMap.keySet());
		Collections.sort(qList);

		for (String qid : qList) {
			latencyMap.put(qid, Collections.newSetFromMap(new ConcurrentHashMap<>()));
			resultCntMap.put(qid, (long) 0);
			cw.write("latency-" + qid);
		}
		// for (String qid : qList) {
		// cw.write("cnt-" + qid);
		// }
		cw.write("memory");
		cw.write("ObIds");
		cw.write("Completeness");
		cw.write("Current Throughput");
		cw.endRecord();
		// cw.flush();
		// cw.
		this.globalInit = System.currentTimeMillis();
	}

	public void run() {
		int minuteCnt = 0;
		while (!stop) {
			try {
				if (((System.currentTimeMillis() - this.globalInit) > 1.5 * duration)
						|| (duration != 0 && resultInitTime != 0 && (System.currentTimeMillis() - this.resultInitTime) > (30000 + duration))) {
					if(!streamsStopped) {
						stopStreams();
						Thread.sleep(2000);
						streamsStopped = true;
					}
					else {
						setLastEntry();
						this.cw.flush();
						this.cw.close();
						logger.info("Stopping after " + (System.currentTimeMillis() - this.globalInit) + " ms.");
						this.cleanup();
						RDFoxResultObserver.giveDifferences();
						logger.info("Experimment stopped.");
						System.exit(0);
					}
				}

				if (this.lastCheckPoint != 0 && (System.currentTimeMillis() - this.lastCheckPoint) >= 60000) {
					minuteCnt += 1;

					this.lastCheckPoint = System.currentTimeMillis();
					cw.write(minuteCnt + "");
					for (String qid : this.qList) {
						double latency = 0.0;
						for (long l : this.latencyMap.get(qid))
							latency += l;
						latency = (latency + 0.0) / (this.latencyMap.get(qid).size() + 0.0);
						cw.write(latency + "");

					}
					// for (String qid : this.qList)
					// cw.write((this.resultCntMap.get(qid) / (this.duplicates + 0.0)) + "");
					double memory = 0.0;
					for (double m : this.memoryList)
						memory += m;
					memory = memory / (this.memoryList.size() + 0.0);
					cw.write(memory + "");
					cw.write(currentObIds + "");
					cw.write( (CityBench.obMap.size() != 0 ? (float)currentObIds/CityBench.obMap.size() : 0) + "");
					cw.write((streamedStatementsPerSecond.size() > 0 ? streamedStatementsPerSecond.stream().mapToLong(c -> c).average().getAsDouble() : 0) + "");
					streamedStatementsPerSecond.clear();
					cw.endRecord();
					cw.flush();
					logger.info("Results logged.");

					// empty memory and latency lists
					this.memoryList.clear();
					for (Entry<String, Set<Long>> en : this.latencyMap.entrySet()) {
						en.getValue().clear();
					}
				}

				Map<String, Double> currentLatency = new HashMap<String, Double>();
				for (String qid : this.qList) {
					double latency = 0.0;
					for (long l : this.latencyMap.get(qid))
						latency += l;
					latency = (latency + 0.0) / (this.latencyMap.get(qid).size() + 0.0);
					currentLatency.put(qid, latency);
				}

				// Map<String,Long> currentResults=new HashMap<String>

				// ConcurrentHashMap<String, SensorObservation> obMapBytes = CityBench.obMap;
				// double obMapBytes = 0.0;
				// try {
				// ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// ObjectOutputStream oos = new ObjectOutputStream(baos);
				// oos.writeObject(CityBench.obMap);
				// oos.close();
				// obMapBytes = (0.0 + baos.size());
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// long listerObIdListBytes = 0;
				// for (Object listener : CityBench.registeredQueries.values()) {
				//
				// if (listener instanceof CQELSResultListener) {
				// for (String obid : ((CQELSResultListener) listener).capturedObIds)
				// listerObIdListBytes += obid.getBytes().length;
				// } else {
				// for (String obid : ((CSPARQLResultObserver) listener).capturedObIds)
				// listerObIdListBytes += obid.getBytes().length;
				// }
				// }
				// long listenerResultListBytes = 0;
				// for (Object listener : CityBench.registeredQueries.values()) {
				//
				// if (listener instanceof CQELSResultListener) {
				// for (String result : ((CQELSResultListener) listener).capturedResults)
				// listenerResultListBytes += result.getBytes().length;
				// } else {
				// for (String result : ((CSPARQLResultObserver) listener).capturedResults)
				// listenerResultListBytes += result.getBytes().length;
				// }
				// }
				System.gc();
				Runtime rt = Runtime.getRuntime();
				double usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0;
				if(isRDFox) {
					double serverSize;
					try(DataStoreConnection dCon = sCon.newDataStoreConnection(RDFoxWrapper.datastoreName)) {
						ComponentInfo ci = dCon.getComponentInfo(true);
						serverSize = Double.valueOf ((Long) ci.getPropertyValues().get("Aggregate size")) / 1024.0 / 1024.0;
						usedMB += serverSize;
//						for(String s : ci.getPropertyValues().keySet()) {
//							System.out.print("	" + s + ": " + ci.getPropertyValues().get(s));
//						}
//						System.out.println("\n\n" + (double)ci.getPropertyValues().get("Dictionary size (%)"));
						if((double)ci.getPropertyValues().get("Dictionary size (%)") > 50) {
							/*System.out.println("Data Sources: ");
							dCon.listDataSources().stream().forEach(System.out::println);
							System.out.println("Rules: ");
							dCon.listRules().stream().forEach(System.out::println);
							System.out.println("Tuple Tables: ");
							for(TupleTableInfo t : dCon.listTupleTables()) {
								System.out.println(t.getName() + ", " + t.getID() + ", " + t.getParameters());
							}
							OutputStream out = new FileOutputStream("Export.txt");
							dCon.exportData(Prefixes.s_emptyPrefixes, out, "application/n-triples", new HashMap<>());
							out.close();*/
						}
						logger.info("Aggregate size: " + ci.getPropertyValues().get("Aggregate size") + ", Aggregate number of entries: " + ci.getPropertyValues().get("Aggregate number of entries") + ", Bytes per entry: " + ci.getPropertyValues().get("Bytes per entry") + ", Aggregate number of IDB facts: " + ci.getPropertyValues().get("Aggregate number of IDB facts") + ", Dictionary size (%): " + ci.getPropertyValues().get("Dictionary size (%)"));
						if((double)ci.getPropertyValues().get("Dictionary size (%)") > 53) {
							RDFoxWrapper.getRDFoxWrapper().createNewDatastore();
						}
					}
				}
				// double overhead = (obMapBytes + listerObIdListBytes + listenerResultListBytes) / 1024.0 / 1024.0;
				this.memoryList.add(usedMB);
				/*logger.info("Current performance: L - " + currentLatency + ", Cnt: " + this.resultCntMap + ", Mem - "
						+ usedMB + ", ObIds - " + currentObIds + ", Completeness: " + (CityBench.obMap.size() != 0 ? (float)currentObIds/CityBench.obMap.size() : 0)
						+ ", Current Throughput: " + (streamedStatementsPerSecond.size() > 0 ? streamedStatementsPerSecond.stream().mapToLong(c -> c).average().getAsDouble() : 0));// + ", monitoring overhead - " + overhead);*/
				System.out.println(("Current performance: L - " + currentLatency + ", Cnt: " + this.resultCntMap + ", Mem - "
						+ usedMB + ", ObIds - " + currentObIds + ", Completeness: " + (CityBench.obMap.size() != 0 ? (float)currentObIds/CityBench.obMap.size() : 0)
						+ ", Current Throughput: " + (streamedStatementsPerSecond.size() > 0 ? streamedStatementsPerSecond.stream().mapToLong(c -> c).average().getAsDouble() : 0)));
				//System.out.println(latencyMap.toString());
				Thread.sleep(5000);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JRDFoxException e) {
				e.printStackTrace();
			}
		}
		System.exit(0);
	}

	private void stopStreams() {
		if (CityBench.csparqlEngine != null) {
			// CityBench.csparqlEngine.destroy();
			for (Object css : CityBench.startedStreamObjects) {
				((CSPARQLSensorStream) css).stop();
			}
		} else if(CityBench.cqelsContext != null) {
			// CityBench.cqelsContext.engine().
			for (Object css : CityBench.startedStreamObjects) {
				((CQELSSensorStream) css).stop();
			}
		} else if (CityBench.rdFoxWrapper != null) {
			for (Object css : CityBench.startedStreamObjects) {
				((RDFoxSensorStream) css).stop();
			}
		}
	}
	private void cleanup() {
		if (CityBench.rdFoxWrapper != null) {
			try {
				for (NamedQuery query : RDFoxWrapper.getRDFoxWrapper().getQueries().values()) {
					query.stop();
				}
				Thread.sleep(2000);
				RDFoxWrapper.getRDFoxWrapper().getServerConnection().close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ThroughputMeasurer.stop();
		this.stop = true;
		System.gc();

	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public synchronized void addResults(String qid, Map<String, Long> results, int cnt) {
		if (this.resultInitTime == 0) {
			this.resultInitTime = System.currentTimeMillis();
			this.lastCheckPoint = System.currentTimeMillis();
		}
		qid = qid.split("-")[0];
		for (Entry en : results.entrySet()) {
			String obid = en.getKey().toString();
			long delay = (long) en.getValue();
			this.latencyMap.get(qid).add(delay);
		}
		this.resultCntMap.put(qid, this.resultCntMap.get(qid) + cnt);
	}

	public synchronized void addResults(String qid, Map<String, Long> results, int cnt, long capturedObIds) {
		if (this.resultInitTime == 0) {
			this.resultInitTime = System.currentTimeMillis();
			this.lastCheckPoint = System.currentTimeMillis();
		}
		qid = qid.split("-")[0];
		for (Entry en : results.entrySet()) {
			String obid = en.getKey().toString();
			long delay = (long) en.getValue();
			this.latencyMap.get(qid).add(delay);
		}
		this.resultCntMap.put(qid, this.resultCntMap.get(qid) + cnt);
		currentObIds = capturedObIds;
	}

	public synchronized void addNumberOfStreamedStatements(int streamStatements) {
		if(!throughputMeasurerRunning) {
			throughputMeasurerRunning = true;
			new Thread(new ThroughputMeasurer(this)).start();
		}
		streamedStatementInLastSecond += streamStatements;
	}

	private void setLastEntry() {
		try {
			this.lastCheckPoint = System.currentTimeMillis();
			cw.write("Final Results");
			for (String qid : this.qList) {
				double latency = 0.0;
				for (long l : this.latencyMap.get(qid))
					latency += l;
				latency = (latency + 0.0) / (this.latencyMap.get(qid).size() + 0.0);
				cw.write(latency + "");

			}
			// for (String qid : this.qList)
			// cw.write((this.resultCntMap.get(qid) / (this.duplicates + 0.0)) + "");
			double memory = 0.0;
			for (double m : this.memoryList)
				memory += m;
			memory = memory / (this.memoryList.size() + 0.0);
			cw.write(memory + "");
			cw.write(currentObIds + "");
			cw.write( (CityBench.obMap.size() != 0 ? (float)currentObIds/CityBench.obMap.size() : 0) + "");
			cw.write((streamedStatementsPerSecond.size() > 0 ? streamedStatementsPerSecond.stream().mapToLong(c -> c).average().getAsDouble() : 0) + "");
			streamedStatementsPerSecond.clear();
			cw.endRecord();
			cw.flush();
			calculateAverages();
			logger.info("Results logged.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void calculateAverages() throws IOException {
		CSVReader reader = new CSVReader(new FileReader(outputFile));
		List<String []> result = reader.readAll();
		result.remove(0);
		int latencyAverage = (int) result.stream().filter(c -> !c[1].equals("NaN")).mapToDouble(c -> Double.parseDouble(c[1])).average().getAsDouble();
		int memoryAverage = (int) result.stream().mapToDouble(c -> Double.parseDouble(c[2])).average().getAsDouble();
		double completeness = Double.parseDouble(result.get(result.size()-1)[4])/0.2;
		cw.endRecord();
		cw.write("AVERAGES");
		cw.write(latencyAverage+"");
		cw.write(memoryAverage+"");
		cw.write("");
		cw.write(completeness+"");
		cw.endRecord();
		cw.flush();
	}

	public void setSCon(ServerConnection sCon) {
		this.isRDFox = true;
		this.sCon = sCon;
	}
}
