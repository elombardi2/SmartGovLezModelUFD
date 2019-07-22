package com.smartgov.lez.input.establishment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.smartgov.lez.core.agent.driver.vehicle.DeliveryVehicle;
import com.smartgov.lez.core.agent.driver.vehicle.DeliveryVehicleFactory;
import com.smartgov.lez.core.agent.establishment.Establishment;
import com.smartgov.lez.core.agent.establishment.Round;
import com.smartgov.lez.core.agent.establishment.ST8;
import com.smartgov.lez.core.agent.establishment.VehicleCapacity;
import com.smartgov.lez.core.copert.inputParser.CopertProfile;
import com.smartgov.lez.core.copert.tableParser.CopertParser;

public class EstablishmentLoader {
	
	private Map<String, Establishment> loadedEstablishments;
	private Map<String, List<TemporaryRound>> temporaryRounds;
	
	public EstablishmentLoader() {
		this.loadedEstablishments = new HashMap<>();
		temporaryRounds = new HashMap<>();
	}

	public static Map<String, Establishment> loadEstablishments(
			File establishmentsFile, File fleetProfiles, File copertFile, Random random) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(EstablishmentLoader.class, new EstablishmentDeserializer(fleetProfiles, copertFile, random));
		mapper.registerModule(module);
		
		return mapper.readValue(establishmentsFile, EstablishmentLoader.class).loadedEstablishments();
	}
	
	public static Map<String, Establishment> loadEstablishments(
			File establishmentsFile, File fleetProfiles, File copertFile) throws JsonParseException, JsonMappingException, IOException {
		return loadEstablishments(establishmentsFile, fleetProfiles, copertFile, new Random());
	}
	
	private Map<String, Establishment> loadedEstablishments() {
		return loadedEstablishments;
	}
	
	public void load(Establishment establishment) {
		this.loadedEstablishments.put(establishment.getId(), establishment);
	}
	
	public void loadTemporaryRounds(String establishmentId, List<TemporaryRound> round) {
		temporaryRounds.put(establishmentId, round);
	}
	
	public void buildFleets(File fleetProfiles, File copertFile, Random random) throws JsonParseException, JsonMappingException, IOException {
		Map<ST8, CopertProfile> fleetProfilesMap = new ObjectMapper().readValue(fleetProfiles, new TypeReference<Map<ST8, CopertProfile>>(){});
		
		// All the vehicles will belong to the loaded copert table
		CopertParser parser = new CopertParser(copertFile, random);
		
		for(Establishment establishment : loadedEstablishments.values()) {
			/*
			 * Hypothesis : each establishment has at its disposal a vehicle by
			 * daily round it must perform.
			 */
			int fleetSize = temporaryRounds.get(establishment.getId()).size();
			DeliveryVehicleFactory vehicleFactory = new DeliveryVehicleFactory(
					fleetProfilesMap.get(establishment.getActivity()),
					parser
					);

			List<DeliveryVehicle> fleet = vehicleFactory.create(fleetSize);
			for(DeliveryVehicle vehicle : fleet) {
				establishment.addVehicleToFleet(vehicle);
			}
		}
		buildRounds();
	}
	
	private void buildRounds() {
		for(String establishmentId : temporaryRounds.keySet()) {
			Establishment loadedEstablishment = loadedEstablishments.get(establishmentId);
			List<Round> rounds = new ArrayList<>();
			for(TemporaryRound tempRound : temporaryRounds.get(establishmentId)) {
				List<Establishment> roundEstablishments = new ArrayList<>();
				for(String id : tempRound.getIds()) {
					Establishment roundEstablishment = loadedEstablishments.get(id);
					if(roundEstablishment == null) {
						throw new IllegalStateException(
								"Establishment id " + "\"" + id + "\" in round " + tempRound.getIds()
								+ "of establishment " + establishmentId + " does not correspond to any input establishment.");
					}
					roundEstablishments.add(roundEstablishment);
				}
				rounds.add(new Round(loadedEstablishment, roundEstablishments, tempRound.getWeight()));
			}
			assignVehiclesToRounds(rounds, loadedEstablishment);
		}
	}
	
	private void assignVehiclesToRounds(List<Round> rounds, Establishment establishment) {
		rounds.sort((round1, round2) -> {
			if(round1.getInitialWeight() < round2.getInitialWeight())
				return -1;
			if(round1.getInitialWeight() < round2.getInitialWeight())
				return 1;
			return 0;
		});
		
		TreeMap<VehicleCapacity, LinkedList<DeliveryVehicle>> availableVehicles = new TreeMap<>();
		for(VehicleCapacity capacity : establishment.getFleet().keySet()) {
			availableVehicles.put(capacity, new LinkedList<>(establishment.getFleet().get(capacity)));
		}
		
		for(Round round : rounds) {
			DeliveryVehicle selectedVehicle = availableVehicles.firstEntry().getValue().pollFirst();
			if(availableVehicles.firstEntry().getValue().isEmpty()) {
				availableVehicles.pollFirstEntry();
			}
			establishment.addRound(selectedVehicle, round);
		}
		
	}
	
	static class TemporaryRound {
		private List<String> ids;
		private double weight;

		public TemporaryRound(List<String> ids, double weight) {
			super();
			this.ids = ids;
			this.weight = weight;
		}

		public List<String> getIds() {
			return ids;
		}
		public double getWeight() {
			return weight;
		}
	}
}