package org.java.aceis.io.streams.rdfox;

import com.csvreader.CsvReader;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.java.aceis.eventmodel.EventDeclaration;
import org.java.aceis.io.rdf.RDFFileManager;
import org.java.aceis.io.streams.DataWrapper;
import org.java.aceis.observations.AarhusParkingObservation;
import org.java.aceis.observations.SensorObservation;
import org.java.aceis.utils.RDFox.RDFoxWrapper;
import org.java.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RDFoxRandomStream extends RDFoxSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	EventDeclaration ed;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
	private Date startDate = null;
	private Date endDate = null;
	int count = 0;
	Random random = new Random();

	public RDFoxRandomStream(String uri, EventDeclaration ed) {
		super(uri);
		logger.info("init");
		this.ed = ed;
	}

	@Override
	public void run() {
		logger.info("Starting sensor stream: " + this.getIRI());
		try {
			while (!stop) {
				// logger.debug("Reading data: " + streamData.toString());
				AarhusParkingObservation po = (AarhusParkingObservation) this.createObservation();
				if(random.nextDouble() < (1 - ((double)sleep/1000))) {
					RDFoxWrapper.getRDFoxWrapper().flushIfNecessary(getIRI());
				}
				// logger.debug("Reading data: " + new Gson().toJson(po));
				List<Statement> stmts = this.getStatements(po);
				if(random.nextDouble() < (1 - ((double)sleep/1000))) {
					RDFoxWrapper.getRDFoxWrapper().flushIfNecessary(getIRI());
				}
				for (Statement st : stmts) {
					try {
						logger.debug(this.getIRI() + " Streaming: " + st.toString());
						//final RdfQuadruple q = new RdfQuadruple(st.getSubject().toString(), st.getPredicate()
						//		.toString(), st.getObject().toString(), System.currentTimeMillis());
						RDFoxWrapper.getRDFoxWrapper().putData(getIRI(), st);
						//logger.debug(this.getIRI() + " Streaming: " + q.toString());

					} catch (Exception e) {
						e.printStackTrace();
						logger.error(this.getIRI() + " CSPARQL streamming error.");
					}
					// messageByte += st.toString().getBytes().length;
				}
				RDFoxWrapper.getRDFoxWrapper().flushIfNecessary(getIRI());
				CityBench.pm.addNumberOfStreamedStatements(stmts.size());
				try {
					if (this.getRate() == 1.0)
						Thread.sleep(sleep);
				} catch (Exception e) {

					e.printStackTrace();
					this.stop();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unexpected thread termination");

		} finally {
			logger.info("Stream Terminated: " + this.getIRI());
			this.stop();
		}

	}

	@Override
	protected List<Statement> getStatements(SensorObservation so) throws NumberFormatException, IOException {
		Model m = ModelFactory.createDefaultModel();
		//Resource observation = m.createResource(RDFFileManager.defaultPrefix + so.getObId() + UUID.randomUUID());
		Resource observation = m.createResource(RDFFileManager.defaultPrefix + so.getObId());
		//System.out.println("OBS: " + observation.toString());
		CityBench.obMap.put(observation.toString(), so);
		observation.addProperty(RDF.type, m.createResource(RDFFileManager.ssnPrefix + "Observation"));
		// observation.addProperty(RDF.type,
		// m.createResource(RDFFileManager.saoPrefix + "StreamData"));
		Resource serviceID = m.createResource(ed.getServiceId());
		observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedBy"), serviceID);
		// Resource property = m.createResource(s.split("\\|")[2]);
		// property.addProperty(RDF.type, m.createResource(s.split("\\|")[0]));
		observation.addProperty(m.createProperty(RDFFileManager.ssnPrefix + "observedProperty"),
				m.createResource(ed.getPayloads().get(0).split("\\|")[2]));
		Property hasValue = m.createProperty(RDFFileManager.saoPrefix + "hasValue");
		// Literal l;
		// System.out.println("Annotating: " + observedProperty.toString());
		// if (observedProperty.contains("AvgSpeed"))
		observation.addLiteral(hasValue, ((AarhusParkingObservation) so).getVacancies());
		// observation.addLiteral(m.createProperty(RDFFileManager.ssnPrefix + "featureOfInterest"),
		// ((AarhusParkingObservation) so).getGarageCode());
		return m.listStatements().toList();
	}

	@Override
	protected SensorObservation createObservation(Object data) {
		try {
			// CsvReader streamData = (CsvReader) data;
			int vehicleCnt = Integer.parseInt("10"), id = Integer.parseInt("10"), total_spaces = Integer.parseInt("10");
			String garagecode = "";
			Date obTime = sdf.parse("2014-05-22 09:09:04.145");
			AarhusParkingObservation apo = new AarhusParkingObservation(total_spaces - vehicleCnt, garagecode, "", 0.0,
					0.0);
			apo.setObTimeStamp(obTime);
			// logger.info("Annotating obTime: " + obTime + " in ms: " + obTime.getTime());
			apo.setObId("AarhusParkingObservation-" + id);
			logger.debug(ed.getServiceId() + ": streaming record @" + apo.getObTimeStamp());
			DataWrapper.waitForInterval(this.currentObservation, apo, this.startDate, getRate());
			this.currentObservation = apo;
			return apo;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			logger.error("ed parse error: " + ed.getServiceId());
			e.printStackTrace();
		}
		return null;

	}

	protected SensorObservation createObservation() {
		try {
			// CsvReader streamData = (CsvReader) data;
			int vehicleCnt = Integer.parseInt("10"), id = Integer.parseInt(String.valueOf(count++)), total_spaces = Integer.parseInt("10");
			String garagecode = "";
			Date obTime = sdf.parse("2014-05-22 09:09:04.145");
			AarhusParkingObservation apo = new AarhusParkingObservation(total_spaces - vehicleCnt, garagecode, "", 0.0,
					0.0);
			apo.setObTimeStamp(obTime);
			// logger.info("Annotating obTime: " + obTime + " in ms: " + obTime.getTime());
			apo.setObId("AarhusParkingObservation-" + id);
			logger.debug(ed.getServiceId() + ": streaming record @" + apo.getObTimeStamp());
			DataWrapper.waitForInterval(this.currentObservation, apo, this.startDate, getRate());
			this.currentObservation = apo;
			return apo;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			logger.error("ed parse error: " + ed.getServiceId());
			e.printStackTrace();
		}
		return null;

	}

}
