package com.thingworx;

import org.codehaus.jettison.json.JSONObject;
import org.jason.turretcontrol.config.ConfigLoader;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;

public class ThingworxConnectedTurret extends ConnectedThingClient {

	private final static String TURRET_CONFIG = "turret";
	private final static String TWX_CONFIG = "thingworx";
	private final static String TWX_URI = "uri";
	private final static String TWX_APP_KEY = "appkey";
	private final static String TWX_THING_NAME = "thingname";
	private final static String TWX_THING_DESC = "thingdesc";
	
	public ThingworxConnectedTurret(ClientConfigurator config) throws Exception {
		super(config);
	}
	
	public static void main(String[] args) throws Exception
	{
		String configFile = "./conf/config.json";
		
		if(args.length > 1)
		{
			configFile = args[0];
		}
		
		//take config file		
		JSONObject tcConfig = ConfigLoader.getConfigJSONObject(configFile);
		
		String turretConfig = tcConfig.getJSONObject(TURRET_CONFIG).toString();
		
		//apply config
		String appKey = tcConfig.getJSONObject(TWX_CONFIG).getString(TWX_APP_KEY);
		String uri = tcConfig.getJSONObject(TWX_CONFIG).getString(TWX_URI);
		String name = tcConfig.getJSONObject(TWX_CONFIG).getString(TWX_THING_NAME);
		String desc = tcConfig.getJSONObject(TWX_CONFIG).getString(TWX_THING_DESC);
		
		ClientConfigurator clientConfigurator = new ClientConfigurator();
		clientConfigurator.setUri(uri);
		clientConfigurator.setAppKey(appKey);
		clientConfigurator.getSecurityClaims().addClaim("appKey", appKey);
		clientConfigurator.ignoreSSLErrors(true);
		
				
		ThingworxConnectedTurret turretClient = new ThingworxConnectedTurret(clientConfigurator);
		
		turretClient.bindThing(new TurretThing(name, desc, "", turretClient, turretConfig));
		
        try {
            // Start the client
        	turretClient.start();
        	
        	turretClient.waitForConnection(5000);
        	
        } catch (Exception eStart) {
            System.out.println("Initial Start Failed : " + eStart.getMessage());
        }
		
        while (!turretClient.isShutdown()) {
            // Only process the Virtual Things if the client is connected
            if (turretClient.isConnected()) {
                // Loop over all the Virtual Things and process them
                for (VirtualThing thing : turretClient.getThings().values()) {
                    try {
                        thing.processScanRequest();
                    } catch (Exception eProcessing) {
                        System.out.println("Error Processing Scan Request for [" + thing.getName() + "] : " + eProcessing.getMessage());
                        eProcessing.printStackTrace();
                    }
                }
            }
            // Suspend processing at the scan rate interval
            Thread.sleep(1000);
        }

	}
	
}
