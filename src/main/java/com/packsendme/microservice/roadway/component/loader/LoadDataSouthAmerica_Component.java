package com.packsendme.microservice.roadway.component.loader;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.packsendme.exchange.bre.model.ExchangeBRE_Model;
import com.packsendme.financecostdelivery.bre.model.FinanceCostDeliveryBRE_Model;
import com.packsendme.lib.common.response.dto.api.GoogleAPITrackingResponse_Dto;
import com.packsendme.lib.simulation.http.SimulationDataForCalculateRequest_Dto;
import com.packsendme.lib.simulation.http.SimulationRequest_Dto;
import com.packsendme.microservice.roadway.component.parser.ResponseParser_SouthAmerica;
import com.packsendme.microservice.roadway.config.Cache_Config;
import com.packsendme.microservice.roadway.controller.IBusinessManager_SA_Client;
import com.packsendme.microservice.roadway.controller.IExchangeRate_Client;
import com.packsendme.microservice.roadway.controller.IGoogleAPI_Client;
import com.packsendme.microservice.roadway.dto.LoadDataSouthAmerica_Dto;
import com.packsendme.roadway.bre.rule.model.RoadwayBRE_Model;

@Component
@ComponentScan("com.packsendme.microservice.roadway")
public class LoadDataSouthAmerica_Component {
	
	@Autowired
	private IGoogleAPI_Client googleClient;

	@Autowired
	private IBusinessManager_SA_Client businessRule_Client;
	
	@Autowired
	private IExchangeRate_Client exchangeRate_Client;
	
	@Autowired
	private ResponseParser_SouthAmerica responseParserSA;
	
	
	@Autowired
	private Cache_Config cache;

	
	public LoadDataSouthAmerica_Dto getDataSouthAmerica(SimulationRequest_Dto simulationData, Map isoInformation) {
		SimulationDataForCalculateRequest_Dto simulationReqCustomer_dto = null;
		
		//(1) Instance Google-API
		ResponseEntity<?> googleAPIResponse = googleClient.getTracking(simulationData);
		GoogleAPITrackingResponse_Dto simulationGoogleAPI = responseParserSA.getParseRoadwayResponseAPI(googleAPIResponse);

		//(2) Instance Roadway-Cache  BusinessManager/Rule
		ResponseEntity<?> roadayCacheResponse = businessRule_Client.getRoadwayBRE_SA(cache.roadwayBRE_SA);
		RoadwayBRE_Model roadwayBRE = responseParserSA.getParseRoadwayResponseCache(roadayCacheResponse);
		
		//(2.1) Instance PackSendPercentage-Cache  BusinessManager/Rule
		ResponseEntity<?> financeCacheResponse = businessRule_Client.getFinanceCostDeliveryBRE_SA(cache.financeCostDeliveryBRE_SA);
		FinanceCostDeliveryBRE_Model packSendPercentage = responseParserSA.getParseFinanceCostDeliveryResponseCache(financeCacheResponse);

		// (3) Instance RateExchange-API
		ResponseEntity<?> exchangeResponse = exchangeRate_Client.getExchange(isoInformation.get("isoCurrencyCode").toString());
		ExchangeBRE_Model exchangeBRE = responseParserSA.getParseExchangeResponseCache(exchangeResponse);

		simulationReqCustomer_dto = new SimulationDataForCalculateRequest_Dto(
				simulationData.weight_product, 
				simulationData.unity_measurement_weight, 
				simulationData.type_delivery, 
				isoInformation.get("isoLanguageCode").toString(), 
				isoInformation.get("isoCountryCode").toString(), 
				exchangeBRE.value, 
				packSendPercentage.percentage_packsend, 
				roadwayBRE);
		
		LoadDataSouthAmerica_Dto loadDataSouthAmerica_Dto = new LoadDataSouthAmerica_Dto(simulationGoogleAPI,simulationReqCustomer_dto);
		return loadDataSouthAmerica_Dto;
	}

}
