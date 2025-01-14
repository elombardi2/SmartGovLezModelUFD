package org.liris.smartgov.lez.core.copert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Random;

import org.junit.Test;
import org.liris.smartgov.lez.core.copert.Copert;
import org.liris.smartgov.lez.core.copert.CopertParameters;
import org.liris.smartgov.lez.core.copert.fields.EuroNorm;
import org.liris.smartgov.lez.core.copert.fields.Fuel;
import org.liris.smartgov.lez.core.copert.fields.HeavyDutyTrucksSegment;
import org.liris.smartgov.lez.core.copert.fields.LightWeightVehicleSegment;
import org.liris.smartgov.lez.core.copert.fields.Load;
import org.liris.smartgov.lez.core.copert.fields.Mode;
import org.liris.smartgov.lez.core.copert.fields.Pollutant;
import org.liris.smartgov.lez.core.copert.fields.RoadSlope;
import org.liris.smartgov.lez.core.copert.fields.Technology;
import org.liris.smartgov.lez.core.copert.fields.VehicleCategory;
import org.liris.smartgov.lez.core.copert.tableParser.CopertParser;
import org.liris.smartgov.lez.core.copert.tableParser.CopertTree;

public class CopertTest {
	
	private static CopertParser loadParser() {
		URL url = CopertTest.class.getResource("copert_test.csv");
		return new CopertParser(new File(url.getFile()), new Random(1907190830l));
	}
	
	private static Copert loadLightVehicle() {
		CopertParser parser = loadParser();
		CopertTree vehicleClass = parser.getCopertTree()
				.select(VehicleCategory.LIGHT_WEIGHT.matcher())
				.select(Fuel.PETROL.matcher())
				.select(LightWeightVehicleSegment.N1_I.matcher())
				.select(EuroNorm.CONVENTIONAL.matcher())
				.select(Technology.NONE.matcher());
		return new Copert(vehicleClass);
	}
	
	private static Copert loadHeavyVehicle() {
		CopertParser parser = loadParser();
		CopertTree vehicleClass = parser.getCopertTree()
				.select(VehicleCategory.HEAVY_DUTY_TRUCK.matcher())
				.select(Fuel.DIESEL.matcher())
				.select(HeavyDutyTrucksSegment.RIGID_12_14_T.matcher())
				.select(EuroNorm.CONVENTIONAL.matcher())
				.select(Technology.NONE.matcher());
		return new Copert(vehicleClass);
	}
	
	@Test
	public void loadCopertTest() {
		loadLightVehicle();
		loadHeavyVehicle();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void tryToLoadCopertFromABadTree() {
		CopertParser parser = loadParser();
		CopertTree vehicleClass = parser.getCopertTree()
				.select(VehicleCategory.LIGHT_WEIGHT.matcher());
		
		// Trying to instantiate a Copert instance from a tree that
		// is not at the pollutant level must throw an exception.
		new Copert(vehicleClass);
	}
	
	// Light vehicle
	
	/*
	 * NOx can be loaded directly from pollutant, because there is no
	 * mode / load / road slope / ... dependence
	 */
	@Test
	public void loadNox() {
		Copert copert = loadLightVehicle();
		
		CopertParameters noxParameters = copert.getCopertParameters(Pollutant.NOx);
		
		assertThat(
				noxParameters,
				equalTo(
					new CopertParameters(
						0.015353282135382,
						4.60598463919402,
						230.29923194744,
						-0.00000000327712611716908,
						-0.0000000000003946197723497,
						0.000000000186443632739188,
						153.532821297973,
						0.,
						10,
						130
						)
					)
				);
	}
	
	/*
	 * NOx can be loaded directly from pollutant, because there is no
	 * mode / load / road slope / ... dependence
	 */
	@Test
	public void loadNoxWithAllParameters() {
		Copert copert = loadLightVehicle();
		
		CopertParameters noxParameters = copert.getCopertParameters(
				Pollutant.NOx,
				Mode.URBAN_PEAK,
				RoadSlope._0,
				Load._50);
		
		assertThat(
				noxParameters,
				equalTo(
					new CopertParameters(
						0.015353282135382,
						4.60598463919402,
						230.29923194744,
						-0.00000000327712611716908,
						-0.0000000000003946197723497,
						0.000000000186443632739188,
						153.532821297973,
						0.,
						10,
						130
						)
					)
				);
	}
	
	/*
	 * Fail, because CH4 as a Mode dependence
	 */
	@Test(expected = IllegalStateException.class)
	public void tryToLoadCh4WithoutMode() {
		Copert copert = loadLightVehicle();
		
		copert.getCopertParameters(Pollutant.CH4);
	}
	
	/*
	 * Ok for CH4 at Mode level
	 */
	@Test
	public void loadCh4() {
		Copert copert = loadLightVehicle();
		
		CopertParameters noxParameters = copert.getCopertParameters(Pollutant.CH4, Mode.URBAN_PEAK);
		
		assertThat(
				noxParameters,
				equalTo(new CopertParameters(0., 0., 131., 0., 0., 0., 1000., 0., 10, 130))
				);
	}
	
	// Heavy vehicle
	@Test(expected = IllegalStateException.class)
	public void loadNoxForHeavyTruckWithoutSpeeds() {
		Copert copert = loadHeavyVehicle();
		copert.getCopertParameters(Pollutant.NOx);
	}
	
	@Test
	public void loadNoxForHeavyTruck() {
		Copert copert = loadHeavyVehicle();
		
		CopertParameters noxParameters = copert.getCopertParameters(Pollutant.NOx, Mode.URBAN_PEAK, RoadSlope.MINUS_4, Load._50);
		
		assertThat(
				noxParameters,
				equalTo(
					new CopertParameters(
						-0.000540181435431,
						0.033989520933288,
						3.02584926554486,
						-2.40713836137629,
						0.000598849577695,
						0.01837331918092,
						0.090855417491015,
						0.,
						12,
						86
						)
					)
				);
	}
	
	@Test
	public void loadCoWithAllParameters() {
		Copert copert = loadHeavyVehicle();
		
		CopertParameters noxParameters = copert.getCopertParameters(Pollutant.CO, Mode.URBAN_PEAK, RoadSlope.MINUS_4, Load._50);
		
		assertThat(
				noxParameters,
				equalTo(
					new CopertParameters(
						0.0000152854841355476,
						-0.005035028733541,
						0.493533494481877,
						3.26069834804614,
						0.0000314367823010959,
						0.007026665411491,
						0.075881074248783,
						0.,
						12,
						86
						)
					)
				);
	}
	
	@Test
	public void loadReductionFactor() {
		Copert copert = loadLightVehicle();
		
		CopertParameters fcParameters = copert.getCopertParameters(Pollutant.FC);
		
		/*
		 * The 22.5 reduction factor is fictional and has been added manually.
		 */
		assertThat(
				fcParameters,
				equalTo(
					new CopertParameters(
						0.000230977790806,
						-0.022251145165247,
						0.702928128250708,
						16.6133248595622,
						4.56724968688701E-05,
						-0.004662246911848,
						0.255671022047122,
						22.5,
						10.,
						130.
						)
					)
				);
		
	}
}
