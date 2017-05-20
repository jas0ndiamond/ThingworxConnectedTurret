package com.thingworx;

import java.util.concurrent.TimeoutException;

import org.jason.turretcontrol.TurretControl;
import org.jason.turretcontrol.firecontrol.cycle.CycleResult;
import org.jason.turretcontrol.motors.MotorMotionResult;
import org.jason.turretcontrol.motors.wrapper.MotorControl;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.ConnectionException;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.EventDefinition;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.PropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.metadata.collections.FieldDefinitionCollection;
import com.thingworx.relationships.RelationshipTypes.ThingworxEntityTypes;
import com.thingworx.types.BaseTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.AspectCollection;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.constants.Aspects;
import com.thingworx.types.constants.CommonPropertyNames;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.DatetimePrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

public class TurretThing extends VirtualThing implements Runnable {

	private final static int SUBSCRIBED_PROPS_TIMEOUT = 5000;
	private final static int SUBSCRIBED_EVENTS_TIMEOUT = 5000;

	private final static String TIME_FIELD = "TurretLocalTime";
	private final static String SAFETY_ON_FIELD = "SafetyOn";
	private final static String AMMO_COUNT_FIELD = "AmmoCount";
	
	private final static String DEMO_RUNNING_FIELD = "demoRunning";
	
//	private final static String IS_JAMMED_FIELD = "IsJammed";
//	private final static String TOTAL_JAMS_OCCURRED = "TotalJamsOccurred";
	
	private final static String CURRENT_MAG_CAPACITY_FIELD = "MagCapacity";
	private final static String CURRENT_MAG_NAME_FIELD = "MagName";
	private final static String MOTOR_X_POS_FIELD = "MotorXPos";
	private final static String MOTOR_Y_POS_FIELD = "MotorYPos";
	private final static String MOTOR_X_TRAVEL_FIELD = "MotorXTravel";
	private final static String MOTOR_Y_TRAVEL_FIELD = "MotorYTravel";
	
	//private final static String LAST_ROUND_VELO_FIELD = "LastRoundVelo";
	
	private final static String LAST_RELOAD_TIME_FIELD = "LastReloadTime";
	private final static String LAST_SERVICE_TIME_FIELD = "LastServiceTime";
	
	//private final static String AVERAGE_ROUND_VELO_FIELD = "AverageRoundVelo";
	
	private final static String LAST_CYCLE_TIME_FIELD = "LastCycleTime";
	private final static String TOTAL_ROUNDS_FIRED_FIELD = "TotalRoundsFired";
	
	private final static String CYCLE_STATUS_FIELD = "CycleStatus";
	private final static String TRIGGER_DURATION_FIELD = "TriggerDuration";
	private final static String ACCEL_DURATION_FIELD = "AccelDuration";
	private final static String TOTAL_CYCLES_ATTEMPTED_FIELD = "TotalCyclesAttempted";

	/*
	private final static String CURRENT_TEMPERATURE = "CurrentTemperature";
	private final static String CURRENT_HUMIDITY = "CurrentHumidity";
	private final static String CURRENT_PRESSURE = "CurrentPressure";
	private final static String DEMO_ISRUNNING_FIELD = "DemoIsRunning";
	*/
	
	private final static String CPU_TEMP_FIELD = "CPUTemp";
	private final static String GPU_TEMP_FIELD = "GPUTemp";
	private final static String LOAD_AVG1_FIELD = "LoadAvg1";
	private final static String LOAD_AVG2_FIELD = "LoadAvg2";
	private final static String LOAD_AVG3_FIELD = "LoadAvg3";
	private final static String MEM_TOTAL_FIELD = "MemTotal";
	private final static String MEM_FREE_FIELD = "MemFree";
	private final static String MEM_USED_FIELD = "MemUsed";
	private final static String JVM_MEM_TOTAL_FIELD = "JVMMemTotal";
	private final static String JVM_MEM_FREE_FIELD = "JVMMemFree";
	private final static String JVM_MEM_USED_FIELD = "JVMMemUsed";
	
	private Thread _shutdownThread = null;
	
	private TurretControl turret;
	
	private final static String MOTOR_X_TRAVEL_THRESHOLD_FIELD = "MotorXTravelThreshold";
	private final static String MOTOR_Y_TRAVEL_THRESHOLD_FIELD = "MotorYTravelThreshold";
	private final static String TOTAL_ROUNDS_FIRED_THRESHOLD_FIELD = "TotalRoundsFiredThreshold";
	private final static String ACCEL_DURATION_THRESHOLD_FIELD = "AccelDurationThreshold";
	private final static String TRIGGER_DURATION_THRESHOLD_FIELD = "TriggerDurationThreshold";

	private final static String MOTOR_X_TRAVEL_THRESHOLD_EXCEEDED_FIELD = "MotorXTravelThresholdExceeded";
	private final static String MOTOR_Y_TRAVEL_THRESHOLD_EXCEEDED_FIELD = "MotorYTravelThresholdExceeded";
	private final static String TOTAL_ROUNDS_FIRED_THRESHOLD_EXCEEDED_FIELD = "TotalRoundsFiredThresholdExceeded";
	private final static String ACCEL_DURATION_THRESHOLD_EXCEEDED_FIELD = "AccelDurationThresholdExceeded";
	private final static String TRIGGER_DURATION_THRESHOLD_EXCEEDED_FIELD = "TriggerDurationThresholdExceeded";	
	
	private final static String MOTOR_X_SERVICE_REQUIRED_FIELD = "MotorXServiceRequired";
	private final static String MOTOR_Y_SERVICE_REQUIRED_FIELD = "MotorYServiceRequired";
	private final static String CYCLE_SERVICE_REQUIRED_FIELD = "CycleServiceRequired";
	private final static String ACCEL_SERVICE_REQUIRED_FIELD = "AccelServiceRequired";
	private final static String TRIGGER_SERVICE_REQUIRED_FIELD = "TriggerServiceRequired";
	
	private final static Logger logger = LoggerFactory.getLogger(TurretThing.class); 


	private static final long serialVersionUID = 1835049479058577519L;

	public TurretThing(String name, String description, String identifier, ConnectedThingClient client, String turretConfig) throws Exception {
		super(name, description, identifier, client);

		//Turret init here
		turret = new TurretControl(turretConfig);

		// Populate the thing shape with the properties, services, and events that are annotated in
		// this code
		super.initializeFromAnnotations();
		this.init();
		
		AspectCollection persist = new AspectCollection();
		persist.put(Aspects.ASPECT_ISPERSISTENT, new BooleanPrimitive(true));
		
		//shouldn't have to do this. annotations should handle it
		super.defineProperty(new PropertyDefinition(TIME_FIELD, TIME_FIELD, BaseTypes.DATETIME));
		super.getProperty(TIME_FIELD).getPropertyDefinition().setReadOnly(true);
		
		super.defineProperty(new PropertyDefinition(SAFETY_ON_FIELD, SAFETY_ON_FIELD, BaseTypes.BOOLEAN));
		super.getProperty(SAFETY_ON_FIELD).getPropertyDefinition().setReadOnly(true);
		
		super.defineProperty(new PropertyDefinition(AMMO_COUNT_FIELD, AMMO_COUNT_FIELD, BaseTypes.INTEGER));
		//super.defineProperty(new PropertyDefinition(IS_JAMMED_FIELD, IS_JAMMED_FIELD, BaseTypes.BOOLEAN));
		super.defineProperty(new PropertyDefinition(CURRENT_MAG_NAME_FIELD, CURRENT_MAG_NAME_FIELD, BaseTypes.STRING));
		super.getProperty(CURRENT_MAG_NAME_FIELD).getPropertyDefinition().setReadOnly(true);
		
		super.defineProperty(new PropertyDefinition(CURRENT_MAG_CAPACITY_FIELD, CURRENT_MAG_CAPACITY_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(MOTOR_X_POS_FIELD, MOTOR_X_POS_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(MOTOR_Y_POS_FIELD, MOTOR_Y_POS_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(MOTOR_X_TRAVEL_FIELD, MOTOR_X_TRAVEL_FIELD, BaseTypes.INTEGER));
		super.getProperty(MOTOR_X_TRAVEL_FIELD).getPropertyDefinition().setAspects(persist);
		
		super.defineProperty(new PropertyDefinition(MOTOR_Y_TRAVEL_FIELD, MOTOR_Y_TRAVEL_FIELD, BaseTypes.INTEGER));
		super.getProperty(MOTOR_Y_TRAVEL_FIELD).getPropertyDefinition().setAspects(persist);
		
		//super.defineProperty(new PropertyDefinition(LAST_ROUND_VELO_FIELD, LAST_ROUND_VELO_FIELD, BaseTypes.NUMBER));
		//super.defineProperty(new PropertyDefinition(LAST_RELOAD_TIME_FIELD, LAST_RELOAD_TIME_FIELD, BaseTypes.DATETIME));
		//super.defineProperty(new PropertyDefinition(LAST_SERVICE_TIME_FIELD, LAST_SERVICE_TIME_FIELD, BaseTypes.DATETIME));
		//super.defineProperty(new PropertyDefinition(AVERAGE_ROUND_VELO_FIELD, AVERAGE_ROUND_VELO_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(LAST_RELOAD_TIME_FIELD, LAST_RELOAD_TIME_FIELD, BaseTypes.DATETIME));
		super.getProperty(LAST_RELOAD_TIME_FIELD).getPropertyDefinition().setAspects(persist);
		
		super.defineProperty(new PropertyDefinition(LAST_CYCLE_TIME_FIELD, LAST_CYCLE_TIME_FIELD, BaseTypes.DATETIME));
		super.getProperty(LAST_CYCLE_TIME_FIELD).getPropertyDefinition().setAspects(persist);

		super.defineProperty(new PropertyDefinition(LAST_SERVICE_TIME_FIELD, LAST_SERVICE_TIME_FIELD, BaseTypes.DATETIME));
		super.getProperty(LAST_SERVICE_TIME_FIELD).getPropertyDefinition().setAspects(persist);

		super.defineProperty(new PropertyDefinition(TOTAL_ROUNDS_FIRED_FIELD, TOTAL_ROUNDS_FIRED_FIELD, BaseTypes.LONG));
		super.getProperty(TOTAL_ROUNDS_FIRED_FIELD).getPropertyDefinition().setAspects(persist);

		super.defineProperty(new PropertyDefinition(TOTAL_CYCLES_ATTEMPTED_FIELD, TOTAL_CYCLES_ATTEMPTED_FIELD, BaseTypes.LONG));
		super.getProperty(TOTAL_CYCLES_ATTEMPTED_FIELD).getPropertyDefinition().setAspects(persist);

		//super.defineProperty(new PropertyDefinition(TOTAL_JAMS_OCCURRED, TOTAL_JAMS_OCCURRED, BaseTypes.LONG));
		//super.defineProperty(new PropertyDefinition(CURRENT_TEMPERATURE, CURRENT_TEMPERATURE, BaseTypes.NUMBER));
		//super.defineProperty(new PropertyDefinition(CURRENT_HUMIDITY, CURRENT_HUMIDITY, BaseTypes.NUMBER));
		//super.defineProperty(new PropertyDefinition(CURRENT_PRESSURE, CURRENT_PRESSURE, BaseTypes.NUMBER));
		
		super.defineProperty(new PropertyDefinition(TRIGGER_DURATION_FIELD, TRIGGER_DURATION_FIELD, BaseTypes.LONG));
		super.getProperty(TRIGGER_DURATION_FIELD).getPropertyDefinition().setAspects(persist);
		
		super.defineProperty(new PropertyDefinition(ACCEL_DURATION_FIELD, ACCEL_DURATION_FIELD, BaseTypes.LONG));
		super.getProperty(ACCEL_DURATION_FIELD).getPropertyDefinition().setAspects(persist);
		
		super.defineProperty(new PropertyDefinition(CPU_TEMP_FIELD, CPU_TEMP_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(GPU_TEMP_FIELD, GPU_TEMP_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(LOAD_AVG1_FIELD, LOAD_AVG1_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(LOAD_AVG2_FIELD, LOAD_AVG2_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(LOAD_AVG3_FIELD, LOAD_AVG3_FIELD, BaseTypes.NUMBER));
		super.defineProperty(new PropertyDefinition(MEM_TOTAL_FIELD, MEM_TOTAL_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(MEM_FREE_FIELD, MEM_FREE_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(MEM_USED_FIELD, MEM_USED_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(JVM_MEM_TOTAL_FIELD, JVM_MEM_TOTAL_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(JVM_MEM_FREE_FIELD, JVM_MEM_FREE_FIELD, BaseTypes.INTEGER));
		super.defineProperty(new PropertyDefinition(JVM_MEM_USED_FIELD, JVM_MEM_USED_FIELD, BaseTypes.INTEGER));

		EventDefinition motorXThresholdEvent = new EventDefinition();
		motorXThresholdEvent.setName(MOTOR_X_TRAVEL_THRESHOLD_EXCEEDED_FIELD);
		motorXThresholdEvent.setDataShapeName(MOTOR_X_TRAVEL_THRESHOLD_EXCEEDED_FIELD);
		motorXThresholdEvent.setInvocable(true);
		motorXThresholdEvent.setPropertyEvent(false);
		super.defineEvent(motorXThresholdEvent);

		EventDefinition motorYThresholdEvent = new EventDefinition();
		motorYThresholdEvent.setName(MOTOR_Y_TRAVEL_THRESHOLD_EXCEEDED_FIELD);
		motorYThresholdEvent.setDataShapeName(MOTOR_Y_TRAVEL_THRESHOLD_EXCEEDED_FIELD);
		motorYThresholdEvent.setInvocable(true);
		motorYThresholdEvent.setPropertyEvent(false);
		super.defineEvent(motorYThresholdEvent);
		
		EventDefinition totalRoundsFiredThresholdEvent = new EventDefinition();
		totalRoundsFiredThresholdEvent.setName(TOTAL_ROUNDS_FIRED_THRESHOLD_EXCEEDED_FIELD);
		totalRoundsFiredThresholdEvent.setDataShapeName(TOTAL_ROUNDS_FIRED_THRESHOLD_EXCEEDED_FIELD);
		totalRoundsFiredThresholdEvent.setInvocable(true);
		totalRoundsFiredThresholdEvent.setPropertyEvent(false);		
		super.defineEvent(totalRoundsFiredThresholdEvent);
		
		EventDefinition accelDurationThresholdEvent = new EventDefinition();
		accelDurationThresholdEvent.setName(ACCEL_DURATION_THRESHOLD_EXCEEDED_FIELD);
		accelDurationThresholdEvent.setDataShapeName(ACCEL_DURATION_THRESHOLD_EXCEEDED_FIELD);
		accelDurationThresholdEvent.setInvocable(true);
		accelDurationThresholdEvent.setPropertyEvent(false);		
		super.defineEvent(accelDurationThresholdEvent);
		
		EventDefinition triggerDurationThresholdEvent = new EventDefinition();
		triggerDurationThresholdEvent.setName(TRIGGER_DURATION_THRESHOLD_EXCEEDED_FIELD);
		triggerDurationThresholdEvent.setDataShapeName(TRIGGER_DURATION_THRESHOLD_EXCEEDED_FIELD);
		triggerDurationThresholdEvent.setInvocable(true);
		triggerDurationThresholdEvent.setPropertyEvent(false);		
		super.defineEvent(triggerDurationThresholdEvent);
		
	}

	// From the VirtualThing class
	// This method will get called when a connect or reconnect happens
	// Need to send the values when this happens
	// This is more important for a solution that does not send its properties on a regular basis
	public void synchronizeState() {
		// Be sure to call the base class
		super.synchronizeState();
		// Send the property values to Thingworx when a synchronization is required
		super.syncProperties();
		
		//signal cancellation of any demo running when we connect/reconnect
        try 
        {
			getClient().writeProperty(ThingworxEntityTypes.Things, getName(), DEMO_RUNNING_FIELD, new BooleanPrimitive(false), 3000);
		} 
        catch (Exception e) 
        {
			logger.warn("Exception while writing initial demoRunning", e);
		}
	}
	
	private void init() throws Exception {

		super.initialize();
		
		FieldDefinitionCollection fields = new FieldDefinitionCollection();

		fields.addFieldDefinition(new FieldDefinition(TIME_FIELD, BaseTypes.DATETIME));
		fields.addFieldDefinition(new FieldDefinition(SAFETY_ON_FIELD, BaseTypes.BOOLEAN));
		fields.addFieldDefinition(new FieldDefinition(AMMO_COUNT_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(CPU_TEMP_FIELD, BaseTypes.NUMBER));
		fields.addFieldDefinition(new FieldDefinition(GPU_TEMP_FIELD, BaseTypes.NUMBER));
		fields.addFieldDefinition(new FieldDefinition(LOAD_AVG1_FIELD, BaseTypes.NUMBER));
		fields.addFieldDefinition(new FieldDefinition(LOAD_AVG2_FIELD, BaseTypes.NUMBER));
		fields.addFieldDefinition(new FieldDefinition(LOAD_AVG3_FIELD, BaseTypes.NUMBER));
		fields.addFieldDefinition(new FieldDefinition(MEM_TOTAL_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(MEM_FREE_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(MEM_USED_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(JVM_MEM_TOTAL_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(JVM_MEM_FREE_FIELD, BaseTypes.INTEGER));
		fields.addFieldDefinition(new FieldDefinition(JVM_MEM_USED_FIELD, BaseTypes.INTEGER));
		defineDataShapeDefinition("TurretTelemetry", fields);
		
		FieldDefinitionCollection supportedMagsFields = new FieldDefinitionCollection();
		supportedMagsFields.addFieldDefinition(new FieldDefinition(CURRENT_MAG_NAME_FIELD, BaseTypes.STRING));
		defineDataShapeDefinition("SupportedMagazines", supportedMagsFields);
		
		FieldDefinitionCollection cycleFields = new FieldDefinitionCollection();
		cycleFields.addFieldDefinition(new FieldDefinition(LAST_CYCLE_TIME_FIELD, BaseTypes.DATETIME));
		cycleFields.addFieldDefinition(new FieldDefinition(CYCLE_STATUS_FIELD, BaseTypes.STRING));
		cycleFields.addFieldDefinition(new FieldDefinition(SAFETY_ON_FIELD, BaseTypes.BOOLEAN));
		cycleFields.addFieldDefinition(new FieldDefinition(TRIGGER_DURATION_FIELD, BaseTypes.LONG));
		cycleFields.addFieldDefinition(new FieldDefinition(ACCEL_DURATION_FIELD, BaseTypes.LONG));
		defineDataShapeDefinition("TurretCycleResult", cycleFields);
		
		FieldDefinitionCollection reloadFields = new FieldDefinitionCollection();
		reloadFields.addFieldDefinition(new FieldDefinition(LAST_RELOAD_TIME_FIELD, BaseTypes.DATETIME));
		reloadFields.addFieldDefinition(new FieldDefinition(AMMO_COUNT_FIELD, BaseTypes.INTEGER));
		reloadFields.addFieldDefinition(new FieldDefinition(CURRENT_MAG_CAPACITY_FIELD, BaseTypes.INTEGER));
		reloadFields.addFieldDefinition(new FieldDefinition(CURRENT_MAG_NAME_FIELD, BaseTypes.STRING));
		defineDataShapeDefinition("TurretReloadResult", reloadFields);
		
		FieldDefinitionCollection motorXEventFields = new FieldDefinitionCollection();
		motorXEventFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition(MOTOR_X_TRAVEL_THRESHOLD_EXCEEDED_FIELD, motorXEventFields);
		
		FieldDefinitionCollection motorYEventFields = new FieldDefinitionCollection();
		motorYEventFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition(MOTOR_Y_TRAVEL_THRESHOLD_EXCEEDED_FIELD, motorYEventFields);
		
		FieldDefinitionCollection triggerEventFields = new FieldDefinitionCollection();
		triggerEventFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition(TOTAL_ROUNDS_FIRED_THRESHOLD_EXCEEDED_FIELD, triggerEventFields);
		
		FieldDefinitionCollection accelEventFields = new FieldDefinitionCollection();
		accelEventFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition(ACCEL_DURATION_THRESHOLD_EXCEEDED_FIELD, accelEventFields);
		
		FieldDefinitionCollection faultFields = new FieldDefinitionCollection();
		faultFields.addFieldDefinition(new FieldDefinition(CommonPropertyNames.PROP_MESSAGE,BaseTypes.STRING));
		defineDataShapeDefinition(TRIGGER_DURATION_THRESHOLD_EXCEEDED_FIELD, faultFields);
	}


	// The processScanRequest is called by the SteamSensorClient every scan cycle
	@Override
	public void processScanRequest() throws Exception {
		// Be sure to call the base classes scan request
		super.processScanRequest();
		// Execute the code for this simulation every scan
		this.scanDevice();
	}

	public void scanDevice() throws Exception {
		
		//get info from turret
		super.setProperty(TIME_FIELD, new DatetimePrimitive(DateTime.now()));
		super.setProperty(SAFETY_ON_FIELD, new BooleanPrimitive(turret.getSafety()));
		super.setProperty(AMMO_COUNT_FIELD, new IntegerPrimitive(turret.getAmmoCount()));	
		super.setProperty(CPU_TEMP_FIELD, new NumberPrimitive(turret.getCPUTemp()));
		super.setProperty(GPU_TEMP_FIELD, new NumberPrimitive(turret.getGPUTemp()));
		
		double[] loadAvg = turret.getSystemLoadAvg();
		
		super.setProperty(LOAD_AVG1_FIELD, new NumberPrimitive(loadAvg[0]));
		super.setProperty(LOAD_AVG2_FIELD, new NumberPrimitive(loadAvg[1]));
		super.setProperty(LOAD_AVG3_FIELD, new NumberPrimitive(loadAvg[2]));
		
		int[] memUtil = turret.getMemoryInfo();
		super.setProperty(MEM_TOTAL_FIELD, new NumberPrimitive(memUtil[0]));
		super.setProperty(MEM_USED_FIELD, new NumberPrimitive(memUtil[1]));
		super.setProperty(MEM_FREE_FIELD, new NumberPrimitive(memUtil[2]));
		
		long[] jvmMemUtil = turret.getJVMMemoryUtilization();
		
		super.setProperty(JVM_MEM_TOTAL_FIELD, new NumberPrimitive(jvmMemUtil[0]));
		super.setProperty(JVM_MEM_USED_FIELD, new NumberPrimitive(jvmMemUtil[1]));
		super.setProperty(JVM_MEM_FREE_FIELD, new NumberPrimitive(jvmMemUtil[2]));
		
		super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		
		
		//check maintenance cycle 
		ValueCollection platformProperties = getClient().readProperties(ThingworxEntityTypes.Things, getName(), 3000).getFirstRow();
		int motorXTravelThreshold = (int)platformProperties.getValue(MOTOR_X_TRAVEL_THRESHOLD_FIELD);
		int motorYTravelThreshold = (int)platformProperties.getValue(MOTOR_Y_TRAVEL_THRESHOLD_FIELD);
		int totalRoundsFiredThreshold = (int)platformProperties.getValue(TOTAL_ROUNDS_FIRED_THRESHOLD_FIELD);
		int accelDurationThreshold = (int)platformProperties.getValue(ACCEL_DURATION_THRESHOLD_FIELD);
		int triggerDurationThreshold = (int)platformProperties.getValue(TRIGGER_DURATION_THRESHOLD_FIELD);
		
		boolean serviceRequired = false;
		if( (int)getProperty(MOTOR_X_TRAVEL_FIELD).getValue().getValue() > motorXTravelThreshold)
		{
			ValueCollection eventInfo = new ValueCollection();
			eventInfo.put(CommonPropertyNames.PROP_MESSAGE, 
					new StringPrimitive("Motor X has exceeded its step threshold"));
			// Queue the event
			super.queueEvent(MOTOR_X_TRAVEL_THRESHOLD_EXCEEDED_FIELD, DateTime.now(), eventInfo);
			
			serviceRequired = true;
		}
		
		if( (int)getProperty(MOTOR_Y_TRAVEL_FIELD).getValue().getValue() > motorYTravelThreshold)
		{
			ValueCollection eventInfo = new ValueCollection();
			eventInfo.put(CommonPropertyNames.PROP_MESSAGE, 
					new StringPrimitive("Motor Y has exceeded its step threshold"));			
			// Queue the event
			super.queueEvent(MOTOR_Y_TRAVEL_THRESHOLD_EXCEEDED_FIELD, DateTime.now(), eventInfo);
			
			serviceRequired = true;			
		}
		
		if( (long)getProperty(TOTAL_ROUNDS_FIRED_FIELD).getValue().getValue() > totalRoundsFiredThreshold)
		{
			ValueCollection eventInfo = new ValueCollection();
			eventInfo.put(CommonPropertyNames.PROP_MESSAGE, 
					new StringPrimitive("Firecontrol has exceeded its cycle count threshold"));
			// Queue the event
			super.queueEvent(TOTAL_ROUNDS_FIRED_THRESHOLD_EXCEEDED_FIELD, DateTime.now(), eventInfo);
			
			serviceRequired = true;
		}
		
		if( (long)getProperty(ACCEL_DURATION_FIELD).getValue().getValue() > accelDurationThreshold)
		{
			ValueCollection eventInfo = new ValueCollection();
			eventInfo.put(CommonPropertyNames.PROP_MESSAGE, 
					new StringPrimitive("The accelerator motor has exceeded its runtime threshold"));
			// Queue the event
			super.queueEvent(ACCEL_DURATION_THRESHOLD_EXCEEDED_FIELD, DateTime.now(), eventInfo);
			
			serviceRequired = true;
		}
		
		if( (long)getProperty(TRIGGER_DURATION_FIELD).getValue().getValue() > triggerDurationThreshold)
		{
			ValueCollection eventInfo = new ValueCollection();
			eventInfo.put(CommonPropertyNames.PROP_MESSAGE, 
					new StringPrimitive("The trigger motor has exceeded its runtime threshold"));
			// Queue the event
			super.queueEvent(TRIGGER_DURATION_THRESHOLD_EXCEEDED_FIELD, DateTime.now(), eventInfo);
			
			serviceRequired = true;
		}
		
		if(serviceRequired)
		{
			super.updateSubscribedEvents(SUBSCRIBED_EVENTS_TIMEOUT);
		}
	}

	//hold turret service
	
	
	@ThingworxServiceDefinition
	(
			name = "Cycle", 
			description = "Cycle the turret"
	)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "InfoTable of cycle result", 
			baseType = "INFOTABLE",
			aspects = { "dataShape:TurretCycleResult" }
	)
	public InfoTable Cycle() {
		
		logger.debug("Cycle invoked");


		InfoTable table = new InfoTable(getDataShapeDefinition("TurretCycleResult"));

		try
		{
			long totalCyclesAttempted = (long)(super.getProperty(TOTAL_CYCLES_ATTEMPTED_FIELD).getValue().getValue());
			super.setProperty(TOTAL_CYCLES_ATTEMPTED_FIELD, totalCyclesAttempted + 1);
			
			CycleResult result = turret.fire();
			
			DateTime cycleTime = new DateTime(result.getTime());
			
			logger.debug("Turret cycled: " + result.toString());
						
			long totalRoundsFired = (long)(super.getProperty(TOTAL_ROUNDS_FIRED_FIELD).getValue().getValue());
			long triggerDuration  = (long)(super.getProperty(TRIGGER_DURATION_FIELD).getValue().getValue());
			long accelDuration  = (long)(super.getProperty(ACCEL_DURATION_FIELD).getValue().getValue());
			
			super.setProperty(TOTAL_ROUNDS_FIRED_FIELD, totalRoundsFired + 1);
			super.setProperty(LAST_CYCLE_TIME_FIELD, new DatetimePrimitive(cycleTime));
			
			super.setProperty(TRIGGER_DURATION_FIELD, result.getTriggerDuration() + triggerDuration);
			super.setProperty(ACCEL_DURATION_FIELD, result.getAccelDuration() + accelDuration);	
			
			//get result from fire and trigger events accordingly
			//jamoccured, roundveltoolow, ammodry, etc
			
			ValueCollection entry = new ValueCollection();
			
			entry.clear();
			entry.SetDateTimeValue(LAST_CYCLE_TIME_FIELD, cycleTime);
			entry.SetStringValue(CYCLE_STATUS_FIELD, result.getStatus());
			entry.SetBooleanValue(SAFETY_ON_FIELD, turret.getSafety());
			entry.SetLongValue(TRIGGER_DURATION_FIELD, result.getTriggerDuration());
			entry.SetLongValue(ACCEL_DURATION_FIELD, result.getAccelDuration());
			table.addRow(entry);
			
			//ammodry event if no ammo left
		}
		catch (Exception e)
		{
			logger.warn("Exception occured during cycle: ", e);
		}
		finally
		{
			
			try 
			{
				super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
			} 
			catch (Exception e) 
			{
				logger.warn("Exception occurred updating properties after cycle", e);
			}
		}
		
		logger.debug("Cycle completed");
		
		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "PerformService", 
			description = "Acknowledge maintenance performed on the turret"
	)
	public void PerformService
	(
		@ThingworxServiceParameter
		( 
			name="servicedXMotor", 
			description="Flag noting the x motor was serviced", 
			baseType="BOOLEAN" 
		) Boolean servicedXMotor,
		@ThingworxServiceParameter
		( 
			name="servicedYMotor", 
			description="Flag noting the y motor was serviced", 
			baseType="BOOLEAN" 
		) Boolean servicedYMotor,
		@ThingworxServiceParameter
		( 
			name="servicedTrigger", 
			description="Flag noting the trigger was serviced", 
			baseType="BOOLEAN" 
		) Boolean servicedTrigger,
		@ThingworxServiceParameter
		( 
			name="servicedAccel", 
			description="Flag noting the accel was serviced", 
			baseType="BOOLEAN" 
		) Boolean servicedAccel,
		@ThingworxServiceParameter
		( 
			name="servicedCycle", 
			description="Flag noting the cycle was serviced", 
			baseType="BOOLEAN" 
		) Boolean servicedCycle
	) throws Exception
	{
		logger.debug("PerformService invoked");
		

		boolean servicePerformed = false;

		if(servicedXMotor)
		{
			super.setProperty(MOTOR_X_TRAVEL_FIELD, 0);

			getClient().writeProperty(
					ThingworxEntityTypes.Things, getName(), 
					MOTOR_X_SERVICE_REQUIRED_FIELD, 
					new BooleanPrimitive(false), 
					3000
					);

			servicePerformed = true;
		}

		if(servicedYMotor)
		{
			super.setProperty(MOTOR_Y_TRAVEL_FIELD, 0);

			getClient().writeProperty(
					ThingworxEntityTypes.Things, getName(), 
					MOTOR_Y_SERVICE_REQUIRED_FIELD, 
					new BooleanPrimitive(false), 
					3000
					);

			servicePerformed = true;
		}

		if(servicedTrigger)
		{
			super.setProperty(TOTAL_ROUNDS_FIRED_FIELD, 0);

			getClient().writeProperty(
					ThingworxEntityTypes.Things, getName(), 
					CYCLE_SERVICE_REQUIRED_FIELD, 
					new BooleanPrimitive(false), 
					3000
					);

			servicePerformed = true;
		}

		if(servicedAccel)
		{
			super.setProperty(ACCEL_DURATION_FIELD, 0);

			getClient().writeProperty(
					ThingworxEntityTypes.Things, getName(), 
					ACCEL_SERVICE_REQUIRED_FIELD, 
					new BooleanPrimitive(false), 
					3000
					);

			servicePerformed = true;
		}

		if(servicedCycle)
		{
			super.setProperty(TRIGGER_DURATION_FIELD, 0);

			getClient().writeProperty(
					ThingworxEntityTypes.Things, getName(), 
					TRIGGER_SERVICE_REQUIRED_FIELD, 
					new BooleanPrimitive(false), 
					3000
					);

			servicePerformed = true;
		}

		if(servicePerformed)
		{
			super.setProperty(LAST_SERVICE_TIME_FIELD, new DatetimePrimitive(DateTime.now()));
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}
		
		logger.debug("PerformService completed");
		
	}
	
	@ThingworxServiceDefinition
	(
			name = "Reload", 
			description = "Reload the turret"
			)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "InfoTable of reload result", 
			baseType = "INFOTABLE",
			aspects = { "dataShape:TurretReloadResult" }
	)
	public InfoTable Reload
	(
			@ThingworxServiceParameter
			( 
					name="Magazine", 
					description="Type of magazine", 
					baseType="STRING" 
			) String magazine
	) throws Exception 
	{
		logger.debug("Reload invoked");

		turret.reload(magazine);

		super.setProperty(LAST_RELOAD_TIME_FIELD, new DatetimePrimitive(DateTime.now()));
		super.setProperty(CURRENT_MAG_NAME_FIELD, turret.getCurrentMagazine());
		super.setProperty(AMMO_COUNT_FIELD, turret.getAmmoCount());
		super.setProperty(CURRENT_MAG_CAPACITY_FIELD, turret.getMagSize());
		super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		
		logger.debug("Reloaded with magazine " + turret.getCurrentMagazine());
		
		InfoTable table = new InfoTable(getDataShapeDefinition("TurretReloadResult"));

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			entry.SetDateTimeValue(LAST_RELOAD_TIME_FIELD, new DatetimePrimitive(DateTime.now()));
			entry.SetIntegerValue(AMMO_COUNT_FIELD, turret.getAmmoCount());
			entry.SetIntegerValue(CURRENT_MAG_CAPACITY_FIELD, turret.getMagSize());
			entry.SetStringValue(CURRENT_MAG_NAME_FIELD, turret.getCurrentMagazine());
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		logger.debug("Reload completed");
		
		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "GetSupportedMagazines", 
			description = "Get magazine names that the turret supports"
			)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "InfoTable of supported magazines", 
			baseType = "INFOTABLE",
			aspects = { "dataShape:SupportedMagazines" }
	)
	public InfoTable GetSupportedMagazines() throws Exception 
	{
		InfoTable table = new InfoTable(getDataShapeDefinition("SupportedMagazines"));

		ValueCollection entry = new ValueCollection();

		try
		{
			for(String magName : turret.getSupportedMagazines())
			{
				logger.debug("Adding supported magazine: " + magName);
				
				entry.clear();
				entry.SetStringValue(CURRENT_MAG_NAME_FIELD, magName);
				table.addRow(entry.clone());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return table;
	}
	
	@ThingworxServiceDefinition
	(
		name = "SetSafety", 
		description = "Set Safety"
	)
	public void SetSafety
	(
			@ThingworxServiceParameter
			( 
					name="SafetyOn", 
					description="Safety", 
					baseType="BOOLEAN" 
			) Boolean safetySet
	) 
	{
		logger.debug("Setting safety to: " + safetySet);
		
		turret.setSafety(safetySet);
	}
	
	@ThingworxServiceDefinition
	(
		name = "PanTo", 
		description = "Pan the turret to designated coordinates"
	)
	@ThingworxServiceResult
	(
		name = CommonPropertyNames.PROP_RESULT, 
		description = "JSON of pan result", 
		baseType = "INFOTABLE",
		aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanTo
	(
		@ThingworxServiceParameter
		( 
			name="xPosition", 
			description="New position of the x motor", 
			baseType="INTEGER" 
		) Integer xPosition,
		@ThingworxServiceParameter
		( 
			name="yPosition", 
			description="New position of the y motor", 
			baseType="INTEGER" 
		) Integer yPosition
	) throws TimeoutException, ConnectionException, Exception 
	{		
		logger.debug("PanTo invoked with target: " + xPosition + ", " + yPosition);
		MotorMotionResult result = turret.panTo(xPosition, yPosition);
		
		logger.debug("PanTo result: " + result.toString());
		
		boolean moved = false;
		
		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();
		
		if(result.hasXMoved())
		{
			long xNewPos = result.getX().get(MotorMotionResult.NEW_POS);
			long xStepsTraveled = result.getX().get(MotorMotionResult.STEPS_MOVED);
			int totalXStepsTravelled = (int)(super.getProperty(MOTOR_X_TRAVEL_FIELD).getValue().getValue()) + (int)xStepsTraveled;
				
			super.setProperty(MOTOR_X_POS_FIELD, new IntegerPrimitive((int)xNewPos));
			super.setProperty(MOTOR_X_TRAVEL_FIELD, new IntegerPrimitive( totalXStepsTravelled ));
			
			entry.clear();
			entry.SetDateTimeValue(TIME_FIELD, new DatetimePrimitive(result.getX().get(MotorMotionResult.TIME)));
			entry.SetIntegerValue(MOTOR_X_POS_FIELD, xNewPos);
			entry.SetIntegerValue(MOTOR_X_TRAVEL_FIELD, totalXStepsTravelled);
			table.addRow(entry);
			
			moved = true;
		}
		
		if(result.hasYMoved())
		{
			long yNewPos = result.getY().get(MotorMotionResult.NEW_POS);
			long yStepsTraveled = result.getY().get(MotorMotionResult.STEPS_MOVED);
			int totalYStepsTravelled = (int)(super.getProperty(MOTOR_Y_TRAVEL_FIELD).getValue().getValue()) + (int)yStepsTraveled;

			super.setProperty(MOTOR_Y_POS_FIELD, new IntegerPrimitive((int)yNewPos));
			super.setProperty(MOTOR_Y_TRAVEL_FIELD, new IntegerPrimitive(totalYStepsTravelled ));
			
			entry.clear();
			entry.SetDateTimeValue(TIME_FIELD, new DatetimePrimitive(result.getY().get(MotorMotionResult.TIME)));
			entry.SetIntegerValue(MOTOR_X_POS_FIELD, yNewPos);
			entry.SetIntegerValue(MOTOR_X_TRAVEL_FIELD, totalYStepsTravelled);
			table.addRow(entry);
			
			moved = true;
		}
		
		if(moved)
		{
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}
		


		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		logger.debug("PanTo invoked");

		return table;
	}
	
	@ThingworxServiceDefinition
	(
		name = "PanXTo", 
		description = "Pan the turret along the x axis to a position"
	)
	@ThingworxServiceResult
	(
		name = CommonPropertyNames.PROP_RESULT, 
		description = "JSON of pan result", 
		baseType = "INFOTABLE",
		aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanXTo
	(
		@ThingworxServiceParameter
		( 
			name="position", 
			description="New position of the x motor", 
			baseType="INTEGER" 
		) Integer position
	) throws TimeoutException, ConnectionException, Exception 
	{		
		MotorMotionResult result = turret.panXTo(position);
		logger.debug("PanXTo result: " + result.toString());
		
		if(result.hasXMoved())
		{
			long xNewPos = result.getX().get(MotorMotionResult.NEW_POS);
			long xStepsTraveled = result.getX().get(MotorMotionResult.STEPS_MOVED);
			int totalXStepsTravelled = (int)(super.getProperty(MOTOR_X_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_X_POS_FIELD, new IntegerPrimitive((int)xNewPos));
			super.setProperty(MOTOR_X_TRAVEL_FIELD, new IntegerPrimitive( totalXStepsTravelled + (int)xStepsTraveled));
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}
		
		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return table;
	}
	
	@ThingworxServiceDefinition
	(
		name = "PanYTo", 
		description = "Pan the turret along the y axis to a position"
	)
	@ThingworxServiceResult
	(
		name = CommonPropertyNames.PROP_RESULT, 
		description = "JSON of pan result", 
		baseType = "INFOTABLE",
		aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanYTo
	(
		@ThingworxServiceParameter
		( 
			name="position", 
			description="New position of the y motor",			
			baseType="INTEGER" 
		) Integer position
	) throws TimeoutException, ConnectionException, Exception 
	{
		MotorMotionResult result = turret.panYTo(position);
		logger.debug("PanYTo result: " + result.toString());
		
		if(result.hasYMoved())
		{
			long yNewPos = result.getY().get(MotorMotionResult.NEW_POS);
			long yStepsTraveled = result.getY().get(MotorMotionResult.STEPS_MOVED);
			int totalYStepsTravelled = (int)(super.getProperty(MOTOR_Y_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_Y_POS_FIELD, new IntegerPrimitive((int)yNewPos));
			super.setProperty(MOTOR_Y_TRAVEL_FIELD, new IntegerPrimitive(totalYStepsTravelled + (int)yStepsTraveled));
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}
		
		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "PanX", 
			description = "Pan the turret along the x axis"
			)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "JSON of pan result", 
			baseType = "INFOTABLE",
			aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanX
	(
			@ThingworxServiceParameter
			( 
					name="steps", 
					description="Number of steps to pan the turret", 
					baseType="INTEGER" 
			) Integer steps,
			@ThingworxServiceParameter
			( 
					name="direction", 
					description="Direction to pan the turret", 
					baseType="INTEGER" 
			) Integer direction
	) throws TimeoutException, ConnectionException, Exception 
	{
		MotorMotionResult result = turret.panX(steps, direction);
		logger.debug("PanX result: " + result.toString());
		
		if(result.hasXMoved())
		{
			long xNewPos = result.getX().get(MotorMotionResult.NEW_POS);
			long xStepsTraveled = result.getX().get(MotorMotionResult.STEPS_MOVED);
			int totalXStepsTravelled = (int)(super.getProperty(MOTOR_X_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_X_POS_FIELD, new IntegerPrimitive((int)xNewPos));
			super.setProperty(MOTOR_X_TRAVEL_FIELD, new IntegerPrimitive( totalXStepsTravelled + (int)xStepsTraveled));
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}

		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "PanY", 
			description = "Pan the turret along the y axis"
			)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "JSON of pan result", 
			baseType = "INFOTABLE",
			aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanY
	(
			@ThingworxServiceParameter
			( 
					name="steps", 
					description="Number of steps to pan the turret", 
					baseType="INTEGER" 
			) Integer steps,
			@ThingworxServiceParameter
			( 
					name="direction", 
					description="Direction to pan the turret", 
					baseType="INTEGER" 
			) Integer direction
	) throws Exception 
	{
		MotorMotionResult result = turret.panY(steps, direction);
		logger.debug("PanY result: " + result.toString());
		
		if(result.hasYMoved())
		{
			long yNewPos = result.getY().get(MotorMotionResult.NEW_POS);
			long yStepsTraveled = result.getY().get(MotorMotionResult.STEPS_MOVED);
			int totalYStepsTravelled = (int)(super.getProperty(MOTOR_Y_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_Y_POS_FIELD, new IntegerPrimitive((int)yNewPos));
			super.setProperty(MOTOR_Y_TRAVEL_FIELD, new IntegerPrimitive(totalYStepsTravelled + (int)yStepsTraveled));
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}

		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "PanHome", 
			description = "Pan the turret to the home position"
			)
	@ThingworxServiceResult
	(
			name = CommonPropertyNames.PROP_RESULT, 
			description = "JSON of pan result", 
			baseType = "INFOTABLE",
			aspects = { "datashape:TurretPanResult" }
	)
	public InfoTable PanHome() throws TimeoutException, ConnectionException, Exception 
	{
		logger.debug("PanHome invoked");
		MotorMotionResult result = turret.panHome();
		logger.debug("PanHome result: " + result.toString());
		
		boolean moved = false;
		if(result.hasXMoved())
		{
			long xNewPos = result.getX().get(MotorMotionResult.NEW_POS);
			long xStepsTraveled = result.getX().get(MotorMotionResult.STEPS_MOVED);
			int totalXStepsTravelled = (int)(super.getProperty(MOTOR_X_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_X_POS_FIELD, new IntegerPrimitive((int)xNewPos));
			super.setProperty(MOTOR_X_TRAVEL_FIELD, new IntegerPrimitive( totalXStepsTravelled + (int)xStepsTraveled));
			moved = true;
		}
		
		if(result.hasYMoved())
		{
			long yNewPos = result.getY().get(MotorMotionResult.NEW_POS);
			long yStepsTraveled = result.getY().get(MotorMotionResult.STEPS_MOVED);
			int totalYStepsTravelled = (int)(super.getProperty(MOTOR_Y_TRAVEL_FIELD).getValue().getValue());
				
			super.setProperty(MOTOR_Y_POS_FIELD, new IntegerPrimitive((int)yNewPos));
			super.setProperty(MOTOR_Y_TRAVEL_FIELD, new IntegerPrimitive(totalYStepsTravelled + (int)yStepsTraveled));
			moved = true;
		}
		
		if(moved)
		{
			logger.debug("Updating position after panHome");
			super.updateSubscribedProperties(SUBSCRIBED_PROPS_TIMEOUT);
		}
		
		InfoTable table = new InfoTable();

		ValueCollection entry = new ValueCollection();

		try
		{
			entry.clear();
			table.addRow(entry);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		logger.debug("PanHome completed");

		return table;
	}
	
	@ThingworxServiceDefinition
	(
			name = "ReleaseMotors", 
			description = "Kill motor power"
	)
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="", baseType="NOTHING")
	public void ReleaseMotors() 
	{
		turret.killMotors();
	}
	
	@ThingworxServiceDefinition
	(
			name = "HoldMotors", 
			description = "Hold motors at current position"
	)
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="", baseType="NOTHING")
	public void HoldMotors() 
	{
		int holdStepCount = 20;

		turret.panX(holdStepCount, MotorControl.DIRECTION_BACKWARD);
		//try { Thread.sleep(2000); } catch (InterruptedException e)	{ logger.warn("Sleep interrupted", e); }
		
		turret.panY(holdStepCount, MotorControl.DIRECTION_BACKWARD);
		//try { Thread.sleep(2000); } catch (InterruptedException e)	{ logger.warn("Sleep interrupted", e); }
		
		turret.panX(holdStepCount, MotorControl.DIRECTION_FORWARD);
		//try { Thread.sleep(2000); } catch (InterruptedException e)	{ logger.warn("Sleep interrupted", e); }
		
		turret.panY(holdStepCount, MotorControl.DIRECTION_FORWARD);
		//try { Thread.sleep(2000); } catch (InterruptedException e)	{ logger.warn("Sleep interrupted", e); }
	}
	
	@ThingworxServiceDefinition( name="Shutdown", description="Shutdown the client")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="", baseType="NOTHING")
	public synchronized void Shutdown() throws Exception {
		// Should not have to do this, but guard against this method being called more than once.
		if(this._shutdownThread == null) {
			// Create a thread for shutting down and start the thread
			this._shutdownThread = new Thread(this);
			this._shutdownThread.start();
		}
	}
	
	@Override
	public void run() {
		try 
		{
			// Delay for a period to verify that the Shutdown service will return
			Thread.sleep(1000);
			// Shutdown the client
			this.getClient().shutdown();
			
			
			turret.shutdown();
		} 
		catch (Exception x) 
		{
			logger.warn("Exception occurred during shutdown", x);
		}
	}
}
